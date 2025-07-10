/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.PermissivenessWithMigration
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenSymbolsSafe
import org.jetbrains.kotlin.fir.analysis.checkers.expression.isDataClassCopy
import org.jetbrains.kotlin.fir.analysis.checkers.inlineCheckerExtension
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineOnly
import org.jetbrains.kotlin.fir.analysis.checkers.relationWithMigration
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

object FirInlineDeclarationChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!declaration.isInline) {
            checkParametersInNotInline(declaration)
            return
        }
        if (context.session.inlineCheckerExtension?.isGenerallyOk(declaration) == false) return
        if (declaration !is FirPropertyAccessor && declaration !is FirSimpleFunction) return

        checkCallableDeclaration(declaration)
    }

    class InlineFunctionBodyContext(
        val inlineFunction: FirFunction,
        val inlineFunEffectiveVisibility: EffectiveVisibility,
        override val session: FirSession,
    ) : SessionHolder {
        private val isEffectivelyPrivateApiFunction: Boolean = inlineFunEffectiveVisibility.privateApi

        private fun accessedDeclarationEffectiveVisibility(
            accessExpression: FirStatement,
            accessedSymbol: FirBasedSymbol<*>,
        ): EffectiveVisibility {
            val recordedEffectiveVisibility = when (accessedSymbol) {
                is FirCallableSymbol<*> -> accessedSymbol.publishedApiEffectiveVisibility ?: accessedSymbol.effectiveVisibility
                is FirClassLikeSymbol<*> -> accessedSymbol.publishedApiEffectiveVisibility ?: accessedSymbol.effectiveVisibility
                else -> shouldNotBeCalled()
            }
            return when {
                recordedEffectiveVisibility.isReachableDueToLocalDispatchReceiver(accessExpression) -> EffectiveVisibility.Public
                recordedEffectiveVisibility == EffectiveVisibility.Local -> EffectiveVisibility.Public
                else -> recordedEffectiveVisibility
            }
        }

        private fun shouldReportNonPublicCallFromPublicInline(accessedDeclarationEffectiveVisibility: EffectiveVisibility): Boolean {
            return inlineFunEffectiveVisibility.publicApi &&
                    !accessedDeclarationEffectiveVisibility.publicApi &&
                    accessedDeclarationEffectiveVisibility !== EffectiveVisibility.Local
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        internal fun checkAccessedDeclaration(
            source: KtSourceElement,
            accessExpression: FirStatement,
            accessedSymbol: FirBasedSymbol<*>,
        ): AccessedDeclarationVisibilityData {
            val accessedVisibility = accessedDeclarationEffectiveVisibility(accessExpression, accessedSymbol)
            val accessedDataCopyVisibility = accessedSymbol.unwrapDataClassCopyWithPrimaryConstructorOrNull(session)
                ?.effectiveVisibility
            when {
                shouldReportNonPublicCallFromPublicInline(accessedVisibility) ->
                    reporter.reportOn(
                        source,
                        getNonPublicCallFromPublicInlineFactory(accessExpression, accessedSymbol, source),
                        accessedSymbol,
                        inlineFunction.symbol,
                    )
                accessedDataCopyVisibility != null &&
                        shouldReportNonPublicCallFromPublicInline(accessedDataCopyVisibility) ->
                    reporter.reportOn(
                        source,
                        FirErrors.NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE,
                        inlineFunction.symbol
                    )
                !isEffectivelyPrivateApiFunction && accessedSymbol.isInsidePrivateClass() ->
                    reporter.reportOn(
                        source,
                        FirErrors.PRIVATE_CLASS_MEMBER_FROM_INLINE,
                        accessedSymbol,
                        inlineFunction.symbol,
                    )
                // We don't need to check inside public inline functions because we already report
                // NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE, PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR and other diagnostics there.
                // In other cases, accessing less visible declarations in callable references is deprecated by KTLC-283.
                inlineFunEffectiveVisibility != EffectiveVisibility.Public &&
                        accessExpression is FirCallableReferenceAccess &&
                        isLessVisibleThanInlineFunction(accessedVisibility) ->
                    reporter.reportOn(
                        source,
                        FirErrors.CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE,
                        accessedSymbol,
                        accessedVisibility,
                        inlineFunEffectiveVisibility
                    )
            }
            return AccessedDeclarationVisibilityData(
                inlineFunEffectiveVisibility.publicApi,
                accessedVisibility.publicApi,
                accessedVisibility
            )
        }

        context(context: CheckerContext)
        private fun getNonPublicCallFromPublicInlineFactory(
            accessExpression: FirStatement,
            accessedSymbol: FirBasedSymbol<*>,
            source: KtSourceElement,
        ): KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> {
            if (!LanguageFeature.ProhibitPrivateOperatorCallInInline.isEnabled()) {
                val isDelegatedPropertyAccessor = source.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor
                val isForLoopButNotIteratorCall = source.kind == KtFakeSourceElementKind.DesugaredForLoop &&
                        accessExpression.toReference(session)?.symbol?.memberDeclarationNameOrNull != OperatorNameConventions.ITERATOR

                if (isDelegatedPropertyAccessor || isForLoopButNotIteratorCall) {
                    return FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION
                }
            }

            if (accessedSymbol is FirCallableSymbol && accessedSymbol.isInline) {
                return FirErrors.NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE
            }

            if (accessedSymbol is FirPropertySymbol) {
                if (context.callsOrAssignments.elementAtOrNull(context.callsOrAssignments.lastIndex - 1)
                        .let { it is FirVariableAssignment && it.lValue == accessExpression } &&
                    accessedSymbol.setterSymbol?.isInline == true
                ) {
                    return FirErrors.NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE
                } else if (accessedSymbol.getterSymbol?.isInline == true) {
                    return FirErrors.NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE
                }
            }

            return FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE
        }

        private fun EffectiveVisibility.isReachableDueToLocalDispatchReceiver(access: FirStatement): Boolean {
            val receiverType = access.localDispatchReceiver() ?: return false
            val receiverProtected = EffectiveVisibility.Protected(receiverType.typeConstructor(session.typeContext))
            val relation = receiverProtected.relation(this, session.typeContext)
            return relation == EffectiveVisibility.Permissiveness.SAME || relation == EffectiveVisibility.Permissiveness.LESS
        }

        private fun FirStatement.localDispatchReceiver(): ConeKotlinType? =
            (this as? FirQualifiedAccessExpression)?.dispatchReceiver?.resolvedType?.takeIf {
                it.toClassLikeSymbol(session)?.effectiveVisibility == EffectiveVisibility.Local
            }

        internal data class AccessedDeclarationVisibilityData(
            val isInlineFunPublicOrPublishedApi: Boolean,
            val isCalledFunPublicOrPublishedApi: Boolean,
            val calledFunEffectiveVisibility: EffectiveVisibility
        )

        context(context: CheckerContext, reporter: DiagnosticReporter)
        fun check(statement: FirStatement, targetSymbol: FirCallableSymbol<*>) {
            val source = statement.source ?: return
            checkVisibilityAndAccess(statement, targetSymbol, source)
            checkRecursion(targetSymbol, source)
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkVisibilityAndAccess(
            accessExpression: FirStatement,
            calledDeclaration: FirCallableSymbol<*>,
            source: KtSourceElement,
        ) {
            // Access of backing field (e.g. from getter) is not important, see inline/property/propertyWithBackingField.kt
            if (calledDeclaration.name == BACKING_FIELD) return

            val (isInlineFunPublicOrPublishedApi, isCalledFunPublicOrPublishedApi, calledFunEffectiveVisibility) = checkAccessedDeclaration(
                source,
                accessExpression,
                calledDeclaration,
            )

            if (isInlineFunPublicOrPublishedApi && isCalledFunPublicOrPublishedApi) {
                checkSuperCalls(calledDeclaration, accessExpression)
            }

            val isConstructorCall = calledDeclaration is FirConstructorSymbol
            if (
                isInlineFunPublicOrPublishedApi &&
                inlineFunEffectiveVisibility.toVisibility() !== Visibilities.Protected &&
                calledFunEffectiveVisibility.toVisibility() === Visibilities.Protected &&
                accessExpression !is FirDelegatedConstructorCall
            ) {
                val factory = when {
                    isConstructorCall -> FirErrors.PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE
                    else -> FirErrors.PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR
                }
                reporter.reportOn(source, factory, inlineFunction.symbol, calledDeclaration)
            }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkSuperCalls(
            calledDeclaration: FirCallableSymbol<*>,
            callExpression: FirStatement,
        ) {
            val receiver = when (callExpression) {
                is FirQualifiedAccessExpression -> callExpression.dispatchReceiver
                is FirVariableAssignment -> callExpression.dispatchReceiver
                else -> null
            } as? FirQualifiedAccessExpression ?: return

            if (receiver is FirSuperReceiverExpression) {
                val dispatchReceiverType = receiver.dispatchReceiver?.resolvedType
                val classSymbol = dispatchReceiverType?.toSymbol(session) ?: return
                if (!classSymbol.isDefinedInInlineFunction()) {
                    reporter.reportOn(
                        receiver.source,
                        FirErrors.SUPER_CALL_FROM_PUBLIC_INLINE,
                        calledDeclaration,
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

        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkRecursion(
            targetSymbol: FirBasedSymbol<*>,
            source: KtSourceElement,
        ) {
            if (targetSymbol == inlineFunction.symbol) {
                reporter.reportOn(source, FirErrors.RECURSION_IN_INLINE, targetSymbol)
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

        fun isLessVisibleThanInlineFunction(visibility: EffectiveVisibility): Boolean {
            if (visibility == EffectiveVisibility.Local && inlineFunEffectiveVisibility.privateApi) return false
            val relation = visibility.relationWithMigration(inlineFunEffectiveVisibility)
            return relation == PermissivenessWithMigration.LESS || relation == PermissivenessWithMigration.UNKNOWN || relation == PermissivenessWithMigration.UNKNOW_WITH_MIGRATION
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParameters(
        function: FirSimpleFunction,
        overriddenSymbols: List<FirCallableSymbol<FirCallableDeclaration>>,
    ) {
        for (param in function.valueParameters) {
            val coneType = param.returnTypeRef.coneType.fullyExpandedType()
            val functionKind = coneType.functionTypeKind(context.session)
            val isFunctionalType = functionKind != null
            val isSuspendFunctionType = functionKind?.isSuspendOrKSuspendFunction == true
            val defaultValue = param.defaultValue

            if (!isFunctionalType && (param.isNoinline || param.isCrossinline)) {
                reporter.reportOn(param.source, FirErrors.ILLEGAL_INLINE_PARAMETER_MODIFIER)
            }

            if (param.isNoinline) continue

            if (function.isSuspend && defaultValue != null && isSuspendFunctionType) {
                context.session.inlineCheckerExtension?.checkSuspendFunctionalParameterWithDefaultValue(param)
            }

            if (isSuspendFunctionType && !param.isCrossinline && !function.isSuspend) {
                reporter.reportOn(param.source, FirErrors.INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED)
            }

            if (coneType.isMarkedNullable && isFunctionalType) {
                reporter.reportOn(
                    param.source,
                    FirErrors.NULLABLE_INLINE_PARAMETER,
                    param.symbol,
                    function.symbol,
                )
            }

            if (isFunctionalType && defaultValue != null && !isInlinableDefaultValue(defaultValue)) {
                reporter.reportOn(
                    defaultValue.source,
                    FirErrors.INVALID_DEFAULT_FUNCTIONAL_PARAMETER_FOR_INLINE,
                    param.symbol,
                )
            }
        }

        if (overriddenSymbols.isNotEmpty()) {
            for (param in function.typeParameters) {
                if (param.isReified) {
                    reporter.reportOn(param.source, FirErrors.REIFIED_TYPE_PARAMETER_IN_OVERRIDE)
                }
            }
        }

        //check for inherited default values
        context.session.inlineCheckerExtension?.checkFunctionalParametersWithInheritedDefaultValues(
            function, overriddenSymbols
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParametersInNotInline(function: FirFunction) {
        for (param in function.valueParameters) {
            if (param.isNoinline || param.isCrossinline) {
                reporter.reportOn(param.source, FirErrors.ILLEGAL_INLINE_PARAMETER_MODIFIER)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkNothingToInline(function: FirSimpleFunction) {
        if (function.isExpect || function.isSuspend) return
        if (function.typeParameters.any { it.symbol.isReified }) return
        val session = context.session
        val hasInlinableParameters = function.valueParameters.any { it.isInlinable(context.session) }
        if (hasInlinableParameters) return
        if (function.isInlineOnly(session)) return
        if (function.returnTypeRef.needsMultiFieldValueClassFlattening(session)) return

        reporter.reportOn(function.source, FirErrors.NOTHING_TO_INLINE)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCanBeInlined(
        declaration: FirCallableDeclaration,
        effectiveVisibility: EffectiveVisibility,
    ): Boolean {
        if (declaration.containingClassLookupTag() == null) return true
        if (effectiveVisibility == EffectiveVisibility.PrivateInClass) return true

        if (!declaration.isEffectivelyFinal()) {
            // For primary constructor parameters there's INLINE_PROPERTY_WITH_BACKING_FIELD already
            if (declaration.source?.kind != KtFakeSourceElementKind.PropertyFromParameter) {
                reporter.reportOn(declaration.source, FirErrors.DECLARATION_CANT_BE_INLINED)
            }
            return false
        }
        return true
    }

    private fun isInlinableDefaultValue(expression: FirExpression): Boolean =
        expression is FirCallableReferenceAccess ||
                expression is FirFunctionCall ||
                expression is FirAnonymousFunctionExpression ||
                (expression is FirLiteralExpression && expression.value == null) //this will be reported separately

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkCallableDeclaration(declaration: FirCallableDeclaration) {
        if (declaration is FirPropertyAccessor) return
        val directOverriddenSymbols = declaration.symbol.directOverriddenSymbolsSafe()
        if (declaration is FirSimpleFunction) {
            checkParameters(declaration, directOverriddenSymbols)
            checkNothingToInline(declaration)
        }
        val canBeInlined = checkCanBeInlined(declaration, declaration.effectiveVisibility)

        if (canBeInlined && directOverriddenSymbols.isNotEmpty()) {
            reporter.reportOn(declaration.source, FirErrors.OVERRIDE_BY_INLINE)
        }
    }
}

internal fun FirValueParameter.isInlinable(session: FirSession): Boolean {
    if (isNoinline) return false
    val fullyExpandedType = returnTypeRef.coneType.fullyExpandedType(session)
    return !fullyExpandedType.isMarkedNullable && fullyExpandedType.functionTypeKind(session)?.isInlineable == true
}

fun createInlineFunctionBodyContext(function: FirFunction, session: FirSession): FirInlineDeclarationChecker.InlineFunctionBodyContext {
    return FirInlineDeclarationChecker.InlineFunctionBodyContext(
        function,
        function.publishedApiEffectiveVisibility ?: function.effectiveVisibility,
        session,
    )
}

fun FirBasedSymbol<*>.unwrapDataClassCopyWithPrimaryConstructorOrNull(session: FirSession): FirCallableSymbol<*>? =
    (this as? FirCallableSymbol<*>)?.containingClassLookupTag()?.toClassSymbol(session)
        ?.takeIf { containingClass -> isDataClassCopy(containingClass, session) }
        ?.primaryConstructorIfAny(session)
