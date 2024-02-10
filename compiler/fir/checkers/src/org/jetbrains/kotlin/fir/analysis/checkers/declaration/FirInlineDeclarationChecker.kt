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
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getDirectOverriddenSymbols
import org.jetbrains.kotlin.fir.analysis.checkers.inlineCheckerExtension
import org.jetbrains.kotlin.fir.analysis.checkers.isInlineOnly
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

object FirInlineDeclarationChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInline) {
            checkParametersInNotInline(declaration, context, reporter)
            return
        }
        if (context.session.inlineCheckerExtension?.isGenerallyOk(declaration, context, reporter) == false) return
        if (declaration !is FirPropertyAccessor && declaration !is FirSimpleFunction) return

        checkCallableDeclaration(declaration, context, reporter)
    }

    class InlineFunctionBodyContext(
        val inlineFunction: FirFunction,
        private val inlineFunEffectiveVisibility: EffectiveVisibility,
        private val inlinableParameters: List<FirValueParameterSymbol>,
        val session: FirSession,
    ) : FirDefaultVisitor<Unit, CheckerContext>() {
        private val isEffectivelyPrivateApiFunction: Boolean = inlineFunEffectiveVisibility.privateApi

        private val prohibitProtectedCallFromInline: Boolean =
            session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitProtectedCallFromInline)

        override fun visitElement(element: FirElement, data: CheckerContext) {}

        // prevent delegation to visitQualifiedAccessExpression, which causes redundant diagnostics
        override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: CheckerContext) {}

        internal fun checkAccessedDeclaration(
            source: KtSourceElement,
            accessExpression: FirStatement,
            accessedSymbol: FirBasedSymbol<*>,
            declarationVisibility: Visibility,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ): AccessedDeclarationVisibilityData {
            val recordedEffectiveVisibility = when (accessedSymbol) {
                is FirCallableSymbol<*> -> accessedSymbol.publishedApiEffectiveVisibility ?: accessedSymbol.effectiveVisibility
                is FirClassLikeSymbol<*> -> accessedSymbol.publishedApiEffectiveVisibility ?: accessedSymbol.effectiveVisibility
                else -> shouldNotBeCalled()
            }

            val accessedDeclarationEffectiveVisibility = when {
                recordedEffectiveVisibility.isReachableDueToLocalDispatchReceiver(accessExpression, context) -> EffectiveVisibility.Public
                recordedEffectiveVisibility == EffectiveVisibility.Local -> EffectiveVisibility.Public
                else -> recordedEffectiveVisibility
            }
            val isCalledFunPublicOrPublishedApi = accessedDeclarationEffectiveVisibility.publicApi
            val isInlineFunPublicOrPublishedApi = inlineFunEffectiveVisibility.publicApi
            if (isInlineFunPublicOrPublishedApi &&
                !isCalledFunPublicOrPublishedApi &&
                declarationVisibility !== Visibilities.Local
            ) {
                reporter.reportOn(
                    source,
                    getNonPublicCallFromPublicInlineFactory(accessExpression, source, context),
                    inlineFunction.symbol,
                    accessedSymbol,
                    context
                )
            } else {
                checkPrivateClassMemberAccess(accessedSymbol, source, context, reporter)
            }
            return AccessedDeclarationVisibilityData(
                isInlineFunPublicOrPublishedApi,
                isCalledFunPublicOrPublishedApi,
                accessedDeclarationEffectiveVisibility
            )
        }

        private fun getNonPublicCallFromPublicInlineFactory(
            accessExpression: FirStatement,
            source: KtSourceElement,
            context: CheckerContext,
        ): KtDiagnosticFactory2<FirBasedSymbol<*>, FirBasedSymbol<*>> {
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitPrivateOperatorCallInInline)) {
                val isDelegatedPropertyAccessor = source.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor
                val isForLoopButNotIteratorCall = source.kind == KtFakeSourceElementKind.DesugaredForLoop &&
                        accessExpression.toReference(context.session)?.symbol?.memberDeclarationNameOrNull != OperatorNameConventions.ITERATOR

                if (isDelegatedPropertyAccessor || isForLoopButNotIteratorCall) {
                    return FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE_DEPRECATION
                }
            }

            return FirErrors.NON_PUBLIC_CALL_FROM_PUBLIC_INLINE
        }

        private fun EffectiveVisibility.isReachableDueToLocalDispatchReceiver(access: FirStatement, context: CheckerContext): Boolean {
            val receiverType = access.localDispatchReceiver(context) ?: return false
            val receiverProtected = EffectiveVisibility.Protected(receiverType.typeConstructor(context.session.typeContext))
            val relation = receiverProtected.relation(this, context.session.typeContext)
            return relation == EffectiveVisibility.Permissiveness.SAME || relation == EffectiveVisibility.Permissiveness.LESS
        }

        private fun FirStatement.localDispatchReceiver(context: CheckerContext): ConeKotlinType? =
            (this as? FirQualifiedAccessExpression)?.dispatchReceiver?.resolvedType?.takeIf {
                (it.toSymbol(context.session) as? FirClassLikeSymbol<*>)?.effectiveVisibility == EffectiveVisibility.Local
            }

        internal data class AccessedDeclarationVisibilityData(
            val isInlineFunPublicOrPublishedApi: Boolean,
            val isCalledFunPublicOrPublishedApi: Boolean,
            val calledFunEffectiveVisibility: EffectiveVisibility
        )

        internal fun checkReceiversOfQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            checkReceiver(qualifiedAccessExpression, qualifiedAccessExpression.dispatchReceiver, targetSymbol, context, reporter)
            checkReceiver(qualifiedAccessExpression, qualifiedAccessExpression.extensionReceiver, targetSymbol, context, reporter)
        }

        internal fun checkArgumentsOfCall(
            functionCall: FirFunctionCall,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            if (context.isContractBody) return
            val calledFunctionSymbol = targetSymbol as? FirFunctionSymbol ?: return
            val argumentMapping = functionCall.resolvedArgumentMapping ?: return
            for ((wrappedArgument, valueParameter) in argumentMapping) {
                val argument = wrappedArgument.unwrapErrorExpression()?.unwrapArgument() ?: continue
                val resolvedArgumentSymbol = argument.toResolvedCallableSymbol(session) as? FirVariableSymbol<*> ?: continue

                val valueParameterOfOriginalInlineFunction = inlinableParameters.firstOrNull { it == resolvedArgumentSymbol }
                if (valueParameterOfOriginalInlineFunction != null) {
                    val factory = when {
                        calledFunctionSymbol.isInline -> when {
                            valueParameter.isNoinline -> {
                                FirErrors.USAGE_IS_NOT_INLINABLE
                            }
                            !valueParameterOfOriginalInlineFunction.isCrossinline &&
                                    (valueParameter.isCrossinline || !isNonLocalReturnAllowed(context, inlineFunction)) -> {
                                FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED
                            }
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
            receiverExpression: FirExpression?,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            if (receiverExpression == null) return
            val receiverSymbol =
                receiverExpression.unwrapErrorExpression()?.toResolvedCallableSymbol(session) as? FirValueParameterSymbol ?: return
            if (receiverSymbol in inlinableParameters) {
                if (!isInvokeOrInlineExtension(targetSymbol)) {
                    reporter.reportOn(
                        receiverExpression.source ?: qualifiedAccessExpression.source,
                        FirErrors.USAGE_IS_NOT_INLINABLE,
                        receiverSymbol,
                        context
                    )
                } else if (!receiverSymbol.isCrossinline && !isNonLocalReturnAllowed(context, inlineFunction)) {
                    reporter.reportOn(
                        receiverExpression.source ?: qualifiedAccessExpression.source,
                        FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED,
                        receiverSymbol,
                        context,
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

        internal fun checkQualifiedAccess(
            qualifiedAccess: FirStatement,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            val source = qualifiedAccess.source ?: return
            if (targetSymbol !is FirCallableSymbol<*>) return

            if (targetSymbol in inlinableParameters) {
                if (!qualifiedAccess.partOfCall(context)) {
                    reporter.reportOn(source, FirErrors.USAGE_IS_NOT_INLINABLE, targetSymbol, context)
                }
                if (context.containingDeclarations.any { it.symbol in inlinableParameters }) {
                    reporter.reportOn(source, FirErrors.NOT_SUPPORTED_INLINE_PARAMETER_IN_INLINE_PARAMETER_DEFAULT_VALUE, targetSymbol as FirValueParameterSymbol, context)
                }
            }
            checkVisibilityAndAccess(qualifiedAccess, targetSymbol, source, context, reporter)
            checkRecursion(targetSymbol, source, context, reporter)
        }

        private fun FirStatement.partOfCall(context: CheckerContext): Boolean {
            if (this !is FirExpression) return false
            val containingQualifiedAccess = context.callsOrAssignments.getOrNull(
                context.callsOrAssignments.size - 2
            ) ?: return false
            if (this == (containingQualifiedAccess as? FirQualifiedAccessExpression)?.explicitReceiver?.unwrapErrorExpression()) return true
            val call = containingQualifiedAccess as? FirCall ?: return false
            return call.arguments.any { it.unwrapErrorExpression()?.unwrapArgument() == this }
        }

        private fun checkVisibilityAndAccess(
            accessExpression: FirStatement,
            calledDeclaration: FirCallableSymbol<*>?,
            source: KtSourceElement,
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            if (calledDeclaration == null ||
                calledDeclaration.callableId.callableName == BACKING_FIELD ||
                calledDeclaration is FirPropertySymbol && calledDeclaration.isConst &&
                context.callsOrAssignments.any { it is FirAnnotationCall }
            ) {
                return
            }
            val (isInlineFunPublicOrPublishedApi, isCalledFunPublicOrPublishedApi, calledFunEffectiveVisibility) = checkAccessedDeclaration(
                source,
                accessExpression,
                calledDeclaration,
                calledDeclaration.visibility,
                context,
                reporter,
            )

            if (isInlineFunPublicOrPublishedApi && isCalledFunPublicOrPublishedApi) {
                checkSuperCalls(calledDeclaration, accessExpression, context, reporter)
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
                reporter.reportOn(source, factory, inlineFunction.symbol, calledDeclaration, context)
            }
        }

        private fun checkPrivateClassMemberAccess(
            calledDeclaration: FirBasedSymbol<*>,
            source: KtSourceElement,
            context: CheckerContext,
            reporter: DiagnosticReporter,
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
            context: CheckerContext,
            reporter: DiagnosticReporter,
        ) {
            val receiver = when (callExpression) {
                is FirQualifiedAccessExpression -> callExpression.dispatchReceiver
                is FirVariableAssignment -> callExpression.dispatchReceiver
                else -> null
            } as? FirQualifiedAccessExpression ?: return

            if (receiver.calleeReference is FirSuperReference) {
                val dispatchReceiverType = receiver.dispatchReceiver?.resolvedType
                val classSymbol = dispatchReceiverType?.toSymbol(session) ?: return
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
            context: CheckerContext,
            reporter: DiagnosticReporter,
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
            // For primary constructor parameters there's INLINE_PROPERTY_WITH_BACKING_FIELD already
            if (declaration.source?.kind != KtFakeSourceElementKind.PropertyFromParameter) {
                reporter.reportOn(declaration.source, FirErrors.DECLARATION_CANT_BE_INLINED, context)
            }
            return false
        }
        return true
    }

    private fun isInlinableDefaultValue(expression: FirExpression): Boolean =
        expression is FirCallableReferenceAccess ||
                expression is FirFunctionCall ||
                expression is FirLambdaArgumentExpression ||
                expression is FirAnonymousFunctionExpression ||
                (expression is FirLiteralExpression<*> && expression.value == null) //this will be reported separately

    fun checkCallableDeclaration(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirPropertyAccessor) return
        val directOverriddenSymbols = declaration.getDirectOverriddenSymbols(context)
        if (declaration is FirSimpleFunction) {
            checkParameters(declaration, directOverriddenSymbols, context, reporter)
            checkNothingToInline(declaration, context, reporter)
        }
        val canBeInlined = checkCanBeInlined(declaration, declaration.effectiveVisibility, context, reporter)

        if (canBeInlined && directOverriddenSymbols.isNotEmpty()) {
            reporter.reportOn(declaration.source, FirErrors.OVERRIDE_BY_INLINE, context)
        }
    }

    private fun isNonLocalReturnAllowed(context: CheckerContext, inlineFunction: FirFunction): Boolean {
        val declarations = context.containingDeclarations
        val inlineFunctionIndex = declarations.indexOf(inlineFunction)
        if (inlineFunctionIndex == -1) return true

        for (i in (inlineFunctionIndex + 1) until declarations.size) {
            val declaration = declarations[i]

            // Only consider containers which can change locality.
            if (declaration !is FirFunction && declaration !is FirClass) continue

            // Anonymous functions are allowed if they are an argument to an inline function call,
            // and the associated anonymous function parameter allows non-local returns. Everything
            // else changes locality, and must not be allowed.
            val anonymousFunction = declaration as? FirAnonymousFunction ?: return false
            val (call, parameter) = extractCallAndParameter(context, anonymousFunction) ?: return false
            val callable = call.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return false
            if (!callable.isInline && !callable.isArrayLambdaConstructor()) return false
            if (parameter.isNoinline || parameter.isCrossinline) return false
        }

        return true
    }

    private fun extractCallAndParameter(
        context: CheckerContext,
        anonymousFunction: FirAnonymousFunction,
    ): Pair<FirFunctionCall, FirValueParameter>? {
        for (call in context.callsOrAssignments) {
            if (call is FirFunctionCall) {
                val mapping = call.resolvedArgumentMapping ?: continue
                for ((argument, parameter) in mapping) {
                    if ((argument.unwrapArgument() as? FirAnonymousFunctionExpression)?.anonymousFunction === anonymousFunction) {
                        return call to parameter
                    }
                }
            }
        }
        return null
    }

    /**
     * @return true if the symbol is the constructor of one of 9 array classes (`Array<T>`,
     * `IntArray`, `FloatArray`, ...) which takes the size and an initializer lambda as parameters.
     * Such constructors are marked as `inline` but they are not loaded as such because the `inline`
     * flag is not stored for constructors in the binary metadata. Therefore, we pretend that they
     * are inline.
     */
    private fun FirFunctionSymbol<*>.isArrayLambdaConstructor(): Boolean {
        return this is FirConstructorSymbol &&
                valueParameterSymbols.size == 2 &&
                resolvedReturnType.isArrayOrPrimitiveArray
    }
}

fun createInlineFunctionBodyContext(function: FirFunction, session: FirSession): FirInlineDeclarationChecker.InlineFunctionBodyContext {
    val inlineableParameters = function.valueParameters.filter {
        if (it.isNoinline) return@filter false
        val type = it.returnTypeRef.coneType
        !type.isMarkedNullable && type.isNonReflectFunctionType(session)
    }.map { it.symbol }

    return FirInlineDeclarationChecker.InlineFunctionBodyContext(
        function,
        function.publishedApiEffectiveVisibility ?: function.effectiveVisibility,
        inlineableParameters,
        session,
    )
}
