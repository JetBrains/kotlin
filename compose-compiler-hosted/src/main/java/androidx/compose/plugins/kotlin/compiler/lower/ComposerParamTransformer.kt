/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.KtxNameConventions
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.util.OperatorNameConventions

private const val DEBUG_LOG = false

class ComposerParamTransformer(val context: JvmBackendContext) :
    IrElementTransformerVoid(),
    FileLoweringPass {

    private val transformedFunctions: MutableMap<IrFunction, IrFunction>
        get() = context.suspendFunctionViews

    private val transformedFunctionSet = mutableSetOf<IrFunction>()

    private val composableChecker = ComposableAnnotationChecker()

    private val typeTranslator =
        TypeTranslator(
            context.ir.symbols.externalSymbolTable,
            context.state.languageVersionSettings,
            context.builtIns
        ).apply {
            constantValueGenerator = ConstantValueGenerator(
                context.state.module,
                context.ir.symbols.externalSymbolTable
            )
            constantValueGenerator.typeTranslator = this
        }

    private val builtIns = context.irBuiltIns

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    private val composerTypeDescriptor = context.state.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(ComposeFqNames.Composer)
    ) ?: error("Cannot find the Composer class")

    private val composerType = composerTypeDescriptor.defaultType.toIrType()

    override fun lower(irFile: IrFile) {
        if (DEBUG_LOG) {
            println("""
                =========
                BEFORE
                =========
            """.trimIndent())
            println(irFile.dump())
        }
        irFile.transformChildrenVoid(this)
        if (DEBUG_LOG) {
            println("""
                =========
                AFTER TRANSFORM
                =========
            """.trimIndent())
            println(irFile.dump())
        }
        val symbolRemapper = DeepCopySymbolRemapper()
        irFile.acceptVoid(symbolRemapper)
        irFile.transformChildren(
            DeepCopyIrTreeWithSymbols(
                symbolRemapper,
                ComposerTypeRemapper(context, typeTranslator, composerTypeDescriptor)
            ),
            null
        )
        irFile.patchDeclarationParents()
        if (DEBUG_LOG) {
            println("""
                =========
                AFTER TYPE REMAPPING
                =========
            """.trimIndent())
            println(irFile.dump())
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration.withComposerParamIfNeeded())
    }

    fun IrCall.withComposerParamIfNeeded(composerParam: IrValueParameter): IrCall {
        val isComposableLambda = isComposableLambdaInvoke()
        if (!descriptor.isComposable() && !isComposableLambda)
            return this
        val ownerFn = when {
            isComposableLambda ->
                (symbol.owner as IrSimpleFunction).lambdaInvokeWithComposerParamIfNeeded()
            else -> (symbol.owner as IrSimpleFunction).withComposerParamIfNeeded()
        }
        if (!transformedFunctionSet.contains(ownerFn))
            return this
        if (symbol.owner == ownerFn)
            return this
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            ownerFn.symbol
        ).also {
            it.copyTypeArgumentsFrom(this)
            it.dispatchReceiver = dispatchReceiver
            it.extensionReceiver = extensionReceiver
            for (i in 0 until valueArgumentsCount) {
                it.putValueArgument(i, getValueArgument(i))
            }
            it.putValueArgument(
                valueArgumentsCount,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    composerParam.symbol
                )
            )
        }
    }

    // Transform `@Composable fun foo(params): RetType` into `fun foo(params, $composer: Composer): RetType`
    fun IrFunction.withComposerParamIfNeeded(): IrFunction {
        // don't transform functions that themselves were produced by this function. (ie, if we
        // call this with a function that has the synthetic composer parameter, we don't want to
        // transform it further).
        if (transformedFunctionSet.contains(this)) return this

        // if not a composable fn, nothing we need to do
        if (!descriptor.isComposable()) return this

        // TODO(lmr): it looks like inlined non-composable lambdas are getting marked as
        // composable by the ComposableAnnotationChecker. This makes this line a requirement for
        // two reasons instead of one. We should fix this.
        if (isInlinedLambda()) return this

        // cache the transformed function with composer parameter
        return transformedFunctions[this] ?: copyWithComposerParam()
    }

    fun IrFunction.lambdaInvokeWithComposerParamIfNeeded(): IrFunction {
        if (transformedFunctionSet.contains(this)) return this
        return transformedFunctions.getOrPut(this) {
            lambdaInvokeWithComposerParam().also { transformedFunctionSet.add(it) }
        }
    }

    fun IrFunction.lambdaInvokeWithComposerParam(): IrFunction {
        val descriptor = descriptor
        val argCount = descriptor.valueParameters.size
        val newFnClass = context.irIntrinsics.symbols.getFunction(argCount + 1)
        val newDescriptor = newFnClass.descriptor.unsubstitutedMemberScope.findFirstFunction(
            OperatorNameConventions.INVOKE.identifier
        ) { true }

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            newDescriptor,
            newDescriptor.returnType?.toIrType()!!
        ).also { fn ->
            fn.parent = newFnClass.owner

            fn.copyTypeParametersFrom(this)
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            newDescriptor.valueParameters.forEach { p ->
                fn.addValueParameter(p.name.identifier, p.type.toIrType())
            }
            assert(fn.body == null) { "expected body to be null" }
        }
    }

    private fun IrFunction.copy(): IrFunction {
        // TODO(lmr): use deepCopy instead?
        val descriptor = descriptor

        val containerSource = (descriptor as? DescriptorWithContainerSource)?.containerSource
        val newDescriptor = if (containerSource != null)
                WrappedFunctionDescriptorWithContainerSource(containerSource)
            else
                WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)

        return IrFunctionImpl(
            startOffset,
            endOffset,
            origin,
            IrSimpleFunctionSymbolImpl(newDescriptor),
            name,
            visibility,
            descriptor.modality,
            returnType,
            isInline,
            isExternal,
            descriptor.isTailrec,
            descriptor.isSuspend
        ).also { fn ->
            newDescriptor.bind(fn)
            fn.parent = parent
            fn.copyTypeParametersFrom(this)
            fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
            fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
            valueParameters.mapTo(fn.valueParameters) { p -> p.copyTo(fn) }
            fn.body = body?.deepCopyWithSymbols(this)
        }
    }

    private fun IrFunction.copyWithComposerParam(): IrFunction {
        assert(explicitParameters.lastOrNull()?.name != KtxNameConventions.COMPOSER_PARAMETER) {
            "Attempted to add composer param to $this, but it has already been added."
        }
        return copy().also { fn ->
            val oldFn = this

            // NOTE: it's important to add these here before we recurse into the body in
            // order to avoid an infinite loop on circular/recursive calls
            transformedFunctionSet.add(fn)
            transformedFunctions[oldFn] = fn

            val valueParametersMapping = explicitParameters
                .zip(fn.explicitParameters)
                .toMap()

            val composerParam = fn.addValueParameter(
                KtxNameConventions.COMPOSER_PARAMETER.identifier,
                composerType
            )
            fn.transformChildrenVoid(object : IrElementTransformerVoid() {
                var isNestedScope = false
                override fun visitGetValue(expression: IrGetValue): IrGetValue {
                    val newParam = valueParametersMapping[expression.symbol.owner]
                    return if (newParam != null) {
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            newParam.symbol,
                            expression.origin
                        )
                    } else expression
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    if (expression.returnTargetSymbol == oldFn.symbol) {
                        // update the return statement to point to the new function, or else
                        // it will be interpreted as a non-local return
                        return super.visitReturn(IrReturnImpl(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            fn.symbol,
                            expression.value
                        ))
                    }
                    return super.visitReturn(expression)
                }

                override fun visitFunction(declaration: IrFunction): IrStatement {
                    val wasNested = isNestedScope
                    try {
                        // we don't want to pass the composer parameter in to composable calls
                        // inside of nested scopes.... *unless* the scope was inlined.
                        isNestedScope = if (declaration.isInlinedLambda()) wasNested else true
                        return super.visitFunction(declaration)
                    } finally {
                        isNestedScope = wasNested
                    }
                }

                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                    if (declaration.parent == oldFn) {
                        declaration.parent = fn
                    }
                    return super.visitDeclaration(declaration)
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val expr = if (!isNestedScope)
                        expression.withComposerParamIfNeeded(composerParam)
                    else
                        expression
                    return super.visitCall(expr)
                }
            })
        }
    }

    fun IrCall.isComposableLambdaInvoke(): Boolean {
        return origin == IrStatementOrigin.INVOKE && dispatchReceiver?.type?.isComposable() == true
    }

    fun IrFunction.isInlinedLambda(): Boolean {
        descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.state.bindingContext,
                        false
                    )
                )
                    return true
            }
        }
        return false
    }

    fun FunctionDescriptor.isComposable(): Boolean {
        val composability = composableChecker.analyze(context.state.bindingTrace, this)
        return when (composability) {
            ComposableAnnotationChecker.Composability.NOT_COMPOSABLE -> false
            ComposableAnnotationChecker.Composability.MARKED -> true
            ComposableAnnotationChecker.Composability.INFERRED -> true
        }
    }

    fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }
}

class ComposerTypeRemapper(
    private val context: JvmBackendContext,
    private val typeTranslator: TypeTranslator,
    private val composerTypeDescriptor: ClassDescriptor
) : TypeRemapper {
    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

    override fun leaveScope() {}

    private fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    override fun remapType(type: IrType): IrType {
        // TODO(lmr):
        // This is basically creating the KotlinType and then converting to an IrType. Consider
        // rewriting to just create the IrType directly, which would probably be more efficient.
        if (type !is IrSimpleType) return type
        if (!type.isFunction()) return type
        if (!type.isComposable()) return type
        val oldArguments = type.toKotlinType().arguments
        val newArguments =
            oldArguments.subList(0, oldArguments.size - 1) +
                    TypeProjectionImpl(composerTypeDescriptor.defaultType) +
                    oldArguments.last()

        return context
            .irBuiltIns
            .builtIns
            .getFunction(oldArguments.size) // return type is an argument, so this is n + 1
            .defaultType
            .replace(newArguments)
            .toIrType()
    }
}
