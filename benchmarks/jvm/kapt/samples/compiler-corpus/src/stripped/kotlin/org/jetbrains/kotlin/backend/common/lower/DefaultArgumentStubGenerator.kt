/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.canHaveDefaultValue
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import org.jetbrains.kotlin.backend.common.lower.canHaveDefaultValue as canHaveDefaultValueImpl

// TODO: fix expect/actual default parameters

/**
 * Generates synthetic stubs for functions with default parameter values.
 */
open class DefaultArgumentStubGenerator<TContext : CommonBackendContext>(
    val context: TContext,
    private val factory: DefaultArgumentFunctionFactory,
    private val skipInlineMethods: Boolean = true,
    private val skipExternalMethods: Boolean = false,
    private val forceSetOverrideSymbols: Boolean = true
) : DeclarationTransformer {
    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }

    protected open fun IrFunction.resolveAnnotations(): List<IrAnnotation> = TODO()

    protected open fun IrFunction.generateDefaultStubBody(originalDeclaration: IrFunction): IrBody { TODO() }

    private fun lower(irFunction: IrFunction): List<IrFunction>? { TODO() }

    /**
     * Prepares the default value to be used inside the `function` body by patching the parents.
     * In K/JS it also copies the expression in order to avoid duplicate declarations after this lowering.
     *
     * In K/JVM copying doesn't preserve metadata, so the following case won't work:
     *
     * ```
     *   import kotlin.reflect.jvm.reflect
     *
     *   fun foo(x: Function<*> = {}) {
     *       // Will print "null" if lambda is copied
     *       println(x.reflect())
     *   }
     * ```
     *
     * Thus the duplicate declarations during the lowering pipeline is considered to be a lesser evil.
     */
    protected open fun IrExpression.prepareToBeUsedIn(function: IrFunction): IrExpression { TODO() }


    protected open fun IrBlockBodyBuilder.selectArgumentOrDefault(
        defaultFlag: IrExpression,
        parameter: IrValueParameter,
        default: IrExpression
    ): IrValueDeclaration { TODO() }

    protected open fun getOriginForCallToImplementation(): IrStatementOrigin? = TODO()

    private fun IrBlockBodyBuilder.dispatchToImplementation(
        irFunction: IrSimpleFunction,
        newIrFunction: IrFunction,
        variablesForDefaultParameters: List<IrValueDeclaration?>
    ): IrExpression = TODO()

    protected open fun IrBlockBodyBuilder.generateSuperCallHandlerCheckIfNeeded(
        irFunction: IrFunction,
        newIrFunction: IrFunction
    ) { TODO() }

    protected open fun defaultArgumentStubVisibility(function: IrFunction) =
        function.visibility // Just use whatever visibility the original declaration has

    protected open fun useConstructorMarker(function: IrFunction) = function is IrConstructor

    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

/**
 * Replaces call site with default parameters with the corresponding stub function.
 */
open class DefaultParameterInjector<TContext : CommonBackendContext>(
    protected val context: TContext,
    protected val factory: DefaultArgumentFunctionFactory,
    protected val skipInline: Boolean = true,
    protected val skipExternalMethods: Boolean = false,
    protected val forceSetOverrideSymbols: Boolean = true,
) : IrElementTransformerVoid(), BodyLoweringPass {

    private val declarationStack = mutableListOf<IrDeclaration>()
    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    protected open fun shouldReplaceWithSyntheticFunction(functionAccess: IrFunctionAccessExpression): Boolean = TODO()

    private fun <T : IrFunctionAccessExpression> visitFunctionAccessExpression(expression: T, builder: (IrFunctionSymbol) -> T): IrExpression { TODO() }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression { TODO() }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression { TODO() }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression { TODO() }

    override fun visitCall(expression: IrCall): IrExpression { TODO() }

    private fun createStubFunction(expression: IrFunctionAccessExpression): IrFunctionSymbol? { TODO() }

    protected open fun IrBlockBuilder.argumentsForCall(expression: IrFunctionAccessExpression, stubFunction: IrFunction): Map<IrValueParameter, IrExpression?> { TODO() }

    protected open fun nullConst(startOffset: Int, endOffset: Int, irParameter: IrValueParameter): IrExpression? = TODO()

    protected open fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression = TODO()

    protected open fun defaultArgumentStubVisibility(function: IrFunction) =
        function.visibility // Just use whatever visibility the original declaration has

    protected open fun useConstructorMarker(function: IrFunction) = function is IrConstructor

    protected open fun isStatic(function: IrFunction): Boolean = TODO()

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }

    protected fun IrValueParameter.canHaveDefaultValue() = canHaveDefaultValueImpl()
}

/**
 * Removes default argument initializers.
 */
open class DefaultParameterCleaner(
    val context: CommonBackendContext,
    val replaceDefaultValuesWithStubs: Boolean = false
) : DeclarationTransformer {
    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }
}

/**
 * Patch overrides for fake override dispatch functions. Should be used in case `forceSetOverrideSymbols = false`.
 */
open class DefaultParameterPatchOverridenSymbolsLowering(
    val context: CommonBackendContext
) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? { TODO() }
}

open class MaskedDefaultArgumentFunctionFactory(context: CommonBackendContext, copyOriginalFunctionLocation: Boolean = true) : DefaultArgumentFunctionFactory(context, copyOriginalFunctionLocation) {
    final override fun IrFunction.generateDefaultArgumentStubFrom(original: IrFunction, useConstructorMarker: Boolean) { TODO() }
}

private fun IrValueParameter.canHaveDefaultValue() =
    kind != IrParameterKind.DispatchReceiver && kind != IrParameterKind.ExtensionReceiver &&
    origin != IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER && origin != IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
