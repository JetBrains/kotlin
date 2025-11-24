/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicit
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isContextParameter
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.util.getChildren

object FirSuspendCallChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val reference = expression.calleeReference.resolved ?: return
        val symbol = reference.resolvedSymbol as? FirCallableSymbol ?: return
        if (reference.name == StandardClassIds.Callables.suspend.callableName ||
            symbol is FirNamedFunctionSymbol && symbol.name == StandardClassIds.Callables.suspend.callableName
        ) {
            checkSuspendModifierForm(expression, reference, symbol)
        }

        if (reference is FirResolvedCallableReference) {
            checkCallableReference(expression, symbol)
            return
        }

        when (symbol) {
            is FirNamedFunctionSymbol -> if (!symbol.isSuspend) return
            is FirPropertySymbol -> if (symbol.callableId != StandardClassIds.Callables.coroutineContext) return
            else -> return
        }
        val enclosingSuspendFunction = findEnclosingSuspendFunction()
        if (enclosingSuspendFunction == null) {
            when (symbol) {
                is FirNamedFunctionSymbol -> reporter.reportOn(expression.source, FirErrors.ILLEGAL_SUSPEND_FUNCTION_CALL, symbol)
                is FirPropertySymbol -> reporter.reportOn(expression.source, FirErrors.ILLEGAL_SUSPEND_PROPERTY_ACCESS, symbol)
                else -> {
                }
            }
        } else {
            if (!checkNonLocalReturnUsage(enclosingSuspendFunction)) {
                reporter.reportOn(expression.source, FirErrors.NON_LOCAL_SUSPENSION_POINT)
            }
            if (isInScopeForDefaultParameterValues(enclosingSuspendFunction)) {
                reporter.reportOn(
                    expression.source,
                    FirErrors.UNSUPPORTED,
                    "Suspend function call in default parameter value is unsupported."
                )
            }
            if (!checkRestrictsSuspension(expression, enclosingSuspendFunction, symbol)) {
                reporter.reportOn(expression.source, FirErrors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuspendModifierForm(
        expression: FirQualifiedAccessExpression,
        reference: FirResolvedNamedReference,
        symbol: FirCallableSymbol<*>,
    ) {
        if (symbol.callableId == StandardClassIds.Callables.suspend) {
            if (reference.name != StandardClassIds.Callables.suspend.callableName ||
                expression.explicitReceiver != null ||
                expression.formOfSuspendModifierForLambdaOrFun() == null
            ) {
                reporter.reportOn(expression.source, FirErrors.NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND)
            }
        } else if (reference.name == StandardClassIds.Callables.suspend.callableName) {
            when (expression.formOfSuspendModifierForLambdaOrFun()) {
                SuspendCallArgumentKind.FUN -> {
                    reporter.reportOn(expression.source, FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND_FUN)
                }
                SuspendCallArgumentKind.LAMBDA -> {
                    reporter.reportOn(expression.source, FirErrors.MODIFIER_FORM_FOR_NON_BUILT_IN_SUSPEND)
                }
                null -> {
                    // Nothing to do
                }
            }
        }
    }

    private fun FirQualifiedAccessExpression.formOfSuspendModifierForLambdaOrFun(): SuspendCallArgumentKind? {
        if (this !is FirFunctionCall) return null
        val reference = this.calleeReference
        if (reference is FirResolvedCallableReference) return null
        if (typeArguments.any { it.isExplicit }) return null
        if (arguments.singleOrNull().let { it is FirAnonymousFunctionExpression && it.isTrailingLambda }) {
            // No brackets should be in a selector call
            val callExpressionSource =
                if (explicitReceiver == null) source
                else source?.getChild(KtNodeTypes.CALL_EXPRESSION, index = 1, depth = 1)
            if (callExpressionSource?.getChild(KtNodeTypes.VALUE_ARGUMENT_LIST, depth = 1) == null) {
                return SuspendCallArgumentKind.LAMBDA
            }
        }
        if (origin == FirFunctionCallOrigin.Infix) {
            val lastArgument = arguments.lastOrNull()
            if (lastArgument is FirAnonymousFunctionExpression && source?.getChild(KtNodeTypes.PARENTHESIZED, depth = 1) == null) {
                return if (lastArgument.source?.lighterASTNode?.tokenType == KtStubElementTypes.FUNCTION) {
                    SuspendCallArgumentKind.FUN
                } else {
                    SuspendCallArgumentKind.LAMBDA
                }
            }
        }
        return null
    }

    context(context: CheckerContext)
    private fun findEnclosingSuspendFunction(): FirFunctionSymbol<*>? {
        return context.containingDeclarations.lastOrNull {
            when (it) {
                is FirAnonymousFunctionSymbol ->
                    if (it.isLambda) it.resolvedTypeRef.coneType.isSuspendOrKSuspendFunctionType(context.session) else it.isSuspend
                is FirNamedFunctionSymbol ->
                    it.isSuspend
                else ->
                    false
            }
        } as? FirFunctionSymbol
    }

    context(context: CheckerContext)
    private fun isInScopeForDefaultParameterValues(
        enclosingSuspendFunction: FirFunctionSymbol<*>
    ): Boolean {
        val valueParameters = enclosingSuspendFunction.valueParameterSymbols
        for (declaration in context.containingDeclarations.asReversed()) {
            when {
                declaration is FirValueParameterSymbol && declaration in valueParameters && declaration.hasDefaultValue -> return true
                declaration is FirAnonymousFunctionSymbol && declaration.inlineStatus == InlineStatus.Inline -> continue
                declaration is FirFunctionSymbol && !declaration.isInline -> return false
            }
        }
        return false
    }

    context(context: CheckerContext)
    private fun checkNonLocalReturnUsage(enclosingSuspendFunction: FirFunctionSymbol<*>): Boolean {
        for (declaration in context.containingDeclarations.asReversed()) {
            // If we found the nearest suspend function, we're finished.
            if (declaration == enclosingSuspendFunction) return true
            // Local variables are okay.
            if (declaration is FirPropertySymbol && declaration is FirLocalPropertySymbol) continue
            // Inline lambdas are okay.
            if (declaration is FirAnonymousFunctionSymbol && declaration.inlineStatus.returnAllowed) continue
            // We already report UNSUPPORTED on suspend calls in value parameters default values, so they are okay for our purposes.
            if (declaration is FirValueParameterSymbol) continue
            // Everything else (local classes, init blocks, non-inline lambdas, etc.F) is not okay.
            return false
        }

        return false
    }

    context(context: CheckerContext)
    private fun checkRestrictsSuspension(
        expression: FirQualifiedAccessExpression,
        enclosingSuspendFunction: FirFunctionSymbol<*>,
        calledDeclarationSymbol: FirCallableSymbol<*>,
    ): Boolean {
        if (expression is FirFunctionCall && isCaseMissedByK1(expression)) {
            return true
        }

        val session = context.session

        val enclosingSuspendFunctionDispatchReceiverOwnerSymbol =
            enclosingSuspendFunction.dispatchReceiverType?.classLikeLookupTagIfAny?.toRegularClassSymbol()
        val enclosingSuspendFunctionExtensionReceiverSymbol = enclosingSuspendFunction.receiverParameterSymbol
        val enclosingSuspendFunctionContextParameterSymbols = enclosingSuspendFunction.contextParameterSymbols

        val receiversInfo = expression.computeReceiversInfo(session, calledDeclarationSymbol)
        for (receiverExpression in receiversInfo.expressions) {
            if (!receiverExpression.resolvedType.isRestrictSuspensionReceiver(session)) continue
            if (sameInstanceOfReceiver(receiverExpression, enclosingSuspendFunctionDispatchReceiverOwnerSymbol)) continue
            if (sameInstanceOfReceiver(receiverExpression, enclosingSuspendFunctionExtensionReceiverSymbol)) continue
            if (enclosingSuspendFunctionContextParameterSymbols.any { sameInstanceOfReceiver(receiverExpression, it) }) continue

            return false
        }

        val restrictSuspensionSymbols: List<FirBasedSymbol<*>> =
            listOfNotNull(enclosingSuspendFunctionExtensionReceiverSymbol?.takeIf { it.resolvedType.isRestrictSuspensionReceiver(session) }) +
                    enclosingSuspendFunctionContextParameterSymbols.filter { it.resolvedReturnType.isRestrictSuspensionReceiver(session) }

        val chosenRestrictSuspensionSymbol = when (restrictSuspensionSymbols.size) {
            0 -> return true
            1 -> restrictSuspensionSymbols.single()
            else -> return false
        }

        if (sameInstanceOfReceiver(receiversInfo.dispatchReceiverExpression, chosenRestrictSuspensionSymbol)) {
            return true
        }

        for (receiver in listOf(receiversInfo.extensionReceiver) + receiversInfo.contextParameters) {
            if (sameInstanceOfReceiver(receiver.expression, chosenRestrictSuspensionSymbol)) {
                if (receiver.type?.isRestrictSuspensionReceiver(session) == true) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * This function exists because of KT-65272:
     *
     * ```
     * @RestrictsSuspension
     * object TestScope
     *
     * val testLambda: suspend TestScope.() -> Unit
     *     get() = TODO()
     *
     * suspend fun test() {
     *     TestScope.testLambda()        // ❌️K1 ❌️K2
     *     testLambda(TestScope)         // ✅️K1 ❌️K2  <-- Working K1 code now fails to compile
     *     testLambda.invoke(TestScope)  // ✅️K1 ✅️K2
     * }
     * ```
     *
     * It was decided to replicate K1 behavior for now, so function
     * returns `true` when given an expression like `testLambda(TestScope)`:
     * an implicit invoke call on a receiver of an extension function type
     * such that the receiver argument is passed as a value argument.
     */
    private fun isCaseMissedByK1(expression: FirFunctionCall): Boolean {
        val isInvokeFromExtensionFunctionType = expression is FirImplicitInvokeCall
                && expression.explicitReceiver?.resolvedType?.isExtensionFunctionType == true

        if (!isInvokeFromExtensionFunctionType) {
            return false
        }

        val source = expression.source
            ?: return false

        val visualValueArgumentsCount = source
            .getChild(KtNodeTypes.VALUE_ARGUMENT_LIST, depth = 1)
            ?.lighterASTNode?.getChildren(source.treeStructure)
            ?.filter { it.tokenType == KtNodeTypes.VALUE_ARGUMENT }
            ?.size
            ?: return false

        return visualValueArgumentsCount != expression.arguments.count() - 1
    }

    private fun ConeKotlinType.isRestrictSuspensionReceiver(session: FirSession): Boolean {
        when (this) {
            is ConeClassLikeType -> {
                val regularClassSymbol = fullyExpandedType(session).lookupTag.toRegularClassSymbol(session) ?: return false
                if (regularClassSymbol.hasAnnotationWithClassId(StandardClassIds.Annotations.RestrictsSuspension, session)) {
                    return true
                }
                return regularClassSymbol.resolvedSuperTypes.any { it.isRestrictSuspensionReceiver(session) }
            }
            is ConeTypeParameterType -> {
                return lookupTag.typeParameterSymbol.resolvedBounds.any { it.coneType.isRestrictSuspensionReceiver(session) }
            }
            else -> return false
        }
    }

    private fun sameInstanceOfReceiver(
        useSiteReceiverExpression: FirExpression?,
        declarationSiteReceiverOwnerSymbol: FirBasedSymbol<*>?
    ): Boolean = when (val unwrappedReceiver = useSiteReceiverExpression?.unwrapSmartcastExpression()) {
        null -> false
        else if declarationSiteReceiverOwnerSymbol == null -> false
        is FirThisReceiverExpression ->
            unwrappedReceiver.calleeReference.boundSymbol == declarationSiteReceiverOwnerSymbol
        is FirPropertyAccessExpression ->
            declarationSiteReceiverOwnerSymbol is FirValueParameterSymbol
                    && declarationSiteReceiverOwnerSymbol.isContextParameter()
                    && unwrappedReceiver.toResolvedCallableSymbol() == declarationSiteReceiverOwnerSymbol
        else -> false
    }

    data class ReceiversInfo(
        val dispatchReceiverExpression: FirExpression?,
        val extensionReceiver: ReceiverInfo,
        val contextParameters: List<ReceiverInfo>,
    ) {
        val expressions: List<FirExpression> =
            listOfNotNull(dispatchReceiverExpression, extensionReceiver.expression) + contextParameters.mapNotNull { it.expression }
    }

    data class ReceiverInfo(val expression: FirExpression?, val type: ConeKotlinType?)

    private fun FirQualifiedAccessExpression.computeReceiversInfo(
        session: FirSession,
        calledDeclarationSymbol: FirCallableSymbol<*>
    ): ReceiversInfo {
        val dispatchReceiver = dispatchReceiver
        if (this is FirImplicitInvokeCall &&
            dispatchReceiver != null && dispatchReceiver.resolvedType.isSuspendOrKSuspendFunctionType(session)
        ) {
            val variableForInvoke = dispatchReceiver
            val variableForInvokeType = variableForInvoke.resolvedType

            if (!variableForInvokeType.hasContextParameters && !variableForInvokeType.isExtensionFunctionType) {
                return ReceiversInfo(null, ReceiverInfo(null, null), emptyList())
            }

            val amountOfContexts = variableForInvokeType.contextParameterNumberForFunctionType
            val contexts = zipReceiverInfo(
                argumentList.arguments.take(amountOfContexts),
                variableForInvokeType.typeArguments.take(amountOfContexts).map { it as? ConeKotlinType }
            )
            val extension = ReceiverInfo(
                argumentList.arguments.getOrNull(amountOfContexts),
                variableForInvokeType.typeArguments.getOrNull(amountOfContexts) as? ConeKotlinType
            )

            // `a.foo()` is resolved to invokeExtension, so it's been desugared to `foo.invoke(a)`
            // And we use the first argument (`a`) as an extension receiver
            return ReceiversInfo(null, extension, contexts)
        }

        return ReceiversInfo(
            dispatchReceiver,
            ReceiverInfo(extensionReceiver, calledDeclarationSymbol.resolvedReceiverType),
            zipReceiverInfo(contextArguments, calledDeclarationSymbol.contextParameterSymbols.map { it.resolvedReturnType })
        )
    }

    private fun zipReceiverInfo(expressions: List<FirExpression>, types: List<ConeKotlinType?>): List<ReceiverInfo> =
        expressions.zip(types, ::ReceiverInfo)

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkCallableReference(
        expression: FirQualifiedAccessExpression,
        symbol: FirCallableSymbol<*>,
    ) {
        if (symbol.callableId == StandardClassIds.Callables.coroutineContext) {
            reporter.reportOn(
                expression.calleeReference.source,
                FirErrors.UNSUPPORTED,
                "Callable reference to suspend property is unsupported."
            )
        }
    }
}

private enum class SuspendCallArgumentKind {
    FUN, LAMBDA
}
