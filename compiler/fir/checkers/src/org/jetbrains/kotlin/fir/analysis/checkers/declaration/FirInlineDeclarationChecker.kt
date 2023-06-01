/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContextForProvider
import org.jetbrains.kotlin.fir.analysis.checkers.inlineCheckerExtension
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineOnly
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

object FirInlineDeclarationChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInline) {
            checkParametersInNotInline(declaration, context, reporter)
            return
        }
        if (context.session.inlineCheckerExtension?.isGenerallyOk(declaration, context, reporter) == false) return
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
            !type.isMarkedNullable && type.isNonReflectFunctionType(context.session)
        }.map { it.symbol }

        val createVisitor = context.session.inlineCheckerExtension?.inlineVisitor ?: ::BasicInlineVisitor
        val visitor = createVisitor(
            function,
            effectiveVisibility,
            inalienableParameters,
            context.session,
            reporter
        )
        body.checkChildrenWithCustomVisitor(context, visitor, function)
    }

    open class BasicInlineVisitor(
        val inlineFunction: FirFunction,
        private val inlineFunEffectiveVisibility: EffectiveVisibility,
        private val inalienableParameters: List<FirValueParameterSymbol>,
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

        // prevent delegation to visitQualifiedAccessExpression, which causes redundant diagnostics
        override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: CheckerContext) {}

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CheckerContext) {
            val propertySymbol = variableAssignment.calleeReference?.toResolvedCallableSymbol() as? FirPropertySymbol ?: return
            val setterSymbol = propertySymbol.setterSymbol ?: return
            checkQualifiedAccess(variableAssignment, setterSymbol, data)
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: CheckerContext) {
            val accessedClass = resolvedQualifier.symbol ?: return
            val source = resolvedQualifier.source ?: return
            if (accessedClass.isCompanion) {
                checkAccessedDeclaration(source, accessedClass, accessedClass.visibility, data)
            }
        }

        private fun checkAccessedDeclaration(
            source: KtSourceElement,
            accessedSymbol: FirBasedSymbol<*>,
            declarationVisibility: Visibility,
            context: CheckerContext
        ): AccessedDeclarationVisibilityData {
            val recordedEffectiveVisibility = when (accessedSymbol) {
                is FirCallableSymbol<*> -> accessedSymbol.publishedApiEffectiveVisibility ?: accessedSymbol.effectiveVisibility
                is FirClassLikeSymbol<*> -> accessedSymbol.publishedApiEffectiveVisibility ?: accessedSymbol.effectiveVisibility
                else -> shouldNotBeCalled()
            }

            val accessedDeclarationEffectiveVisibility = recordedEffectiveVisibility.let {
                if (it == EffectiveVisibility.Local) {
                    EffectiveVisibility.Public
                } else {
                    it
                }
            }
            val isCalledFunPublicOrPublishedApi = accessedDeclarationEffectiveVisibility.publicApi
            val isInlineFunPublicOrPublishedApi = inlineFunEffectiveVisibility.publicApi
            if (isInlineFunPublicOrPublishedApi &&
                !isCalledFunPublicOrPublishedApi &&
                declarationVisibility !== Visibilities.Local
            ) {
                reporter.reportOn(
                    source,
                    FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE,
                    accessedSymbol,
                    inlineFunction.symbol,
                    context
                )
            } else {
                checkPrivateClassMemberAccess(accessedSymbol, source, context)
            }
            return AccessedDeclarationVisibilityData(
                isInlineFunPublicOrPublishedApi,
                isCalledFunPublicOrPublishedApi,
                accessedDeclarationEffectiveVisibility
            )
        }

        private data class AccessedDeclarationVisibilityData(
            val isInlineFunPublicOrPublishedApi: Boolean,
            val isCalledFunPublicOrPublishedApi: Boolean,
            val calledFunEffectiveVisibility: EffectiveVisibility
        )

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
            if (context.isContractBody) return
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
                        receiverExpression.source ?: qualifiedAccessExpression.source,
                        FirErrors.USAGE_IS_NOT_INLINABLE,
                        receiverSymbol,
                        context
                    )
                }
            }
        }

        private fun isInvokeOrInlineExtension(targetSymbol: FirBasedSymbol<*>?): Boolean {
            if (targetSymbol !is FirNamedFunctionSymbol) return false
            // TODO: receivers are currently not inline (KT-5837)
            // if (targetSymbol.isInline) return true
            return targetSymbol.name == OperatorNameConventions.INVOKE &&
                    targetSymbol.dispatchReceiverType?.isSomeFunctionType(session) == true
        }

        private fun checkQualifiedAccess(
            qualifiedAccess: FirStatement,
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

        private fun FirStatement.partOfCall(context: CheckerContext): Boolean {
            if (this !is FirExpression) return false
            val containingQualifiedAccess = context.qualifiedAccessOrAssignmentsOrAnnotationCalls.getOrNull(
                context.qualifiedAccessOrAssignmentsOrAnnotationCalls.size - 2
            ) ?: return false
            if (this == (containingQualifiedAccess as? FirQualifiedAccessExpression)?.explicitReceiver) return true
            val call = containingQualifiedAccess as? FirCall ?: return false
            return call.arguments.any { it.unwrapArgument() == this }
        }

        private fun checkVisibilityAndAccess(
            accessExpression: FirStatement,
            calledDeclaration: FirCallableSymbol<*>?,
            source: KtSourceElement,
            context: CheckerContext
        ) {
            if (
                calledDeclaration == null ||
                calledDeclaration.callableId.callableName == BACKING_FIELD
            ) {
                return
            }
            val (isInlineFunPublicOrPublishedApi, isCalledFunPublicOrPublishedApi, calledFunEffectiveVisibility) = checkAccessedDeclaration(
                source,
                calledDeclaration,
                calledDeclaration.visibility,
                context
            )

            if (isInlineFunPublicOrPublishedApi && isCalledFunPublicOrPublishedApi) {
                checkSuperCalls(calledDeclaration, accessExpression, context)
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
            calledDeclaration: FirBasedSymbol<*>,
            source: KtSourceElement,
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
            callExpression: FirStatement,
            context: CheckerContext
        ) {
            val receiver = when (callExpression) {
                is FirQualifiedAccessExpression -> callExpression.dispatchReceiver
                is FirVariableAssignment -> callExpression.dispatchReceiver
                else -> null
            } as? FirQualifiedAccessExpression ?: return

            if (receiver.calleeReference is FirSuperReference) {
                val dispatchReceiverType = receiver.dispatchReceiver.typeRef.coneType
                val classSymbol = dispatchReceiverType.toSymbol(session) ?: return
                if (!classSymbol.isDefinedInInlineFunction()) {
                    reporter.reportOn(
                        receiver.source,
                        FirErrors.SUPER_CALL_FROM_PUBLIC_INLINE,
                        calledDeclaration,
                        context
                    )
                }
            }
        }

        private fun FirClassifierSymbol<*>.isDefinedInInlineFunction(): Boolean {
            return when (val symbol = this) {
                is FirAnonymousObjectSymbol -> true
                is FirRegularClassSymbol -> symbol.classId.isLocal
                is FirTypeAliasSymbol, is FirTypeParameterSymbol -> error("Unexpected classifier declaration type: $symbol")
            }
        }

        private fun checkRecursion(
            targetSymbol: FirBasedSymbol<*>,
            source: KtSourceElement,
            context: CheckerContext
        ) {
            if (targetSymbol == inlineFunction.symbol) {
                reporter.reportOn(source, FirErrors.RECURSION_IN_INLINE, targetSymbol, context)
            }
        }

        private fun FirBasedSymbol<*>.isInsidePrivateClass(): Boolean {
            val containingClassSymbol = this.getOwnerLookupTag()?.toSymbol(session) ?: return false

            val containingClassVisibility = when (containingClassSymbol) {
                is FirAnonymousObjectSymbol -> return false
                is FirRegularClassSymbol -> containingClassSymbol.visibility
                is FirTypeAliasSymbol -> containingClassSymbol.visibility
            }
            if (containingClassVisibility == Visibilities.Private || containingClassVisibility == Visibilities.PrivateToThis) {
                return true
            }
            // We should check containing class of declaration only if this declaration is a member, not a class
            if (this is FirCallableSymbol<*> && containingClassSymbol is FirRegularClassSymbol && containingClassSymbol.isCompanion) {
                return containingClassSymbol.isInsidePrivateClass()
            }
            return false
        }
    }

    private fun checkParameters(
        function: FirSimpleFunction,
        overriddenSymbols: List<FirCallableSymbol<out FirCallableDeclaration>>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (param in function.valueParameters) {
            val coneType = param.returnTypeRef.coneType
            val functionKind = coneType.functionTypeKind(context.session)
            val isFunctionalType = functionKind != null
            val isSuspendFunctionType = functionKind?.isSuspendOrKSuspendFunction == true
            val defaultValue = param.defaultValue

            if (!isFunctionalType && (param.isNoinline || param.isCrossinline)) {
                reporter.reportOn(param.source, FirErrors.ILLEGAL_INLINE_PARAMETER_MODIFIER, context)
            }

            if (param.isNoinline) continue

            if (function.isSuspend && defaultValue != null && isSuspendFunctionType) {
                context.session.inlineCheckerExtension?.checkSuspendFunctionalParameterWithDefaultValue(param, context, reporter)
            }

            if (isSuspendFunctionType && !param.isCrossinline && !function.isSuspend) {
                reporter.reportOn(param.source, FirErrors.INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED, context)
            }

            if (coneType.isNullable && isFunctionalType) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NULLABLE_INLINE_PARAMETER,
                    param.symbol,
                    function.symbol,
                    context
                )
            }

            if (isFunctionalType && defaultValue != null && !isInlinableDefaultValue(defaultValue)) {
                reporter.reportOn(
                    defaultValue.source,
                    FirErrors.INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE,
                    defaultValue,
                    param.symbol,
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
        context.session.inlineCheckerExtension?.checkFunctionalParametersWithInheritedDefaultValues(
            function, context, reporter, overriddenSymbols
        )
    }

    private fun checkParametersInNotInline(function: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        for (param in function.valueParameters) {
            if (param.isNoinline || param.isCrossinline) {
                reporter.reportOn(param.source, FirErrors.ILLEGAL_INLINE_PARAMETER_MODIFIER, context)
            }
        }
    }

    private fun FirCallableDeclaration.getOverriddenSymbols(context: CheckerContext): List<FirCallableSymbol<out FirCallableDeclaration>> {
        if (!this.isOverride) return emptyList()
        val classSymbol = this.containingClassLookupTag()?.toSymbol(context.session) as? FirClassSymbol<*> ?: return emptyList()
        val scope = classSymbol.unsubstitutedScope(context)
        //this call is needed because AbstractFirUseSiteMemberScope collect overrides in it only,
        //and not in processDirectOverriddenFunctionsWithBaseScope
        scope.processFunctionsByName(this.symbol.name) { }
        return scope.getDirectOverriddenMembers(this.symbol, true)
    }

    private fun checkNothingToInline(function: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (function.isExpect || function.isSuspend) return
        if (function.typeParameters.any { it.symbol.isReified }) return
        val session = context.session
        val hasInlinableParameters =
            function.valueParameters.any { param ->
                val type = param.returnTypeRef.coneType
                !param.isNoinline && !type.isNullable
                        && (type.isBasicFunctionType(session) || type.isSuspendOrKSuspendFunctionType(session))
            }
        if (hasInlinableParameters) return
        if (function.isInlineOnly(session)) return
        if (function.returnTypeRef.needsMultiFieldValueClassFlattening(session)) return

        reporter.reportOn(function.source, FirErrors.NOTHING_TO_INLINE, context)
    }

    private fun checkCanBeInlined(
        declaration: FirCallableDeclaration,
        effectiveVisibility: EffectiveVisibility,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        if (declaration.containingClassLookupTag() == null) return true
        if (effectiveVisibility == EffectiveVisibility.PrivateInClass) return true

        if (!declaration.isEffectivelyFinal(context)) {
            reporter.reportOn(declaration.source, FirErrors.DECLARATION_CANT_BE_INLINED, context)
            return false
        }
        return true
    }

    private fun isInlinableDefaultValue(expression: FirExpression): Boolean =
        expression is FirCallableReferenceAccess ||
                expression is FirFunctionCall ||
                expression is FirLambdaArgumentExpression ||
                expression is FirAnonymousFunctionExpression ||
                (expression is FirConstExpression<*> && expression.value == null) //this will be reported separately

    fun checkCallableDeclaration(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
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

    private fun FirElement.checkChildrenWithCustomVisitor(
        parentContext: CheckerContext,
        visitorVoid: FirVisitor<Unit, CheckerContext>,
        rootFunction: FirFunction,
    ) {
        // TODO: Get rid of this cast and the following context modification as it looks like a leaking abstraction (see KT-56460)
        require(parentContext is CheckerContextForProvider) {
            "This checked violates the contract for read-only checkers"
        }

        parentContext.withDeclaration(rootFunction) {
            val collectingVisitor = object : AbstractDiagnosticCollectorVisitor(it) {
                override fun checkElement(element: FirElement) {
                    element.accept(visitorVoid, context)
                }
            }
            this.accept(collectingVisitor, null)
        }
    }
}
