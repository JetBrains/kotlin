/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.serialization.fqNameSafe
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::SingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes"
)

class SingleAbstractMethodLowering(val context: CommonBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        // TODO use `IrType`s; unfortunately, they are currently not comparable.
        //      (Can't use class symbols here, we want type parameters as well.)
        val implementations = mutableMapOf<Pair<KotlinType, KotlinType>, IrClass>()
        irClass.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.descriptor !is SamConstructorDescriptor)
                    return super.visitCall(expression)
                // Direct construction of an interface:
                //    Runnable { print("Hello, World!") }
                return createInstance(expression.type, expression.getValueArgument(0)!!.transform(this, null))
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                if (expression.operator != IrTypeOperator.SAM_CONVERSION)
                    return super.visitTypeOperator(expression)
                // Implicit conversion to an interface:
                //    val a: (Runnable) -> Unit = { it.run() } // pretend this is a Java method
                //    val b: () -> Unit = { print("Hello, World!") }
                //    a(b)
                return createInstance(expression.typeOperand, expression.argument.transform(this, null))
            }

            // Replace a SAM-conversion of a callable object with an instance of an appropriate implementation.
            private fun createInstance(superType: IrType, invokable: IrExpression): IrExpression {
                val implementation = implementations.getOrPut(superType.toKotlinType() to invokable.type.toKotlinType()) {
                    createClass(superType, invokable.type)
                }
                // Running in the class context, so empty scope means this is a field/anonymous initializer.
                val scopeOwnerSymbol = currentScope?.scope?.scopeOwnerSymbol ?: irClass.symbol
                return context.createIrBuilder(scopeOwnerSymbol).irBlock(invokable, null, superType) {
                    // TODO if invokable is a callable reference, avoid materializing it?
                    if (superType.isNullable() && invokable.type.isNullable()) {
                        val invokableVariable = scope.createTemporaryVariable(invokable)
                        val instance = irCall(implementation.constructors.single()).apply { putValueArgument(0, irGet(invokableVariable)) }
                        +invokableVariable
                        +irIfNull(superType, irGet(invokableVariable), irNull(), instance)
                    } else {
                        +irCall(implementation.constructors.single()).apply { putValueArgument(0, invokable) }
                    }
                }
            }

            // Construct a class that wraps an invokable object into an implementation of an interface:
            //     class sam$n(private val invokable: F) : Interface() { override fun method(...) = invokable(...) }
            private fun createClass(superType: IrType, invokableType: IrType): IrClass {
                val invokableClass = invokableType.classifierOrFail.owner as IrClass
                val invoke = invokableClass.functions.single { it.name == OperatorNameConventions.INVOKE }

                val superClass = superType.classifierOrFail.owner as IrClass
                val superConstructor = if (superClass.kind != ClassKind.INTERFACE)
                    superClass.constructors.single { it.valueParameters.size == 0 }
                else
                    context.irBuiltIns.anyClass.owner.constructors.single { it.valueParameters.size == 0 }
                val superMethod = superClass.functions.single { it.modality == Modality.ABSTRACT }

                val superClassName = superClass.fqNameSafe.pathSegments().joinToString("_") { it.toString() }
                val subclass = buildClass {
                    // TODO this is not the name some tests (e.g. kt11519) expect
                    name = Name.identifier("sam\$${superClassName}\$${implementations.size}")
                    origin = JvmLoweredDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION
                }.apply {
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    parent = irClass
                    // TODO convert all type parameters to upper bounds? See the kt11696 test.
                    superTypes += superType
                }

                val invokableField = subclass.addField {
                    name = Name.identifier("invokable")
                    type = invokableType
                    origin = subclass.origin
                    visibility = Visibilities.PRIVATE
                }

                subclass.addConstructor {
                    returnType = subclass.defaultType
                    origin = subclass.origin
                    isPrimary = true
                }.apply {
                    addValueParameter {
                        name = invokableField.name
                        type = invokableType
                        index = 0
                        origin = subclass.origin
                    }
                    body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(superConstructor)
                        +irSetField(irGet(subclass.thisReceiver!!), invokableField, irGet(valueParameters[0]))
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, superClass.symbol, context.irBuiltIns.unitType)
                    }
                }

                subclass.addFunction {
                    name = superMethod.name
                    returnType = superMethod.returnType
                    visibility = superMethod.visibility
                    origin = subclass.origin
                }.apply {
                    overriddenSymbols.add(superMethod.symbol)
                    dispatchReceiverParameter = subclass.thisReceiver!!.copyTo(this)
                    superMethod.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
                    body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        +irReturn(irCall(invoke.symbol).apply {
                            dispatchReceiver = irGetField(irGet(subclass.thisReceiver!!), invokableField)
                            valueParameters.forEachIndexed { i, parameter -> putValueArgument(i, irGet(parameter)) }
                        })
                    }
                }

                return subclass
            }
        })
        irClass.declarations.addAll(implementations.values)
    }
}
