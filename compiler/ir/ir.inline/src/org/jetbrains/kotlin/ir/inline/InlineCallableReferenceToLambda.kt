/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.backend.common.lower.LoweredDeclarationOrigins
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.toBuilder
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name

private val STUB_FOR_INLINING = Name.identifier("stub_for_inlining")

fun IrFunction.isStubForInline() = name == STUB_FOR_INLINING && origin == LoweredDeclarationOrigins.INLINE_LAMBDA

/**
 * This lowering transforms inlined callable references to lambdas. Callable reference is inlined if it's passed to a non-noinline
 * parameter of an inline function.
 *
 *     inline fun foo(inlineParameter: (A) -> B): B {
 *         return inlineParameter()
 *     }
 *
 * `foo(::smth)` is transformed to `foo { a -> smth(a) }`.
 */
abstract class InlineCallableReferenceToLambdaPhase(
    val context: LoweringContext,
    protected val inlineFunctionResolver: InlineFunctionResolver,
) : FileLoweringPass, IrTransformer<IrDeclarationParent?>() {
    override fun lower(irFile: IrFile) {
        irFile.transform(this, null)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent?) =
        super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclarationParent?): IrElement {
        expression.transformChildren(this, data)
        if (inlineFunctionResolver.needsInlining(expression)) {
            val function = expression.symbol.owner
            for (parameter in function.parameters) {
                if (parameter.isInlineParameter()) {
                    expression.arguments[parameter] = expression.arguments[parameter]?.transformToLambda(data)
                }
            }
        }

        return expression
    }

    protected fun IrExpression.transformToLambda(scope: IrDeclarationParent?) = when {
        this is IrBlock && origin.isInlinable -> apply {
            // Already a lambda or similar, just mark it with an origin.
            val reference = statements.last() as IrFunctionReference
            reference.symbol.owner.origin = LoweredDeclarationOrigins.INLINE_LAMBDA
            reference.origin = LoweredStatementOrigins.INLINE_LAMBDA
        }

        this is IrFunctionReference -> {
            // ::function -> { args... -> function(args...) }
            wrapFunction(symbol.owner).toLambda(this, scope!!)
        }

        this is IrPropertyReference -> {
            val isReferenceToSyntheticJavaProperty = symbol.owner.origin.let {
                it == IrDeclarationOrigin.SYNTHETIC_JAVA_PROPERTY_DELEGATE || it == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            }
            when {
                // References to generic synthetic Java properties aren't inlined in K1. Fixes KT-57103
                typeArguments.isNotEmpty() && isReferenceToSyntheticJavaProperty -> this
                // ::property -> { receiver -> receiver.property }; prefer direct field access if allowed.
                field != null -> wrapField(field!!.owner).toLambda(this, scope!!)
                else -> wrapFunction(getter!!.owner).toLambda(this, scope!!)
            }
        }


        else -> this // not an inline argument
    }

    private fun IrPropertyReference.wrapField(field: IrField): IrSimpleFunction =
        context.irFactory.buildFun {
            setSourceRange(this@wrapField)
            origin = LoweredDeclarationOrigins.INLINE_LAMBDA
            name = STUB_FOR_INLINING
            visibility = DescriptorVisibilities.LOCAL
            returnType = field.type
        }.apply {
            body = context.createIrBuilder(symbol).run {
                val boundReceiver = boundReceiver()
                val fieldReceiver = when {
                    field.isStatic -> null
                    boundReceiver != null -> irGet(addExtensionReceiver(boundReceiver.type))
                    else -> irGet(addValueParameter("receiver", field.parentAsClass.defaultType))
                }
                irBlockBody {
                    val exprToReturn = irGetField(fieldReceiver, field)
                    +irReturn(exprToReturn)
                }
            }
        }

    private fun IrCallableReference<*>.wrapFunction(referencedFunction: IrFunction): IrSimpleFunction =
        context.irFactory.buildFun {
            setSourceRange(this@wrapFunction)
            origin = LoweredDeclarationOrigins.INLINE_LAMBDA
            name = STUB_FOR_INLINING
            visibility = DescriptorVisibilities.LOCAL
            returnType = ((type as IrSimpleType).arguments.last() as IrTypeProjection).type
            isSuspend = referencedFunction.isSuspend
        }.apply {
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                val boundReceiver = boundReceiver()
                val boundReceiverParameter = when {
                    dispatchReceiver != null -> referencedFunction.dispatchReceiverParameter
                    boundReceiver != null -> referencedFunction.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
                    else -> null
                }

                // TODO: could there be a star projection here?
                val unboundArgumentTypes = (type as IrSimpleType).arguments.dropLast(1).map { (it as IrTypeProjection).type }
                val argumentTypes = getAllArgumentsWithIr()
                    .filter { it.first != boundReceiverParameter }
                    .map { it.second }
                    .let { boundArguments ->
                        var i = 0
                        // if the argument is bound, then use the argument's type, otherwise take a type from reference's return type
                        boundArguments.map { it?.type ?: unboundArgumentTypes[i++] }
                    }

                irBlockBody {
                    val exprToReturn = irCall(referencedFunction.symbol, returnType).apply {
                        copyTypeArgumentsFrom(this@wrapFunction)
                        for (parameter in referencedFunction.parameters) {
                            val next = parameters.count { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
                            val getOnNewParameter = when {
                                boundReceiverParameter == parameter -> irGet(addExtensionReceiver(boundReceiver!!.type))
                                next >= argumentTypes.size ->
                                    error(
                                        "The number of parameters for reference and referenced function is different\n" +
                                                "Reference: ${this@wrapFunction.render()}\n" +
                                                "Referenced function: ${referencedFunction.render()}\n"
                                    )
                                parameter.isVararg && parameter.type == argumentTypes[next] ->
                                    irGet(addValueParameter("p$next", argumentTypes[next]))
                                parameter.isVararg && !parameter.hasDefaultValue() ->
                                    error("Callable reference with vararg should not appear at this stage.\n${this@wrapFunction.render()}")
                                else -> irGet(addValueParameter("p$next", argumentTypes[next]))
                            }
                            arguments[parameter] = getOnNewParameter
                        }
                    }
                    +irReturn(exprToReturn)
                }
            }
        }

    private fun IrSimpleFunction.toLambda(original: IrCallableReference<*>, scope: IrDeclarationParent) =
        context.createIrBuilder(symbol).irBlock(startOffset, endOffset, IrStatementOrigin.LAMBDA) {
            this@toLambda.parent = scope
            +this@toLambda
            +IrFunctionReferenceImpl.fromSymbolOwner(
                startOffset, endOffset, original.type.convertKPropertyToKFunction(context.irBuiltIns), symbol,
                typeArgumentsCount = 0, reflectionTarget = null,
                origin = LoweredStatementOrigins.INLINE_LAMBDA
            ).apply {
                copyAttributes(original)
                if (original is IrFunctionReference) {
                    // It is required to copy value arguments if any.
                    // Don't need to copy the dispatch receiver because it was remapped on extension receiver.
                    original.symbol.owner.parameters.zip(original.arguments).forEach { (parameter, argument) ->
                        if (parameter.kind == IrParameterKind.Regular || parameter.kind == IrParameterKind.Context) {
                            arguments[parameter.indexInParameters] = argument
                        }
                    }
                }
                val receiver = original.boundReceiver()
                receiver?.let { arguments[0] = it }
            }
        }
}

private val IrStatementOrigin?.isInlinable: Boolean
    get() = isLambda || this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || this == IrStatementOrigin.SUSPEND_CONVERSION

private fun IrType.convertKPropertyToKFunction(irBuiltIns: IrBuiltIns): IrType {
    if (this !is IrSimpleType) return this
    if (!this.isKProperty() && !this.isKMutableProperty()) return this

    return this.toBuilder().apply { classifier = irBuiltIns.functionN(arguments.size - 1).symbol }.buildSimpleType()
}

// Returns dispatch or extension receiver of function or property reference, if any. Otherwise, return null.
fun IrCallableReference<*>.boundReceiver(): IrExpression? =
    when (val owner = symbol.owner) {
        is IrFunction -> arguments.getOrNull(owner.parameters.indexOfFirst {
            it.kind == IrParameterKind.DispatchReceiver || it.kind == IrParameterKind.ExtensionReceiver
        })
        is IrProperty -> {
            val boundArgs = arguments.filterNotNull()
            when (boundArgs.size) {
                0 -> null
                1 -> boundArgs[0]
                else -> error("Several bound arguments is not supported yet")
            }
        }
        else -> null
    }
