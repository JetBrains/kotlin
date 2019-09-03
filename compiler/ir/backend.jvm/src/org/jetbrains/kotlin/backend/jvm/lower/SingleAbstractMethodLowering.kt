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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
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
        val localImplementations = mutableListOf<IrClass>()
        irClass.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                if (expression.operator != IrTypeOperator.SAM_CONVERSION)
                    return super.visitTypeOperator(expression)
                val superType = expression.typeOperand
                val invokable = expression.argument.transform(this, null)
                // Running in the class context, so empty scope means this is a field/anonymous initializer.
                val scopeOwnerSymbol = currentScope?.scope?.scopeOwnerSymbol ?: irClass.symbol
                return context.createIrBuilder(scopeOwnerSymbol).irBlock(invokable, null, superType) {
                    // Not an exhaustive check, but enough to cover the most common cases.
                    if (invokable is IrFunctionReference) {
                        // Runnable(::function)
                        +createFunctionProxyInstance(superType, invokable)
                    } else if (invokable is IrBlock && invokable.statements.last() is IrFunctionReference) {
                        // Runnable { lambda }
                        for (statement in invokable.statements.dropLast(1))
                            +statement
                        +createFunctionProxyInstance(superType, invokable.statements.last() as IrFunctionReference)
                    } else {
                        // Fall back to materializing an invokable object.
                        +createObjectProxyInstance(superType, invokable, expression)
                    }
                }
            }

            // Construct a class that implements the specified SAM interface and contains a bunch of fields
            // in which the arguments to the constructor are stored:
            //     class sam$n(private val arg0: T0, ...) { override fun method(...) = <whatever buildOverride returns> }
            private fun implement(
                superType: IrType,
                parameters: List<IrType>,
                attributeOwner: IrAttributeContainer,
                buildOverride: IrSimpleFunction.(List<IrField>) -> IrBody
            ): IrClass {
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

                val fields = parameters.mapIndexed { i, fieldType ->
                    subclass.addField {
                        name = Name.identifier("arg$i")
                        type = fieldType
                        origin = subclass.origin
                        visibility = Visibilities.PRIVATE
                    }
                }

                subclass.addConstructor {
                    origin = subclass.origin
                    isPrimary = true
                }.apply {
                    for (field in fields) {
                        addValueParameter {
                            name = field.name
                            type = field.type
                            origin = subclass.origin
                        }
                    }
                    body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                        for ((field, parameter) in fields.zip(valueParameters))
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
                    overriddenSymbols.add(superMethod.symbol)
                    dispatchReceiverParameter = subclass.thisReceiver!!.copyTo(this)
                    superMethod.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
                    body = buildOverride(fields)
                }

                return subclass
            }

            // Construct a class that wraps an invokable object into an implementation of an interface:
            //     class sam$n(private val invokable: F) : Interface { override fun method(...) = invokable(...) }
            private fun createObjectProxy(superType: IrType, invokableType: IrType, attributeOwner: IrAttributeContainer): IrClass =
                implement(superType, listOf(invokableType), attributeOwner) { fields ->
                    context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        val invokableClass = invokableType.classifierOrFail.owner as IrClass
                        +irReturn(irCall(invokableClass.functions.single { it.name == OperatorNameConventions.INVOKE }).apply {
                            dispatchReceiver = irGetField(irGet(dispatchReceiverParameter!!), fields[0])
                            valueParameters.forEachIndexed { i, parameter -> putValueArgument(i, irGet(parameter)) }
                        })
                    }
                }

            private fun IrBlockBuilder.createObjectProxyInstance(
                superType: IrType, invokable: IrExpression, attributeOwner: IrAttributeContainer
            ): IrExpression {
                // Do not generate a wrapper class for null, it has no invoke() anyway.
                if (invokable.isNullConst())
                    return invokable
                val implementation = cachedImplementations.getOrPut(superType to invokable.type) {
                    createObjectProxy(superType, invokable.type, attributeOwner)
                }
                return if (superType.isNullable() && invokable.type.isNullable()) {
                    val invokableVariable = irTemporary(invokable)
                    val instance = irCall(implementation.constructors.single()).apply { putValueArgument(0, irGet(invokableVariable)) }
                    irIfNull(superType, irGet(invokableVariable), irNull(), instance)
                } else {
                    irCall(implementation.constructors.single()).apply { putValueArgument(0, invokable) }
                }
            }

            // This lowering is located after LocalDeclarationsLowering. On the one hand, this allows us to cache
            // object-wrapping classes without checking if the types are local declarations or not. On the other hand,
            // this means we need to manually create fields for all bound arguments when wrapping callable references.
            private val IrFunctionReference.arguments: List<IrExpression?>
                get() = listOf(dispatchReceiver).filter { symbol.owner.dispatchReceiverParameter != null } +
                        listOf(extensionReceiver).filter { symbol.owner.extensionReceiverParameter != null } +
                        (0 until valueArgumentsCount).map(::getValueArgument)

            // Construct a class that forwards method calls to an existing function/method:
            //     class sam$n(private val receiver: R) : Interface { override fun method(...) = receiver.target(...) }
            //
            // Unlike the above variant, this avoids materializing an invokable KFunction representing
            // the target in CallableReferenceLowering, thus producing one less class. This is actually very
            // common, as `Interface { something }` is a local function + a SAM-conversion of a reference
            // to it into an implementation.
            private fun createFunctionProxy(superType: IrType, reference: IrFunctionReference): IrClass =
                implement(superType, reference.arguments.mapNotNull { it?.type }, reference) { fields ->
                    context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                        +irReturn(irCall(reference.symbol).apply {
                            var boundOffset = 0
                            var offset = 0
                            val arguments = reference.arguments.map {
                                if (it != null)
                                    irGetField(irGet(dispatchReceiverParameter!!), fields[boundOffset++])
                                else
                                    irGet(valueParameters[offset++])
                            }.toMutableList()
                            if (reference.symbol.owner.dispatchReceiverParameter != null) {
                                dispatchReceiver = arguments.removeAt(0)
                            }
                            if (reference.symbol.owner.extensionReceiverParameter != null) {
                                extensionReceiver = arguments.removeAt(0)
                            }
                            arguments.forEachIndexed(::putValueArgument)
                        })
                    }
                }

            private fun IrBlockBuilder.createFunctionProxyInstance(superType: IrType, reference: IrFunctionReference): IrExpression {
                val implementation = createFunctionProxy(superType, reference)
                if (reference.origin == IrStatementOrigin.ANONYMOUS_FUNCTION || reference.origin == IrStatementOrigin.LAMBDA) {
                    val target = reference.symbol.owner
                    implementation.functions.single().apply {
                        annotations.addAll(target.annotations)
                        valueParameters.forEachIndexed { i, p ->
                            p.annotations.addAll(target.valueParameters[i].annotations)
                        }
                    }
                }
                localImplementations += implementation
                return irCall(implementation.constructors.single()).apply {
                    reference.arguments.filterNotNull().forEachIndexed(::putValueArgument)
                }
            }
        })
        irClass.declarations += cachedImplementations.values
        irClass.declarations += localImplementations
    }
}
