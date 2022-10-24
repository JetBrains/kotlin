/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal abstract class JvmValueClassAbstractLowering(val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {
    abstract val replacements: MemoizedValueClassAbstractReplacements

    final override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    abstract fun IrClass.isSpecificLoweringLogicApplicable(): Boolean

    abstract override fun visitClassNew(declaration: IrClass): IrStatement

    abstract fun handleSpecificNewClass(declaration: IrClass)

    protected fun transformFunctionFlat(function: IrFunction): List<IrDeclaration>? {
        if (function is IrConstructor && function.isPrimary && function.constructedClass.isSpecificLoweringLogicApplicable()) {
            return null
        }

        val replacement = replacements.getReplacementFunction(function)

        if (keepOldFunctionInsteadOfNew(function)) {
            function.transformChildrenVoid()
            addBindingsFor(function, replacement!!)
            return null
        }

        if (replacement == null) {
            if (function is IrConstructor) {
                val constructorReplacement = replacements.getReplacementForRegularClassConstructor(function)
                if (constructorReplacement != null) {
                    addBindingsFor(function, constructorReplacement)
                    return transformFlattenedConstructor(function, constructorReplacement)
                }
            }
            function.transformChildrenVoid()
            // Non-mangled functions can override mangled functions under some conditions, e.g., a function
            // `fun f(): Nothing` can override a function `fun f(): UInt`. The former is not mangled, while
            // the latter is.
            //
            // This is a potential problem for bridge generation, where we have to ensure that the overridden
            // symbols are always up to date. Right now they might not be since we lower each file independently
            // and since deserialized declarations are not mangled at all.
            if (function is IrSimpleFunction) {
                function.overriddenSymbols = replacements.replaceOverriddenSymbols(function)
            }
            return null
        }

        if (function is IrSimpleFunction && function.overriddenSymbols.any { it.owner.parentAsClass.isFun }) {
            // If fun interface methods are already mangled, do not mangle them twice.
            val suffix = function.hashSuffix()
            if (suffix != null && function.name.asString().endsWith(suffix)) {
                function.transformChildrenVoid()
                return null
            }
        }

        addBindingsFor(function, replacement)
        return when (function) {
            is IrSimpleFunction -> transformSimpleFunctionFlat(function, replacement)
            is IrConstructor -> transformSecondaryConstructorFlat(function, replacement)
            else -> throw IllegalStateException()
        }
    }

    private fun transformFlattenedConstructor(function: IrConstructor, replacement: IrConstructor): List<IrDeclaration>? {
        replacement.valueParameters.forEach {
            it.transformChildrenVoid()
            it.defaultValue?.patchDeclarationParents(replacement)
        }
        allScopes.push(createScope(function))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        return listOf(replacement)
    }

    private fun IrFunction.hashSuffix(): String? = InlineClassAbi.hashSuffix(
        this,
        context.state.functionsWithInlineClassReturnTypesMangled,
        context.state.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
    )

    protected abstract fun transformSecondaryConstructorFlat(constructor: IrConstructor, replacement: IrSimpleFunction): List<IrDeclaration>

    private fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration> {
        replacement.valueParameters.forEach {
            it.transformChildrenVoid()
            it.defaultValue?.patchDeclarationParents(replacement)
        }
        allScopes.push(createScope(replacement))
        replacement.body = function.body?.transform(this, null)?.patchDeclarationParents(replacement)
        allScopes.pop()
        replacement.copyAttributes(function)

        // Don't create a wrapper for functions which are only used in an unboxed context
        if (function.overriddenSymbols.isEmpty() || replacement.dispatchReceiverParameter != null)
            return listOf(replacement)

        val bridgeFunction = createBridgeFunction(function, replacement)

        return listOf(replacement, bridgeFunction)
    }

    abstract fun keepOldFunctionInsteadOfNew(function: IrFunction): Boolean

    final override fun visitReturn(expression: IrReturn): IrExpression {
        expression.returnTargetSymbol.owner.safeAs<IrFunction>()?.let { target ->
            val suffix = target.hashSuffix()
            if (suffix != null && target.name.asString().endsWith(suffix))
                return super.visitReturn(expression)

            replacements.run {
                if (keepOldFunctionInsteadOfNew(target)) return@run null
                getReplacementFunction(target) ?: if (target is IrConstructor) getReplacementForRegularClassConstructor(target) else null
            }?.let {
                return context.createIrBuilder(it.symbol, expression.startOffset, expression.endOffset).irReturn(
                    expression.value.transform(this, null)
                )
            }
        }
        return super.visitReturn(expression)
    }

    private fun visitStatementContainer(container: IrStatementContainer) {
        container.statements.transformFlat { statement ->
            if (statement is IrFunction)
                transformFunctionFlat(statement)
            else
                listOf(statement.transformStatement(this))
        }
    }

    final override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        visitStatementContainer(expression)
        return expression
    }

    final override fun visitBlockBody(body: IrBlockBody): IrBody {
        visitStatementContainer(body)
        return body
    }

    // Anonymous initializers in inline classes are processed when building the primary constructor.
    final override fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer): IrStatement =
        if (declaration.parent.safeAs<IrClass>()?.isSpecificLoweringLogicApplicable() == true)
            declaration
        else
            super.visitAnonymousInitializerNew(declaration)

    protected abstract fun addBindingsFor(original: IrFunction, replacement: IrFunction)

    protected enum class SpecificMangle { Inline, MultiField }

    protected abstract val specificMangle: SpecificMangle
    private fun createBridgeFunction(
        function: IrSimpleFunction,
        replacement: IrSimpleFunction
    ): IrSimpleFunction {
        val bridgeFunction = createBridgeDeclaration(
            function,
            replacement,
            when {
                function.isTypedEquals -> InlineClassAbi.mangledNameFor(function, mangleReturnTypes = false, useOldMangleRules = false)
                // If the original function has signature which need mangling we still need to replace it with a mangled version.
                (!function.isFakeOverride || function.findInterfaceImplementation(context.state.jvmDefaultMode) != null) && when (specificMangle) {
                    SpecificMangle.Inline -> function.signatureRequiresMangling(includeInline = true, includeMFVC = false)
                    SpecificMangle.MultiField -> function.signatureRequiresMangling(includeInline = false, includeMFVC = true)
                } -> replacement.name
                // Since we remove the corresponding property symbol from the bridge we need to resolve getter/setter
                // names at this point.
                replacement.isGetter ->
                    Name.identifier(JvmAbi.getterName(replacement.correspondingPropertySymbol!!.owner.name.asString()))

                replacement.isSetter ->
                    Name.identifier(JvmAbi.setterName(replacement.correspondingPropertySymbol!!.owner.name.asString()))

                else ->
                    function.name
            }
        )

        // Update the overridden symbols to point to their inline class replacements
        bridgeFunction.overriddenSymbols = replacement.overriddenSymbols

        // Replace the function body with a wrapper
        if (bridgeFunction.isFakeOverride && bridgeFunction.parentAsClass.isSpecificLoweringLogicApplicable()) {
            // Fake overrides redirect from the replacement to the original function, which is in turn replaced during interfacePhase.
            createBridgeBody(replacement, bridgeFunction, function, true)
        } else {
            createBridgeBody(bridgeFunction, replacement, function, false)
        }
        return bridgeFunction
    }

    private fun IrSimpleFunction.signatureRequiresMangling(includeInline: Boolean = true, includeMFVC: Boolean = true) =
        fullValueParameterList.any { it.type.getRequiresMangling(includeInline, includeMFVC) } ||
                context.state.functionsWithInlineClassReturnTypesMangled &&
                returnType.getRequiresMangling(includeInline = includeInline, includeMFVC = false)

    protected fun typedArgumentList(function: IrFunction, expression: IrMemberAccessExpression<*>) = listOfNotNull(
        function.dispatchReceiverParameter?.let { it to expression.dispatchReceiver },
        function.extensionReceiverParameter?.let { it to expression.extensionReceiver }
    ) + function.valueParameters.map { it to expression.getValueArgument(it.index) }


    // We may need to add a bridge method for inline class methods with static replacements. Ideally, we'd do this in BridgeLowering,
    // but unfortunately this is a special case in the old backend. The bridge method is not marked as such and does not follow the normal
    // visibility rules for bridge methods.
    abstract fun createBridgeDeclaration(source: IrSimpleFunction, replacement: IrSimpleFunction, mangledName: Name): IrSimpleFunction

    protected abstract fun createBridgeBody(source: IrSimpleFunction, target: IrSimpleFunction, original: IrFunction, inverted: Boolean)
}