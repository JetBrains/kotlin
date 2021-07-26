/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_2_20_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_2_30_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_3_FQ_NAME
import org.jetbrains.kotlin.serialization.deserialization.KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirSuspendCallChecker : FirQualifiedAccessExpressionChecker() {
    private val SUSPEND_PROPERTIES_FQ_NAMES = setOf(
        COROUTINE_CONTEXT_1_2_20_FQ_NAME, COROUTINE_CONTEXT_1_2_30_FQ_NAME, COROUTINE_CONTEXT_1_3_FQ_NAME
    )

    private val RESTRICTS_SUSPENSION_CLASS_ID =
        ClassId(StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE, Name.identifier("RestrictsSuspension"))

    private val BUILTIN_SUSPEND_NAME = KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME.shortName()

    @OptIn(SymbolInternals::class)
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val reference = expression.calleeReference as? FirResolvedNamedReference ?: return
        val symbol = reference.resolvedSymbol as? FirCallableSymbol ?: return
        symbol.ensureResolved(FirResolvePhase.STATUS)
        val fir = symbol.fir as? FirCallableDeclaration ?: return
        if (reference.name == BUILTIN_SUSPEND_NAME ||
            fir is FirSimpleFunction && fir.name == BUILTIN_SUSPEND_NAME
        ) {
            checkSuspendModifierForm(expression, reference, symbol, context, reporter)
        }
        if (reference is FirResolvedCallableReference) return
        when (fir) {
            is FirSimpleFunction -> if (!fir.isSuspend) return
            is FirProperty -> if (symbol.callableId.asSingleFqName() !in SUSPEND_PROPERTIES_FQ_NAMES) return
            else -> return
        }
        val enclosingSuspendFunction = findEnclosingSuspendFunction(context)
        if (enclosingSuspendFunction == null) {
            when (fir) {
                is FirSimpleFunction -> reporter.reportOn(expression.source, FirErrors.ILLEGAL_SUSPEND_FUNCTION_CALL, symbol, context)
                is FirProperty -> reporter.reportOn(expression.source, FirErrors.ILLEGAL_SUSPEND_PROPERTY_ACCESS, symbol, context)
                else -> {
                }
            }
        } else {
            if (!checkNonLocalReturnUsage(enclosingSuspendFunction, context)) {
                reporter.reportOn(expression.source, FirErrors.NON_LOCAL_SUSPENSION_POINT, context)
            }
            if (!checkRestrictsSuspension(expression, enclosingSuspendFunction, fir, context)) {
                reporter.reportOn(expression.source, FirErrors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL, context)
            }
        }
    }

    private fun checkSuspendModifierForm(
        expression: FirQualifiedAccessExpression,
        reference: FirResolvedNamedReference,
        symbol: FirCallableSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (symbol.callableId.asSingleFqName() == KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME) {
            if (reference.name != BUILTIN_SUSPEND_NAME ||
                expression.explicitReceiver != null ||
                !expression.hasFormOfSuspendModifierForLambda()
            ) {
                reporter.reportOn(expression.source, FirErrors.NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND, context)
            }
        } else {
            if (reference.name == BUILTIN_SUSPEND_NAME && expression.hasFormOfSuspendModifierForLambda()) {
                reporter.reportOn(expression.source, FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND, context)
            }
        }
    }

    private fun FirQualifiedAccessExpression.hasFormOfSuspendModifierForLambda(): Boolean {
        if (this !is FirFunctionCall) return false
        val reference = this.calleeReference
        if (reference is FirResolvedCallableReference) return false
        if (typeArguments.any { it.source != null }) return false
        if (arguments.singleOrNull() is FirLambdaArgumentExpression) {
            // No brackets should be in a selector call
            val callExpressionSource =
                if (explicitReceiver == null) source
                else source?.getChild(KtNodeTypes.CALL_EXPRESSION, index = 1, depth = 1)
            if (callExpressionSource?.getChild(KtNodeTypes.VALUE_ARGUMENT_LIST, depth = 1) == null) {
                return true
            }
        }
        if (source?.elementType == KtNodeTypes.BINARY_EXPRESSION) {
            val lastArgument = arguments.lastOrNull()
            if (lastArgument is FirAnonymousFunctionExpression && source?.getChild(KtNodeTypes.PARENTHESIZED, depth = 1) == null) {
                return true
            }
        }
        return false
    }

    private fun findEnclosingSuspendFunction(context: CheckerContext): FirFunction? {
        return context.containingDeclarations.lastOrNull {
            when (it) {
                is FirAnonymousFunction -> it.typeRef.coneType.isSuspendFunctionType(context.session)
                is FirSimpleFunction -> it.isSuspend
                else -> false
            }
        } as? FirFunction
    }

    private fun checkNonLocalReturnUsage(enclosingSuspendFunction: FirFunction, context: CheckerContext): Boolean {
        val containingFunction = context.containingDeclarations.lastIsInstanceOrNull<FirFunction>() ?: return false
        return if (containingFunction is FirAnonymousFunction && enclosingSuspendFunction !== containingFunction) {
            containingFunction.inlineStatus.returnAllowed
        } else {
            enclosingSuspendFunction === containingFunction
        }
    }

    private fun checkRestrictsSuspension(
        expression: FirQualifiedAccessExpression,
        enclosingSuspendFunction: FirFunction,
        calledDeclaration: FirCallableDeclaration,
        context: CheckerContext
    ): Boolean {
        val session = context.session
        val enclosingSuspendFunctionDispatchReceiverOwner =
            (enclosingSuspendFunction.dispatchReceiverType as? ConeClassLikeType)?.lookupTag?.toFirRegularClass(session)
        val enclosingSuspendFunctionExtensionReceiverOwner = enclosingSuspendFunction.takeIf { it.receiverTypeRef != null }
        val dispatchReceiverExpression = expression.dispatchReceiver.takeIf { it !is FirNoReceiverExpression }
        val extensionReceiverExpression = expression.extensionReceiver.takeIf { it !is FirNoReceiverExpression }
        for (receiverExpression in listOfNotNull(dispatchReceiverExpression, extensionReceiverExpression)) {
            if (!receiverExpression.typeRef.coneType.isRestrictSuspensionReceiver(session)) continue
            if (sameInstanceOfReceiver(receiverExpression, enclosingSuspendFunctionDispatchReceiverOwner)) continue
            if (sameInstanceOfReceiver(receiverExpression, enclosingSuspendFunctionExtensionReceiverOwner)) continue

            return false
        }
        if (enclosingSuspendFunctionExtensionReceiverOwner?.receiverTypeRef?.coneType?.isRestrictSuspensionReceiver(session) != true) {
            return true
        }
        if (sameInstanceOfReceiver(dispatchReceiverExpression, enclosingSuspendFunctionExtensionReceiverOwner)) {
            return true
        }
        if (sameInstanceOfReceiver(extensionReceiverExpression, enclosingSuspendFunctionExtensionReceiverOwner)) {
            if (calledDeclaration.receiverTypeRef?.coneType?.isRestrictSuspensionReceiver(session) == true) {
                return true
            }
        }
        return false
    }

    private fun ConeKotlinType.isRestrictSuspensionReceiver(session: FirSession): Boolean {
        when (this) {
            is ConeClassLikeType -> {
                val regularClass = fullyExpandedType(session).lookupTag.toFirRegularClass(session) ?: return false
                if (regularClass.hasAnnotation(RESTRICTS_SUSPENSION_CLASS_ID)) {
                    return true
                }
                return regularClass.superTypeRefs.any { it.coneType.isRestrictSuspensionReceiver(session) }
            }
            is ConeTypeParameterType -> {
                return lookupTag.typeParameterSymbol.resolvedBounds.any { it.coneType.isRestrictSuspensionReceiver(session) }
            }
            else -> return false
        }
    }

    private fun sameInstanceOfReceiver(useSiteReceiverExpression: FirExpression?, declarationSiteReceiverOwner: FirDeclaration?): Boolean {
        if (declarationSiteReceiverOwner == null || useSiteReceiverExpression == null) return false
        if (useSiteReceiverExpression is FirThisReceiverExpression) {
            return useSiteReceiverExpression.calleeReference.boundSymbol == declarationSiteReceiverOwner.symbol
        }
        return false
    }
}
