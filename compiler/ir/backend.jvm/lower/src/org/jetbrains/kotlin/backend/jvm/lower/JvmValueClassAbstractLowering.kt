/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

internal abstract class JvmValueClassAbstractLowering(
    val context: JvmBackendContext,
    override val scopeStack: MutableList<ScopeWithIr>,
) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    abstract val replacements: MemoizedValueClassAbstractReplacements

    final override fun lower(irFile: IrFile) = withinScope(irFile) {
        irFile.transformChildrenVoid()
    }

    abstract fun IrClass.isSpecificLoweringLogicApplicable(): Boolean

    abstract fun handleSpecificNewClass(declaration: IrClass)

    protected fun transformFunctionFlat(function: IrFunction): List<IrDeclaration>? {
        if (function is IrConstructor && function.isPrimary && function.constructedClass.isSpecificLoweringLogicApplicable()) {
            return null
        }

        val replacement = replacements.getReplacementFunction(function)

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

    private fun transformFlattenedConstructor(function: IrConstructor, replacement: IrConstructor): List<IrDeclaration> {
        replacement.valueParameters.forEach {
            visitParameter(it)
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

    open fun visitParameter(parameter: IrValueParameter) {
        parameter.transformChildrenVoid()
    }

    final override fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
        visitParameter(declaration)
        return declaration
    }

    protected abstract fun transformSimpleFunctionFlat(function: IrSimpleFunction, replacement: IrSimpleFunction): List<IrDeclaration>

    final override fun visitReturn(expression: IrReturn): IrExpression {
        (expression.returnTargetSymbol.owner as? IrFunction)?.let { target ->
            val suffix = target.hashSuffix()
            if (suffix != null && target.name.asString().endsWith(suffix))
                return super.visitReturn(expression)

            replacements.run {
                getReplacementFunction(target) ?: if (target is IrConstructor) getReplacementForRegularClassConstructor(target) else null
            }?.let {
                return context.createIrBuilder(it.symbol, expression.startOffset, expression.endOffset).irReturn(
                    expression.value.transform(this, null)
                )
            }
        }
        return super.visitReturn(expression)
    }

    internal fun visitStatementContainer(container: IrStatementContainer) {
        container.statements.transformFlat { statement ->
            val newStatements =
                if (statement is IrFunction) withinScope(statement) { transformFunctionFlat(statement) }
                else listOf(statement.transformStatement(this))
            for (replacingDeclaration in (newStatements ?: listOf(statement)).filterIsInstance<IrDeclaration>()) {
                postActionAfterTransformingClassDeclaration(replacingDeclaration)
            }
            newStatements
        }
    }

    protected open fun postActionAfterTransformingClassDeclaration(replacingDeclaration: IrDeclaration) = Unit

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        visitStatementContainer(expression)
        return expression
    }

    final override fun visitBlockBody(body: IrBlockBody): IrBody {
        visitStatementContainer(body)
        return body
    }

    // Anonymous initializers in inline classes are processed when building the primary constructor.
    final override fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer): IrStatement =
        if ((declaration.parent as? IrClass)?.isSpecificLoweringLogicApplicable() == true)
            declaration
        else
            super.visitAnonymousInitializerNew(declaration)

    protected abstract fun addBindingsFor(original: IrFunction, replacement: IrFunction)

    protected enum class SpecificMangle { Inline, MultiField }

    protected abstract val specificMangle: SpecificMangle
    protected fun createBridgeFunction(
        function: IrSimpleFunction,
        replacement: IrSimpleFunction
    ): IrSimpleFunction {
        val bridgeFunction = createBridgeDeclaration(
            function,
            replacement,
            when {
                function.isValueClassTypedEquals -> InlineClassAbi.mangledNameFor(
                    function,
                    mangleReturnTypes = false,
                    useOldMangleRules = false
                )
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


    // Functions for common lowering dispatching
    private inner class NeedsToVisit : IrElementVisitor<Boolean, Nothing?> {
        override fun visitElement(element: IrElement, data: Nothing?): Boolean = false
        override fun visitClass(declaration: IrClass, data: Nothing?): Boolean =
            declaration.isSpecificLoweringLogicApplicable() || declaration.declarations.any { it.accept(this, null) }

        override fun visitFunction(declaration: IrFunction, data: Nothing?): Boolean =
            replacements.quickCheckIfFunctionIsNotApplicable(declaration)

        override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): Boolean =
            visitFunction(expression.symbol.owner, data)

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: Nothing?): Boolean =
            visitFunction(expression.symbol.owner, data)

        override fun visitField(declaration: IrField, data: Nothing?): Boolean = declaration.type.needsHandling
        override fun visitFieldAccess(expression: IrFieldAccessExpression, data: Nothing?): Boolean =
            visitField(expression.symbol.owner, data)

        override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean = visitValueDeclaration(declaration)

        private fun visitValueDeclaration(declaration: IrValueDeclaration) = declaration.type.needsHandling
        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): Boolean = visitValueDeclaration(declaration)
        override fun visitValueAccess(expression: IrValueAccessExpression, data: Nothing?): Boolean =
            visitValueDeclaration(expression.symbol.owner)

        override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean = false
        override fun visitReturn(expression: IrReturn, data: Nothing?): Boolean = (expression.returnTargetSymbol.owner as? IrFunction)
            ?.let { replacements.quickCheckIfFunctionIsNotApplicable(it) } ?: false

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): Boolean =
            (declaration.parent as? IrClass)?.isSpecificLoweringLogicApplicable() == true

        private fun visitStatementContainer(container: IrStatementContainer) = container.statements.any { it.accept(this, null) }

        override fun visitContainerExpression(expression: IrContainerExpression, data: Nothing?): Boolean =
            visitStatementContainer(expression)

        override fun visitBlockBody(body: IrBlockBody, data: Nothing?): Boolean = visitStatementContainer(body)
    }

    internal fun needsToVisitClassNew(declaration: IrClass): Boolean = declaration.accept(NeedsToVisit(), null)

    internal fun needsToVisitFunctionReference(expression: IrFunctionReference): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitFunctionAccess(expression: IrFunctionAccessExpression): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitCall(expression: IrCall): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitStringConcatenation(expression: IrStringConcatenation): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitGetField(expression: IrGetField): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitSetField(expression: IrSetField): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitGetValue(expression: IrGetValue): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitSetValue(expression: IrSetValue): Boolean = expression.accept(NeedsToVisit(), null)

    internal fun needsToVisitVariable(declaration: IrVariable): Boolean = declaration.accept(NeedsToVisit(), null)

    internal fun needsToVisitReturn(expression: IrReturn): Boolean = expression.accept(NeedsToVisit(), null)
    internal abstract fun visitClassNewDeclarationsWhenParallel(declaration: IrDeclaration)

    // forbid other overrides without modifying dispatcher file JvmValueClassLoweringDispatcher.kt

    final override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment = super.visitModuleFragment(declaration)
    final override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment = super.visitPackageFragment(declaration)
    final override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment =
        super.visitExternalPackageFragment(declaration)

    final override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement = super.visitDeclaration(declaration)
    final override fun visitSimpleFunction(declaration: IrSimpleFunction) = super.visitSimpleFunction(declaration)
    final override fun visitConstructor(declaration: IrConstructor) = super.visitConstructor(declaration)
    final override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) = super.visitLocalDelegatedProperty(declaration)
    final override fun visitEnumEntry(declaration: IrEnumEntry) = super.visitEnumEntry(declaration)
    final override fun visitTypeParameter(declaration: IrTypeParameter) = super.visitTypeParameter(declaration)
    final override fun visitTypeAlias(declaration: IrTypeAlias) = super.visitTypeAlias(declaration)
    final override fun visitBody(body: IrBody): IrBody = super.visitBody(body)
    final override fun visitExpressionBody(body: IrExpressionBody) = super.visitExpressionBody(body)
    final override fun visitSyntheticBody(body: IrSyntheticBody) = super.visitSyntheticBody(body)
    final override fun visitSuspendableExpression(expression: IrSuspendableExpression) = super.visitSuspendableExpression(expression)
    final override fun visitSuspensionPoint(expression: IrSuspensionPoint) = super.visitSuspensionPoint(expression)
    final override fun visitExpression(expression: IrExpression): IrExpression = super.visitExpression(expression)
    final override fun visitConst(expression: IrConst<*>) = super.visitConst(expression)
    final override fun visitConstantValue(expression: IrConstantValue): IrConstantValue = super.visitConstantValue(expression)
    final override fun visitConstantObject(expression: IrConstantObject) = super.visitConstantObject(expression)
    final override fun visitConstantPrimitive(expression: IrConstantPrimitive) = super.visitConstantPrimitive(expression)
    final override fun visitConstantArray(expression: IrConstantArray) = super.visitConstantArray(expression)
    final override fun visitVararg(expression: IrVararg) = super.visitVararg(expression)
    final override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement = super.visitSpreadElement(spread)
    final override fun visitBlock(expression: IrBlock) = super.visitBlock(expression)
    final override fun visitComposite(expression: IrComposite) = super.visitComposite(expression)
    final override fun visitDeclarationReference(expression: IrDeclarationReference) = super.visitDeclarationReference(expression)
    final override fun visitSingletonReference(expression: IrGetSingletonValue) = super.visitSingletonReference(expression)
    final override fun visitGetObjectValue(expression: IrGetObjectValue) = super.visitGetObjectValue(expression)
    final override fun visitGetEnumValue(expression: IrGetEnumValue) = super.visitGetEnumValue(expression)
    final override fun visitValueAccess(expression: IrValueAccessExpression) = super.visitValueAccess(expression)
    final override fun visitFieldAccess(expression: IrFieldAccessExpression) = super.visitFieldAccess(expression)
    final override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) = super.visitMemberAccess(expression)
    final override fun visitConstructorCall(expression: IrConstructorCall) = super.visitConstructorCall(expression)
    final override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) =
        super.visitDelegatingConstructorCall(expression)

    final override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) = super.visitEnumConstructorCall(expression)
    final override fun visitGetClass(expression: IrGetClass) = super.visitGetClass(expression)
    final override fun visitCallableReference(expression: IrCallableReference<*>) = super.visitCallableReference(expression)
    final override fun visitPropertyReference(expression: IrPropertyReference) = super.visitPropertyReference(expression)
    final override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) =
        super.visitLocalDelegatedPropertyReference(expression)

    final override fun visitRawFunctionReference(expression: IrRawFunctionReference) = super.visitRawFunctionReference(expression)
    final override fun visitFunctionExpression(expression: IrFunctionExpression) = super.visitFunctionExpression(expression)
    final override fun visitClassReference(expression: IrClassReference) = super.visitClassReference(expression)
    final override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) = super.visitInstanceInitializerCall(expression)
    final override fun visitTypeOperator(expression: IrTypeOperatorCall) = super.visitTypeOperator(expression)
    final override fun visitWhen(expression: IrWhen) = super.visitWhen(expression)
    final override fun visitBranch(branch: IrBranch): IrBranch = super.visitBranch(branch)
    final override fun visitElseBranch(branch: IrElseBranch): IrElseBranch = super.visitElseBranch(branch)
    final override fun visitLoop(loop: IrLoop) = super.visitLoop(loop)
    final override fun visitWhileLoop(loop: IrWhileLoop) = super.visitWhileLoop(loop)
    final override fun visitDoWhileLoop(loop: IrDoWhileLoop) = super.visitDoWhileLoop(loop)
    final override fun visitTry(aTry: IrTry) = super.visitTry(aTry)
    final override fun visitCatch(aCatch: IrCatch): IrCatch = super.visitCatch(aCatch)
    final override fun visitBreakContinue(jump: IrBreakContinue) = super.visitBreakContinue(jump)
    final override fun visitBreak(jump: IrBreak) = super.visitBreak(jump)
    final override fun visitContinue(jump: IrContinue) = super.visitContinue(jump)
    final override fun visitThrow(expression: IrThrow) = super.visitThrow(expression)
    final override fun visitDynamicExpression(expression: IrDynamicExpression) = super.visitDynamicExpression(expression)
    final override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression) =
        super.visitDynamicOperatorExpression(expression)

    final override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression) = super.visitDynamicMemberExpression(expression)
    final override fun visitErrorDeclaration(declaration: IrErrorDeclaration) = super.visitErrorDeclaration(declaration)
    final override fun visitErrorExpression(expression: IrErrorExpression) = super.visitErrorExpression(expression)
    final override fun visitErrorCallExpression(expression: IrErrorCallExpression) = super.visitErrorCallExpression(expression)


    abstract val IrType.needsHandling: Boolean
}
