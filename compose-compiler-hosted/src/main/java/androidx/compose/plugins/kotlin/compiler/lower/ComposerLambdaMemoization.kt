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

import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.kotlin.ComposeUtils.composeInternalFqName
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.hasUntrackedAnnotation
import androidx.compose.plugins.kotlin.irTrace
import androidx.compose.plugins.kotlin.isEmitInline
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.getDeclaration
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.typeUtil.isUnit

private class CaptureCollector {
    val captures = mutableSetOf<IrValueDeclaration>()

    fun recordCapture(local: IrValueDeclaration) {
        captures.add(local)
    }
}

private abstract class DeclarationContext {
    abstract val composable: Boolean
    abstract val symbol: IrSymbol
    abstract fun declareLocal(local: IrValueDeclaration?)
    abstract fun recordCapture(local: IrValueDeclaration?)
    abstract fun pushCollector(collector: CaptureCollector)
    abstract fun popCollector(collector: CaptureCollector)
}

private class SymbolOwnerContext(val declaration: IrSymbolOwner) : DeclarationContext() {
    override val composable get() = false
    override val symbol get() = declaration.symbol
    override fun declareLocal(local: IrValueDeclaration?) { }
    override fun recordCapture(local: IrValueDeclaration?) { }
    override fun pushCollector(collector: CaptureCollector) { }
    override fun popCollector(collector: CaptureCollector) { }
}

private class FunctionContext(
    val declaration: IrFunction,
    override val composable: Boolean,
    val canRemember: Boolean
) : DeclarationContext() {
    override val symbol get() = declaration.symbol

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

const val RESTARTABLE_FUNCTION = "restartableFunction"
const val RESTARTABLE_FUNCTION_N = "restartableFunctionN"
const val RESTARTABLE_FUNCTION_INSTANCE = "restartableFunctionInstance"
const val RESTARTABLE_FUNCTION_N_INSTANCE = "restartableFunctionNInstance"

class ComposerLambdaMemoization(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace),
    ModuleLoweringPass {

    private val declarationContextStack = mutableListOf<DeclarationContext>()

    override fun lower(module: IrModuleFragment) = module.transformChildrenVoid(this)

    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
        if (declaration is IrFunction) return super.visitDeclaration(declaration)
        val symbolOwner = declaration as? IrSymbolOwner
        if (symbolOwner != null) declarationContextStack.push(SymbolOwnerContext(declaration))
        val result = super.visitDeclaration(declaration)
        if (symbolOwner != null) declarationContextStack.pop()
        return result
    }

    private fun irCurrentComposer(): IrExpression {
        val currentComposerSymbol = getTopLevelPropertyGetter(
            ComposeUtils.composeFqName("currentComposer")
        )
        return irCall(currentComposerSymbol)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val descriptor = declaration.descriptor
        val composable = descriptor.isComposable()
        val canRemember = composable &&
                // Don't use remember in an inline function
                !descriptor.isInline &&
                // Don't use remember if in a composable that returns a value
                // TODO(b/150390108): Consider allowing remember in effects
                descriptor.returnType.let { it != null && it.isUnit() }

        declarationContextStack.push(FunctionContext(declaration, composable, canRemember))
        val result = super.visitFunction(declaration)
        declarationContextStack.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declarationContextStack.peek()?.declareLocal(declaration)
        return super.visitVariable(declaration)
    }

    override fun visitValueAccess(expression: IrValueAccessExpression): IrExpression {
        declarationContextStack.forEach { it.recordCapture(expression.symbol.owner) }
        return super.visitValueAccess(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        // Memoize the instance created by using the :: operator
        val result = super.visitFunctionReference(expression)
        val functionContext = declarationContextStack.peek() as? FunctionContext
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
        if (functionContext.canRemember) {
            // Memoize the reference for <expr>::<method>
            val dispatchReceiver = expression.dispatchReceiver
            val extensionReceiver = expression.extensionReceiver
            if ((dispatchReceiver != null || extensionReceiver != null) &&
                        dispatchReceiver.isNullOrStable() &&
                        extensionReceiver.isNullOrStable()) {
                // Save the receivers into a temporaries and memoize the function reference using
                // the resulting temporaries
                val builder = DeclarationIrBuilder(
                    generatorContext = context,
                    symbol = functionContext.symbol,
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
                        functionContext,
                        IrFunctionReferenceImpl(
                            startOffset,
                            endOffset,
                            expression.type,
                            expression.symbol,
                            expression.typeArgumentsCount).copyAttributes(expression).apply {
                            this.dispatchReceiver = tempDispatchReceiver?.let { irGet(it) }
                            this.extensionReceiver = tempExtensionReceiver?.let { irGet(it) }
                        },
                        captures
                    )
                }
            } else if (dispatchReceiver == null) {
                return rememberExpression(functionContext, result, emptyList())
            }
        }
        return result
    }

    private fun visitNonComposableFunctionExpression(
        expression: IrFunctionExpression,
        declarationContext: DeclarationContext
    ): IrExpression {
        val functionContext = declarationContext as? FunctionContext
            ?: return super.visitFunctionExpression(expression)

        if (
            // Only memoize non-composable lambdas in a context we can use remember
            !functionContext.canRemember ||
            // Don't memoize inlined lambdas
            expression.isInlineArgument() ||
            // Don't memoize untracked function
            !expression.isTracked()) {
            return super.visitFunctionExpression(expression)
        }

        // Record capture variables for this scope
        val collector = CaptureCollector()
        startCollector(collector)
        // Wrap composable functions expressions or memoize non-composable function expressions
        val result = super.visitFunctionExpression(expression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        return rememberExpression(
            functionContext,
            functionExpression,
            collector.captures.toList()
        )
    }

    private fun visitComposableFunctionExpression(
        expression: IrFunctionExpression,
        declarationContext: DeclarationContext
    ): IrExpression {
        val result = super.visitFunctionExpression(expression)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        // Do not wrap target of an inline function
        if (expression.isInlineArgument()) return functionExpression

        // Do not wrap composable lambdas with return results
        if (!functionExpression.function.descriptor.returnType.let { it == null || it.isUnit() }) {
            return functionExpression
        }

        return wrapFunctionExpression(declarationContext, functionExpression)
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        val declarationContext = declarationContextStack.peek()
            ?: return super.visitFunctionExpression(expression)
        return if (expression.isComposable())
            visitComposableFunctionExpression(expression, declarationContext)
        else
            visitNonComposableFunctionExpression(expression, declarationContext)
    }

    private fun startCollector(collector: CaptureCollector) {
        for (declarationContext in declarationContextStack) {
            declarationContext.pushCollector(collector)
        }
    }

    private fun stopCollector(collector: CaptureCollector) {
        for (declarationContext in declarationContextStack) {
            declarationContext.popCollector(collector)
        }
    }

    private fun wrapFunctionExpression(
        declarationContext: DeclarationContext,
        expression: IrFunctionExpression
    ): IrExpression {
        val function = expression.function
        val argumentCount = function.descriptor.valueParameters.size
        val useRestartableFunctionN = argumentCount > MAX_RESTART_ARGUMENT_COUNT
        val restartFunctionFactory =
            if (declarationContext.composable)
                if (useRestartableFunctionN)
                    RESTARTABLE_FUNCTION_N
                else RESTARTABLE_FUNCTION
            else if (useRestartableFunctionN)
                RESTARTABLE_FUNCTION_N_INSTANCE
                else RESTARTABLE_FUNCTION_INSTANCE
        val restartFactorySymbol =
            getTopLevelFunction(composeInternalFqName(restartFunctionFactory))
        val irBuilder = DeclarationIrBuilder(context,
            symbol = declarationContext.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        context.irProviders.getDeclaration(restartFactorySymbol)
        return irBuilder.irCall(restartFactorySymbol).apply {
            var index = 0

            // first parameter is the composer parameter if we are ina  composable context
            if (declarationContext.composable) {
                putValueArgument(
                    index++,
                    irCurrentComposer()
                )
            }

            // key parameter
            putValueArgument(
                index++, irBuilder.irInt(
                    symbol.descriptor.fqNameSafe.hashCode() xor expression.startOffset
                )
            )

            // tracked parameter
            putValueArgument(index++, irBuilder.irBoolean(expression.isTracked()))

            // RestartableFunctionN requires the arity
            if (useRestartableFunctionN) {
                // arity parameter
                putValueArgument(index++, irBuilder.irInt(argumentCount))
            }

            // block parameter
            putValueArgument(index, expression)
        }
    }

    private fun rememberExpression(
        functionContext: FunctionContext,
        expression: IrExpression,
        captures: List<IrValueDeclaration>
    ): IrExpression {
        if (captures.any {
            !((it as? IrVariable)?.isVar != true && it.type.toKotlinType().isStable())
        }) return expression
        val rememberParameterCount = captures.size + 1 // One additional parameter for the lambda
        val declaration = functionContext.declaration
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

        val irBuilder = DeclarationIrBuilder(
            generatorContext = context,
            symbol = functionContext.symbol,
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
        }.patchDeclarationParents(declaration).markAsSynthetic(mark = true)
    }

    private fun <T : IrFunctionAccessExpression> T.markAsSynthetic(mark: Boolean): T {
        if (mark) {
            // Mark it so the ComposableCallTransformer will insert the correct code around this
            // call
            context.irTrace.record(
                ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL,
                this,
                true
            )
        }
        return this
    }

    private fun IrFunctionExpression.isInlineArgument(): Boolean {
        function.descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.bindingContext,
                        false
                    )
                )
                    return true
                if (it.isEmitInline(context.bindingContext)) {
                    return true
                }
            }
        }
        return false
    }

    private fun IrExpression?.isNullOrStable() = this == null || type.toKotlinType().isStable()
    private fun IrFunctionExpression.isTracked() = !function.descriptor.hasUntrackedAnnotation()
}

// This must match the highest value of FunctionXX which is current Function22
private const val MAX_RESTART_ARGUMENT_COUNT = 22
