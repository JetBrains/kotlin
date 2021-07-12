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
import org.jetbrains.kotlin.fir.analysis.checkers.util.checkChildrenWithCustomVisitor
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isFunctionalType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirInlineDeclarationChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInline) return
        // local inline functions are prohibited
        if (declaration.isLocalMember) return
        if (declaration !is FirPropertyAccessor && declaration !is FirSimpleFunction) return

        val effectiveVisibility = declaration.effectiveVisibility
        checkInlineFunctionBody(declaration, effectiveVisibility, context, reporter)
    }

    private fun checkInlineFunctionBody(
        function: FirFunction,
        effectiveVisibility: EffectiveVisibility,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val body = function.body ?: return
        val inlinableParameters = function.valueParameters.filter {
            if (it.isNoinline) return@filter false
            val type = it.returnTypeRef.coneType
            !type.isMarkedNullable && type.isFunctionalType(context.session) { kind -> !kind.isReflectType }
        }

        val visitor = Visitor(
            function,
            effectiveVisibility,
            inlinableParameters,
            context.session,
            reporter
        )
        body.checkChildrenWithCustomVisitor(context, visitor)
    }

    private class Visitor(
        val inlineFunction: FirFunction,
        val inlineFunEffectiveVisibility: EffectiveVisibility,
        val inlinableParameters: List<FirValueParameter>,
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
            val setterSymbol = propertySymbol.fir.setter?.symbol ?: return
            checkQualifiedAccess(variableAssignment, setterSymbol, data)
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
            val calledFunction = (targetSymbol as? FirNamedFunctionSymbol)?.fir ?: return
            val argumentMapping = functionCall.resolvedArgumentMapping ?: return
            for ((wrappedArgument, valueParameter) in argumentMapping) {
                val argument = wrappedArgument.unwrapArgument()
                val resolvedArgumentSymbol = argument.toResolvedCallableSymbol() as? FirVariableSymbol<*> ?: continue

                val valueParameterOfOriginalInlineFunction = inlinableParameters.firstOrNull { it == resolvedArgumentSymbol.fir }
                if (valueParameterOfOriginalInlineFunction != null) {
                    val factory = when {
                        calledFunction.isInline -> when {
                            valueParameter.isNoinline -> FirErrors.USAGE_IS_NOT_INLINABLE
                            valueParameter.isCrossinline && !valueParameterOfOriginalInlineFunction.isCrossinline
                            -> FirErrors.NON_LOCAL_RETURN_NOT_ALLOWED
                            else -> continue
                        }
                        else -> FirErrors.USAGE_IS_NOT_INLINABLE
                    }
                    reporter.reportOn(argument.source, factory, valueParameterOfOriginalInlineFunction.symbol, context)
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
            if (receiverSymbol.fir in inlinableParameters) {
                val valueParameter = receiverSymbol.fir as FirValueParameter
                if (!isInvokeOrInlineExtension(targetSymbol)) {
                    reporter.reportOn(
                        qualifiedAccessExpression.source,
                        FirErrors.USAGE_IS_NOT_INLINABLE,
                        valueParameter.symbol,
                        context
                    )
                }
            }
        }

        private fun isInvokeOrInlineExtension(targetSymbol: FirBasedSymbol<*>?): Boolean {
            if (targetSymbol !is FirNamedFunctionSymbol) return false
            val function = targetSymbol.fir
            if (function.isInline) return true
            return function.name == OperatorNameConventions.INVOKE &&
                    function.dispatchReceiverType?.isBuiltinFunctionalType(session) == true
        }

        private fun checkQualifiedAccess(
            qualifiedAccess: FirQualifiedAccess,
            targetSymbol: FirBasedSymbol<*>?,
            context: CheckerContext
        ) {
            val source = qualifiedAccess.source ?: return
            if (targetSymbol == null) return
            val targetFir = targetSymbol.fir as? FirCallableDeclaration

            if (targetSymbol.fir in inlinableParameters) {
                if (!qualifiedAccess.partOfCall(context)) {
                    val valueParameter = targetSymbol.fir as FirValueParameter
                    reporter.reportOn(source, FirErrors.USAGE_IS_NOT_INLINABLE, valueParameter.symbol, context)
                }
            }
            checkVisibilityAndAccess(qualifiedAccess, targetFir, source, context)
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
            calledDeclaration: FirCallableDeclaration?,
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
                    calledDeclaration.symbol,
                    inlineFunction.symbol,
                    context
                )
            } else {
                checkPrivateClassMemberAccess(calledDeclaration, source, context)
                if (isInlineFunPublicOrPublishedApi) {
                    checkSuperCalls(calledDeclaration, accessExpression, context)
                }
            }

            val isConstructorCall = calledDeclaration is FirConstructor
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
                reporter.reportOn(source, factory, calledDeclaration.symbol, inlineFunction.symbol, context)
            }
        }

        private fun checkPrivateClassMemberAccess(
            calledDeclaration: FirCallableDeclaration,
            source: FirSourceElement,
            context: CheckerContext
        ) {
            if (!isEffectivelyPrivateApiFunction) {
                if (calledDeclaration.isInsidePrivateClass()) {
                    reporter.reportOn(
                        source,
                        FirErrors.PRIVATE_CLASS_MEMBER_FROM_INLINE,
                        calledDeclaration.symbol,
                        inlineFunction.symbol,
                        context
                    )
                }
            }
        }

        private fun checkSuperCalls(
            calledDeclaration: FirCallableDeclaration,
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
                        calledDeclaration.symbol,
                        context
                    )
                }
            }
        }

        private fun FirBasedSymbol<*>.isDefinedInInlineFunction(): Boolean {
            return when (val fir = this.fir) {
                is FirAnonymousFunction -> true
                is FirMemberDeclaration -> fir.isLocalMember
                is FirAnonymousObject -> true
                is FirRegularClass -> fir.classId.isLocal
                else -> error("Unknown callable declaration type: ${fir.render()}")
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

        private fun FirCallableDeclaration.isInsidePrivateClass(): Boolean {
            val containingClass = this.containingClass()?.toSymbol(session)?.fir ?: return false

            val containingClassVisibility = when (containingClass) {
                is FirAnonymousObject -> return false
                is FirRegularClass -> containingClass.visibility
                is FirTypeAlias -> containingClass.visibility
            }
            return containingClassVisibility == Visibilities.Private || containingClassVisibility == Visibilities.PrivateToThis
        }
    }
}
