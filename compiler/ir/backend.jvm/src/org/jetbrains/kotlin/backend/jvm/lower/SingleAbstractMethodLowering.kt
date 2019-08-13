/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::SingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes",
    prerequisite = setOf(localDeclarationsPhase)
)

class SingleAbstractMethodLowering(val context: CommonBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val cachedImplementations = mutableMapOf<Pair<IrType, IrType>, IrClass>()
        irClass.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                if (expression.operator != IrTypeOperator.SAM_CONVERSION)
                    return super.visitTypeOperator(expression)
                val superType = expression.typeOperand
                val invokable = expression.argument.transform(this, null)
                // Running in the class context, so empty scope means this is a field/anonymous initializer.
                val scopeOwnerSymbol = currentScope?.scope?.scopeOwnerSymbol ?: irClass.symbol
                context.createIrBuilder(scopeOwnerSymbol).apply {
                    // Do not generate a wrapper class for null, it has no invoke() anyway.
                    if (invokable.isNullConst())
                        return invokable

                    val implementation = cachedImplementations.getOrPut(superType to invokable.type) {
                        createObjectProxy(superType, invokable.type, expression)
                    }

                    return if (superType.isNullable() && invokable.type.isNullable()) {
                        irBlock(invokable, null, superType) {
                            val invokableVariable = irTemporary(invokable)
                            val instance = irCall(implementation.constructors.single()).apply {
                                putValueArgument(0, irGet(invokableVariable))
                            }
                            irIfNull(superType, irGet(invokableVariable), irNull(), instance)
                        }
                    } else {
                        irCall(implementation.constructors.single()).apply { putValueArgument(0, invokable) }
                    }
                }
            }

            // Construct a class that wraps an invokable object into an implementation of an interface:
            //     class sam$n(private val invokable: F) : Interface { override fun method(...) = invokable(...) }
            private fun createObjectProxy(superType: IrType, invokableType: IrType, attributeOwner: IrAttributeContainer): IrClass {
                val superClass = superType.classifierOrFail.owner as IrClass
                // The language documentation prohibits casting lambdas to classes, but if it was allowed,
                // the `irDelegatingConstructorCall` in the constructor below would need to be modified.
                assert(superClass.kind == ClassKind.INTERFACE) { "SAM conversion to an abstract class not allowed" }

                val subclass = buildClass {
                    name = Name.special("<sam adapter for ${superClass.fqNameWhenAvailable}>")
                    origin = JvmLoweredDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION
                }.apply {
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    parent = irClass
                    // TODO convert all type parameters to upper bounds? See the kt11696 test.
                    superTypes += superType
                }.copyAttributes(attributeOwner)

                val field = subclass.addField {
                    name = Name.identifier("arg0")
                    type = invokableType
                    origin = subclass.origin
                    visibility = Visibilities.PRIVATE
                }

                subclass.addConstructor {
                    origin = subclass.origin
                    isPrimary = true
                }.apply {
                    val parameter = addValueParameter {
                        name = field.name
                        type = field.type
                        origin = subclass.origin
                    }

                    body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                        +irSetField(irGet(subclass.thisReceiver!!), field, irGet(parameter))
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, subclass.symbol, context.irBuiltIns.unitType)
                    }
                }

                val superMethod = superClass.functions.single { it.modality == Modality.ABSTRACT }
                subclass.addFunction {
                    name = superMethod.name
                    returnType = superMethod.returnType
                    visibility = superMethod.visibility
                    origin = subclass.origin
                }.apply {
                    overriddenSymbols += superMethod.symbol
                    dispatchReceiverParameter = subclass.thisReceiver!!.copyTo(this)
                    superMethod.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
                    val invokableClass = invokableType.classifierOrFail.owner as IrClass
                    body = context.createIrBuilder(symbol).run {
                        irExprBody(irCall(invokableClass.functions.single { it.name == OperatorNameConventions.INVOKE }).apply {
                            dispatchReceiver = irGetField(irGet(dispatchReceiverParameter!!), field)
                            valueParameters.forEachIndexed { i, parameter -> putValueArgument(i, irGet(parameter)) }
                        })
                    }
                }

                return subclass
            }
        })
        irClass.declarations += cachedImplementations.values
    }
}
