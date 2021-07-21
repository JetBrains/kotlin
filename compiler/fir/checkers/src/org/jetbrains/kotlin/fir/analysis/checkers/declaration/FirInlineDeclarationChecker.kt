/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineOnly
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.checkers.util.checkChildrenWithCustomVisitor
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirInlineDeclarationChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInline) return
        // local inline functions are prohibited
        if (declaration.isLocalMember) {
            reporter.reportOn(declaration.source, FirErrors.NOT_YET_SUPPORTED_IN_INLINE, "Local inline functions", context)
            return
        }
        if (declaration !is FirPropertyAccessor && declaration !is FirSimpleFunction) return

        val effectiveVisibility = declaration.effectiveVisibility
        checkInlineFunctionBody(declaration, effectiveVisibility, context, reporter)
        checkCallableDeclaration(declaration, context, reporter)
    }

    private fun checkInlineFunctionBody(
        function: FirFunction,
        effectiveVisibility: EffectiveVisibility,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val body = function.body ?: return
        val inalienableParameters = function.valueParameters.filter {
            if (it.isNoinline) return@filter false
            val type = it.returnTypeRef.coneType
            !type.isMarkedNullable && type.isFunctionalType(context.session) { kind -> !kind.isReflectType }
        }.map { it.symbol }

        val visitor = Visitor(
            function,
            effectiveVisibility,
            inalienableParameters,
            context.session,
            reporter
        )
        body.checkChildrenWithCustomVisitor((context as PersistentCheckerContext).addDeclaration(function), visitor)
    }

    private class Visitor(
        val inlineFunction: FirFunction,
        val inlineFunEffectiveVisibility: EffectiveVisibility,
        val inalienableParameters: List<FirValueParameterSymbol>,
        val session: FirSession,
        val reporter: DiagnosticReporter
    ) : FirDefaultVisitor<Unit, CheckerContext>() {
        private val isEffectivelyPrivateApiFunction: Boolean = inlineFunEffectiveVisibility.privateApi

        private val prohibitProtectedCallFromInline: Boolean =
            session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitProtectedCallFromInline)

        override fun visitElement(element: FirElement, data: CheckerContext) {}

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: CheckerContext) {
            val targetSymbol = functionCall.toResolvedCallableSymbol()
            if (targetSymbol != null) {
                checkReceiversOfQualifiedAccessExpression(functionCall, targetSymbol, data)
                checkArgumentsOfCall(functionCall, targetSymbol, data)
                checkQualifiedAccess(functionCall, targetSymbol, data)
            }
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: CheckerContext) {
            val targetSymbol = qualifiedAccessExpression.toResolvedCallableSymbol()
            checkQualifiedAccess(qualifiedAccessExpression, targetSymbol, data)
            checkReceiversOfQualifiedAccessExpression(qualifiedAccessExpression, targetSymbol, data)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
            val propertySymbol = variableAssignment.calleeReference.toResolvedCallableSymbol() as? FirPropertySymbol ?: return
            val setterSymbol = propertySymbol.setterSymbol ?: return
            checkQualifiedAccess(variableAssignment, setterSymbol, data)
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
            if (!regularClass.classKind.isSingleton && data.containingDeclarations.lastOrNull() === inlineFunction) {
                reporter.reportOn(regularClass.source, FirErrors.NOT_YET_SUPPORTED_IN_INLINE, "Local classes", data)
            } else {
                super.visitRegularClass(regularClass, data)
            }
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
            if (data.containingDeclarations.lastOrNull() === inlineFunction) {
                reporter.reportOn(simpleFunction.source, FirErrors.NOT_YET_SUPPORTED_IN_INLINE, "Local functions", data)
            } else {
                super.visitSimpleFunction(simpleFunction, data)
            }
        }

        private fun checkReceiversOfQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext
        ) {
            checkReceiver(qualifiedAccessExpression, qualifiedAccessExpression.dispatchReceiver, targetSymbol, context)
            checkReceiver(qualifiedAccessExpression, qualifiedAccessExpression.extensionReceiver, targetSymbol, context)
        }

        private fun checkArgumentsOfCall(
            functionCall: FirFunctionCall,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext
        ) {
            val calledFunctionSymbol = targetSymbol as? FirNamedFunctionSymbol ?: return
            val argumentMapping = functionCall.resolvedArgumentMapping ?: return
            for ((wrappedArgument, valueParameter) in argumentMapping) {
                val argument = wrappedArgument.unwrapArgument()
                val resolvedArgumentSymbol = argument.toResolvedCallableSymbol() as? FirVariableSymbol<*> ?: continue

                val valueParameterOfOriginalInlineFunction = inalienableParameters.firstOrNull { it == resolvedArgumentSymbol }
                if (valueParameterOfOriginalInlineFunction != null) {
                    val factory = when {
                        calledFunctionSymbol.isInline -> when {
                            valueParameter.isNoinline -> FirErrors.USAGE_IS_NOT_INLINABLE
                            valueParameter.isCrossinline && !valueParameterOfOriginalInlineFunction.isCrossinline
                            -> FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED
                            else -> continue
                        }
                        else -> FirErrors.USAGE_IS_NOT_INLINABLE
                    }
                    reporter.reportOn(argument.source, factory, valueParameterOfOriginalInlineFunction, context)
                }
            }
        }

        private fun checkReceiver(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            receiverExpression: FirExpression,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext
        ) {
            val receiverSymbol = receiverExpression.toResolvedCallableSymbol() ?: return
            if (receiverSymbol in inalienableParameters) {
                if (!isInvokeOrInlineExtension(targetSymbol)) {
                    reporter.reportOn(
                        qualifiedAccessExpression.source,
                        FirErrors.USAGE_IS_NOT_INLINABLE,
                        receiverSymbol,
                        context
                    )
                }
            }
        }

        private fun isInvokeOrInlineExtension(targetSymbol: FirBasedSymbol<*>?): Boolean {
            if (targetSymbol !is FirNamedFunctionSymbol) return false
            if (targetSymbol.isInline) return true
            return targetSymbol.name == OperatorNameConventions.INVOKE &&
                    targetSymbol.dispatchReceiverType?.isBuiltinFunctionalType(session) == true
        }

        private fun checkQualifiedAccess(
            qualifiedAccess: FirQualifiedAccess,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext
        ) {
            val source = qualifiedAccess.source ?: return
            if (targetSymbol !is FirCallableSymbol<*>) return

            if (targetSymbol in inalienableParameters) {
                if (!qualifiedAccess.partOfCall(context)) {
                    reporter.reportOn(source, FirErrors.USAGE_IS_NOT_INLINABLE, targetSymbol, context)
                }
            }
            checkVisibilityAndAccess(qualifiedAccess, targetSymbol, source, context)
            checkRecursion(targetSymbol, source, context)
        }

        private fun FirQualifiedAccess.partOfCall(context: CheckerContext): Boolean {
            if (this !is FirExpression) return false
            val containingQualifiedAccess = context.qualifiedAccessOrAnnotationCalls.getOrNull(
                context.qualifiedAccessOrAnnotationCalls.size - 2
            ) ?: return false
            if (this == (containingQualifiedAccess as? FirQualifiedAccess)?.explicitReceiver) return true
            val call = containingQualifiedAccess as? FirCall ?: return false
            return call.arguments.any { it.unwrapArgument() == this }
        }

        private fun checkVisibilityAndAccess(
            accessExpression: FirQualifiedAccess,
            calledDeclaration: FirCallableSymbol<*>?,
            source: FirSourceElement,
            context: CheckerContext
        ) {
            if (calledDeclaration == null) return
            val recordedEffectiveVisibility = calledDeclaration.publishedApiEffectiveVisibility ?: calledDeclaration.effectiveVisibility
            val calledFunEffectiveVisibility = recordedEffectiveVisibility.let {
                if (it == EffectiveVisibility.Local) {
                    EffectiveVisibility.Public
                } else {
                    it
                }
            }
            val isCalledFunPublicOrPublishedApi = calledFunEffectiveVisibility.publicApi
            val isInlineFunPublicOrPublishedApi = inlineFunEffectiveVisibility.publicApi
            if (isInlineFunPublicOrPublishedApi &&
                !isCalledFunPublicOrPublishedApi &&
                calledDeclaration.visibility !== Visibilities.Local
            ) {
                reporter.reportOn(
                    source,
                    FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE,
                    calledDeclaration,
                    inlineFunction.symbol,
                    context
                )
            } else {
                checkPrivateClassMemberAccess(calledDeclaration, source, context)
                if (isInlineFunPublicOrPublishedApi) {
                    checkSuperCalls(calledDeclaration, accessExpression, context)
                }
            }

            val isConstructorCall = calledDeclaration is FirConstructorSymbol
            if (
                isInlineFunPublicOrPublishedApi &&
                inlineFunEffectiveVisibility.toVisibility() !== Visibilities.Protected &&
                calledFunEffectiveVisibility.toVisibility() === Visibilities.Protected
            ) {
                val factory = when {
                    isConstructorCall -> FirErrors.PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE
                    prohibitProtectedCallFromInline -> FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR
                    else -> FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE
                }
                reporter.reportOn(source, factory, calledDeclaration, inlineFunction.symbol, context)
            }
        }

        private fun checkPrivateClassMemberAccess(
            calledDeclaration: FirCallableSymbol<*>,
            source: FirSourceElement,
            context: CheckerContext
        ) {
            if (!isEffectivelyPrivateApiFunction) {
                if (calledDeclaration.isInsidePrivateClass()) {
                    reporter.reportOn(
                        source,
                        FirErrors.PRIVATE_CLASS_MEMBER_FROM_INLINE,
                        calledDeclaration,
                        inlineFunction.symbol,
                        context
                    )
                }
            }
        }

        private fun checkSuperCalls(
            calledDeclaration: FirCallableSymbol<*>,
            callExpression: FirQualifiedAccess,
            context: CheckerContext
        ) {
            val receiver = callExpression.dispatchReceiver as? FirQualifiedAccessExpression ?: return
            if (receiver.calleeReference is FirSuperReference) {
                val dispatchReceiverType = receiver.dispatchReceiver.typeRef.coneType
                val classSymbol = dispatchReceiverType.toSymbol(session) ?: return
                if (!classSymbol.isDefinedInInlineFunction()) {
                    reporter.reportOn(
                        callExpression.dispatchReceiver.source,
                        FirErrors.SUPER_CALL_FROM_PUBLIC_INLINE,
                        calledDeclaration,
                        context
                    )
                }
            }
        }

        private fun FirBasedSymbol<*>.isDefinedInInlineFunction(): Boolean {
            return when (val symbol = this) {
                is FirAnonymousFunctionSymbol -> true
                is FirCallableSymbol<*> -> symbol.isLocalMember
                is FirAnonymousObjectSymbol -> true
                is FirRegularClassSymbol -> symbol.classId.isLocal
                else -> error("Unknown callable declaration type: $symbol")
            }
        }

        private fun checkRecursion(
            targetSymbol: FirBasedSymbol<*>,
            source: FirSourceElement,
            context: CheckerContext
        ) {
            if (targetSymbol == inlineFunction.symbol) {
                reporter.reportOn(source, FirErrors.RECURSION_IN_INLINE, targetSymbol, context)
            }
        }

        private fun FirCallableSymbol<*>.isInsidePrivateClass(): Boolean {
            val containingClassSymbol = this.containingClass()?.toSymbol(session) ?: return false

            val containingClassVisibility = when (containingClassSymbol) {
                is FirAnonymousObjectSymbol -> return false
                is FirRegularClassSymbol -> containingClassSymbol.visibility
                is FirTypeAliasSymbol -> containingClassSymbol.visibility
            }
            return containingClassVisibility == Visibilities.Private || containingClassVisibility == Visibilities.PrivateToThis
        }
    }

    private fun checkParameters(
        function: FirSimpleFunction,
        overriddenSymbols: List<FirCallableSymbol<out FirCallableDeclaration>>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (param in function.valueParameters) {
            if (param.isNoinline) continue

            val coneType = param.returnTypeRef.coneType
            if (function.isSuspend && param.defaultValue != null && coneType.isSuspendFunctionType(context.session)) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
                    "Suspend functional parameters with default values",
                    context
                )
            }

            if (coneType.isNullable && coneType.isFunctionalType(context.session)) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NULLABLE_INLINE_PARAMETER,
                    param.symbol,
                    function.symbol,
                    context
                )
            }
        }

        if (overriddenSymbols.isNotEmpty()) {
            for (param in function.typeParameters) {
                if (param.isReified) {
                    reporter.reportOn(param.source, FirErrors.REIFIED_TYPE_PARAMETER_IN_OVERRIDE, context)
                }
            }
        }

        //check for inherited default values
        val paramsWithDefaults = overriddenSymbols.flatMap {
            if (it !is FirFunctionSymbol<*>) return@flatMap emptyList<Int>()
            it.valueParameterSymbols.mapIndexedNotNull { idx, param ->
                idx.takeIf { param.hasDefaultValue }
            }
        }.toSet()
        function.valueParameters.forEachIndexed { idx, param ->
            if (param.defaultValue == null && paramsWithDefaults.contains(idx)) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NOT_YET_SUPPORTED_IN_INLINE,
                    "Functional parameters with inherited default values",
                    context
                )
            }
        }
    }

    private fun FirCallableDeclaration.getOverriddenSymbols(context: CheckerContext): List<FirCallableSymbol<out FirCallableDeclaration>> {
        if (!this.isOverride) return emptyList()
        val classSymbol = this.containingClass()?.toSymbol(context.session) as? FirClassSymbol<*> ?: return emptyList()
        val scope = classSymbol.unsubstitutedScope(context)
        //this call is needed because AbstractFirUseSiteMemberScope collect overrides in it only,
        //and not in processDirectOverriddenFunctionsWithBaseScope
        scope.processFunctionsByName(this.symbol.name) { }
        return scope.getDirectOverriddenMembers(this.symbol, true)
    }

    private fun checkNothingToInline(function: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (function.isExpect || function.isSuspend) return
        if (function.typeParameters.any { it.symbol.isReified }) return
        val hasInlinableParameters =
            function.valueParameters.any { param ->
                val type = param.returnTypeRef.coneType
                !param.isNoinline && !type.isNullable
                        && (type.isFunctionalType(context.session) || type.isSuspendFunctionType(context.session))
            }
        if (hasInlinableParameters) return
        if (function.isInlineOnly()) return

        reporter.reportOn(function.source, FirErrors.NOTHING_TO_INLINE, context)
    }

    private fun checkCanBeInlined(
        declaration: FirCallableDeclaration,
        effectiveVisibility: EffectiveVisibility,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        if (declaration.containingClass() == null) return true
        if (effectiveVisibility == EffectiveVisibility.PrivateInClass) return true

        if (!declaration.isFinal) {
            reporter.reportOn(declaration.source, FirErrors.DECLARATION_CANT_BE_INLINED, context)
            return false
        }
        return true
    }

    internal fun checkCallableDeclaration(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) return
        val overriddenSymbols = declaration.getOverriddenSymbols(context)
        if (declaration is FirSimpleFunction) {
            checkParameters(declaration, overriddenSymbols, context, reporter)
            checkNothingToInline(declaration, context, reporter)
        }
        val canBeInlined = checkCanBeInlined(declaration, declaration.effectiveVisibility, context, reporter)

        if (canBeInlined && overriddenSymbols.isNotEmpty()) {
            reporter.reportOn(declaration.source, FirErrors.OVERRIDE_BY_INLINE, context)
        }
    }
}
