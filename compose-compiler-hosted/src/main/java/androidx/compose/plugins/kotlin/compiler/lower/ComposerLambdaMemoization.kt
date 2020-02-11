/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.hasUntrackedAnnotation
import androidx.compose.plugins.kotlin.irTrace
import androidx.compose.plugins.kotlin.isEmitInline
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.typeUtil.isUnit

private class CaptureCollector {
    val captures = mutableSetOf<IrValueDeclaration>()

    fun recordCapture(local: IrValueDeclaration) {
        captures.add(local)
    }
}

private sealed class MemoizationContext {
    open val composable get() = false
    open fun declareLocal(local: IrValueDeclaration?) { }
    open fun recordCapture(local: IrValueDeclaration?) { }
    open fun pushCollector(collector: CaptureCollector) { }
    open fun popCollector(collector: CaptureCollector) { }
}

private class ClassContext : MemoizationContext()

private class FunctionContext(val declaration: IrFunction, override val composable: Boolean) :
    MemoizationContext() {
    val locals = mutableSetOf<IrValueDeclaration>()
    var collectors = mutableListOf<CaptureCollector>()

    init {
        declaration.valueParameters.forEach {
            declareLocal(it)
        }
    }

    override fun declareLocal(local: IrValueDeclaration?) {
        if (local != null) {
            locals.add(local)
        }
    }

    override fun recordCapture(local: IrValueDeclaration?) {
        if (local != null && collectors.isNotEmpty() && locals.contains(local)) {
            for (collector in collectors) {
                collector.recordCapture(local)
            }
        }
    }

    override fun pushCollector(collector: CaptureCollector) {
        collectors.add(collector)
    }

    override fun popCollector(collector: CaptureCollector) {
        require(collectors.lastOrNull() == collector)
        collectors.removeAt(collectors.size - 1)
    }
}

class ComposerLambdaMemoization(
    context: JvmBackendContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace),
    ModuleLoweringPass {

    private val memoizationContextStack = mutableListOf<MemoizationContext>()

    override fun lower(module: IrModuleFragment) = module.transformChildrenVoid(this)

    override fun visitClass(declaration: IrClass): IrStatement {
        memoizationContextStack.push(ClassContext())
        val result = super.visitClass(declaration)
        memoizationContextStack.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val descriptor = declaration.descriptor
        val composable = descriptor.isComposable() &&
                // Don't memoize in an inline function
                !descriptor.isInline &&
                // Don't memoize if in a composable that returns a value
                // TODO(b/150390108): Consider allowing memoization in effects
                descriptor.returnType.let { it != null && it.isUnit() }

        memoizationContextStack.push(FunctionContext(declaration, composable))
        val result = super.visitFunction(declaration)
        memoizationContextStack.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        memoizationContextStack.peek()?.declareLocal(declaration)
        return super.visitVariable(declaration)
    }

    override fun visitValueAccess(expression: IrValueAccessExpression): IrExpression {
        memoizationContextStack.forEach { it.recordCapture(expression.symbol.owner) }
        return super.visitValueAccess(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        // Memoize the instance created by using the :: prefix
        val result = super.visitFunctionReference(expression)
        val memoizationContext = memoizationContextStack.peek() as? FunctionContext
            ?: return result
        if (expression.valueArgumentsCount != 0) {
            // If this syntax is as a curry syntax in the future, don't memoize.
            // The syntax <expr>::<method>(<params>) and ::<function>(<params>) is reserved for
            // future use. This ensures we don't try to memoize this syntax without knowing
            // its meaning.

            // The most likely correct implementation is to treat the parameters exactly as the
            // receivers are treated below.
            return result
        }
        if (memoizationContext.composable) {
            // Memoize the reference for <expr>::<method>
            val dispatchReceiver = expression.dispatchReceiver
            val extensionReceiver = expression.extensionReceiver
            if ((dispatchReceiver != null || extensionReceiver != null) &&
                        dispatchReceiver.isNullOrStable() &&
                        extensionReceiver.isNullOrStable()) {
                // Save the receivers into a temporaries and memoize the function reference using
                // the resulting temporaries
                val builder = context.createIrBuilder(
                    memoizationContext.declaration.symbol,
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset
                )
                return builder.irBlock(
                    resultType = expression.type
                ) {
                    val captures = mutableListOf<IrValueDeclaration>()

                    val tempDispatchReceiver = dispatchReceiver?.let {
                        val tmp = irTemporary(it)
                        captures.add(tmp)
                        tmp
                    }
                    val tempExtensionReceiver = extensionReceiver?.let {
                        val tmp = irTemporary(it)
                        captures.add(tmp)
                        tmp
                    }

                    +rememberExpression(
                        memoizationContext,
                        IrFunctionReferenceImpl(
                            startOffset,
                            endOffset,
                            expression.type,
                            expression.symbol,
                            expression.descriptor,
                            expression.typeArgumentsCount).copyAttributes(expression).apply {
                            this.dispatchReceiver = tempDispatchReceiver?.let { irGet(it) }
                            this.extensionReceiver = tempExtensionReceiver?.let { irGet(it) }
                        },
                        captures
                    )
                }
            } else if (dispatchReceiver == null) {
                return rememberExpression(memoizationContext, result, emptyList())
            }
        }
        return result
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        // Start recording variables captured in this scope
        val composableFunction = memoizationContextStack.peek() as? FunctionContext
            ?: return super.visitFunctionExpression(expression)
        if (!composableFunction.composable) return super.visitFunctionExpression(expression)

        val collector = CaptureCollector()
        startCollector(collector)
        // Wrap composable functions expressions or memoize non-composable function expressions
        val result = super.visitFunctionExpression(expression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        // Ensure we don't transform targets of an inline function
        if (functionExpression.isInlineArgument()) return functionExpression
        if (functionExpression.isComposable()) {
            return functionExpression
        }

        if (functionExpression.function.descriptor.hasUntrackedAnnotation())
            return functionExpression

        return rememberExpression(
            composableFunction,
            functionExpression,
            collector.captures.toList()
        )
    }

    private fun startCollector(collector: CaptureCollector) {
        for (memoizationContext in memoizationContextStack) {
            memoizationContext.pushCollector(collector)
        }
    }

    private fun stopCollector(collector: CaptureCollector) {
        for (memoizationContext in memoizationContextStack) {
            memoizationContext.popCollector(collector)
        }
    }

    private fun rememberExpression(
        memoizationContext: FunctionContext,
        expression: IrExpression,
        captures: List<IrValueDeclaration>
    ): IrExpression {

        if (captures.any {
            !((it as? IrVariable)?.isVar != true && it.type.toKotlinType().isStable())
        }) return expression
        val rememberParameterCount = captures.size + 1 // One additional parameter for the lambda
        val declaration = memoizationContext.declaration
        val descriptor = declaration.descriptor
        val module = descriptor.module
        val rememberFunctions = module
            .getPackage(FqName(ComposeUtils.generateComposePackageName()))
            .memberScope
            .getContributedFunctions(
                Name.identifier("remember"),
                NoLookupLocation.FROM_BACKEND
            )
        val directRememberFunction = // Exclude the varargs version
            rememberFunctions.singleOrNull {
                it.valueParameters.size == rememberParameterCount &&
                        // Exclude the varargs version
                        it.valueParameters.firstOrNull()?.varargElementType == null
            }
        val rememberFunctionDescriptor = directRememberFunction
            ?: rememberFunctions.single {
                // Use the varargs version
                it.valueParameters.firstOrNull()?.varargElementType != null
            }

        val rememberFunctionSymbol = referenceSimpleFunction(rememberFunctionDescriptor)

        val irBuilder = context.createIrBuilder(
            symbol = declaration.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        return irBuilder.irCall(
            callee = rememberFunctionSymbol,
            descriptor = rememberFunctionDescriptor,
            type = expression.type
        ).apply {
            // The result type type parameter is first, followed by the argument types
            putTypeArgument(0, expression.type)
            val lambdaArgumentIndex = if (directRememberFunction != null) {
                // Call to the non-vararg version
                for (i in 1..captures.size) {
                    putTypeArgument(i, captures[i - 1].type)
                }

                // condition arguments are the first `arg.size` arguments
                for (i in captures.indices) {
                    putValueArgument(i, irBuilder.irGet(captures[i]))
                }
                // The lambda is the last parameter
                captures.size
            } else {
                val parameterType = rememberFunctionDescriptor.valueParameters[0].type.toIrType()
                // Call to the vararg version
                putValueArgument(0,
                    IrVarargImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = parameterType,
                        varargElementType = context.irBuiltIns.anyType,
                        elements = captures.map {
                            irBuilder.irGet(it)
                        }
                    )
                )
                1
            }

            putValueArgument(lambdaArgumentIndex, irBuilder.irLambdaExpression(
                descriptor = irBuilder.createFunctionDescriptor(
                    rememberFunctionDescriptor.valueParameters.last().type
                ),
                type = rememberFunctionDescriptor.valueParameters.last().type.toIrType(),
                body = {
                    +irReturn(expression)
                }
            ))
        }.patchDeclarationParents(declaration).markAsSynthetic()
    }

    private fun <T : IrFunctionAccessExpression> T.markAsSynthetic(): T {
        // Mark it so the ComposableCallTransformer will insert the correct code around this call
        context.state.irTrace.record(
            ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL,
            this,
            true
        )
        return this
    }

    private fun IrFunctionExpression.isInlineArgument(): Boolean {
        function.descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.state.bindingContext,
                        false
                    )
                )
                    return true
                if (it.isEmitInline(context.state.bindingContext)) {
                    return true
                }
            }
        }
        return false
    }

    fun IrExpression?.isNullOrStable() = this == null || type.toKotlinType().isStable()
}
