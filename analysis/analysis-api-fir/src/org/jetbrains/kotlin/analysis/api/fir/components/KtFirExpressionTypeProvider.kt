/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtExpressionTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.getReferencedElementType
import org.jetbrains.kotlin.analysis.api.fir.utils.unwrap
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.constructFunctionalType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal class KtFirExpressionTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtExpressionTypeProvider(), KtFirAnalysisSessionComponent {

    override fun getKtExpressionType(expression: KtExpression): KtType? {
        return when (val fir = expression.unwrap().getOrBuildFir(firResolveSession)) {
            is FirFunctionCall -> {
                getReturnTypeForArrayStyleAssignmentTarget(expression, fir)
                    ?: fir.typeRef.coneType.asKtType()
            }
            is FirPropertyAccessExpression -> {
                // For unresolved `super`, we manually create an intersection type so that IDE features like completion can work correctly.
                val containingClass =
                    (fir.dispatchReceiver as? FirThisReceiverExpression)?.calleeReference?.boundSymbol as? FirClassSymbol<*>
                if (fir.calleeReference is FirSuperReference && fir.typeRef is FirErrorTypeRef && containingClass != null) {
                    val superTypes = containingClass.resolvedSuperTypes
                    when (superTypes.size) {
                        0 -> analysisSession.builtinTypes.ANY
                        1 -> superTypes.single().asKtType()
                        else -> ConeIntersectionType(superTypes).asKtType()
                    }
                } else {
                    fir.typeRef.coneType.asKtType()
                }
            }
            is FirVariableAssignment -> {
                if (expression is KtUnaryExpression && expression.operationToken in KtTokens.INCREMENT_AND_DECREMENT) {
                    fir.rValue.typeRef.coneType.asKtType()
                } else {
                    analysisSession.builtinTypes.UNIT
                }
            }
            is FirExpression -> fir.typeRef.coneType.asKtType()
            is FirNamedReference -> fir.getReferencedElementType().asKtType()
            is FirStatement -> with(analysisSession) { builtinTypes.UNIT }
            is FirTypeRef, is FirImport, is FirPackageDirective, is FirLabel -> null
            // For invalid code like the following,
            // ```
            // when {
            //   true, false -> {}
            // }
            // ```
            // `false` does not have a corresponding elements on the FIR side and hence the containing `FirWhenBranch` is returned. In this
            // case, we simply report null since FIR does not know about it.
            is FirWhenBranch -> null
            else -> error("Unexpected ${fir?.let { it::class }} for ${expression::class} with text `${expression.text}`")
        }
    }

    private fun getReturnTypeForArrayStyleAssignmentTarget(
        expression: KtExpression,
        fir: FirFunctionCall
    ): KtType? {
        if (fir.calleeReference !is FirResolvedNamedReference) return null
        if (expression !is KtArrayAccessExpression) return null
        val assignment = expression.parent as? KtBinaryExpression ?: return null
        if (assignment.operationToken !in KtTokens.ALL_ASSIGNMENTS) return null
        if (assignment.left != expression) return null
        val setTargetArgumentParameter = fir.resolvedArgumentMapping?.entries?.last()?.value ?: return null
        return setTargetArgumentParameter.returnTypeRef.coneType.asKtType()
    }

    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType {
        val firDeclaration = if (isAnonymousFunction(declaration))
            declaration.toFirAnonymousFunction()
        else
            declaration.getOrBuildFir(firResolveSession)
        return when (firDeclaration) {
            is FirCallableDeclaration -> firDeclaration.returnTypeRef.coneType.asKtType()
            is FirFunctionTypeParameter -> firDeclaration.returnTypeRef.coneType.asKtType()
            else -> unexpectedElementError<FirElement>(firDeclaration)
        }
    }

    override fun getFunctionalTypeForKtFunction(declaration: KtFunction): KtType {
        val firFunction = if (isAnonymousFunction(declaration))
            declaration.toFirAnonymousFunction()
        else
            declaration.getOrBuildFirOfType<FirFunction>(firResolveSession)
        return firFunction.constructFunctionalType(firFunction.isSuspend).asKtType()
    }

    @OptIn(ExperimentalContracts::class)
    private fun isAnonymousFunction(ktDeclaration: KtDeclaration): Boolean {
        contract {
            returns(true) implies (ktDeclaration is KtNamedFunction)
        }
        return ktDeclaration is KtNamedFunction && ktDeclaration.isAnonymous
    }

    private fun KtFunction.toFirAnonymousFunction(): FirAnonymousFunction {
        return getOrBuildFirOfType<FirAnonymousFunctionExpression>(firResolveSession).anonymousFunction
    }

    override fun getExpectedType(expression: PsiElement): KtType? {
        val unwrapped = expression.unwrap()
        val expectedType = getExpectedTypeByReturnExpression(unwrapped)
            ?: getExpressionTypeByIfOrBooleanCondition(unwrapped)
            ?: getExpectedTypeByTypeCast(unwrapped)
            ?: getExpectedTypeOfFunctionParameter(unwrapped)
            ?: getExpectedTypeOfInfixFunctionParameter(unwrapped)
            ?: getExpectedTypeByVariableAssignment(unwrapped)
            ?: getExpectedTypeByPropertyDeclaration(unwrapped)
            ?: getExpectedTypeByFunctionExpressionBody(unwrapped)
        return expectedType
    }

    private fun getExpectedTypeByTypeCast(expression: PsiElement): KtType? {
        val typeCastExpression =
            expression.unwrapQualified<KtBinaryExpressionWithTypeRHS> { castExpr, expr -> castExpr.left == expr } ?: return null
        with(analysisSession) {
            return typeCastExpression.right?.getKtType()
        }
    }

    private fun getExpectedTypeOfFunctionParameter(expression: PsiElement): KtType? {
        val (ktCallExpression, argumentExpression) = expression.getFunctionCallAsWithThisAsParameter() ?: return null
        val firCall = ktCallExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveSession) ?: return null

        val callee = (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        if (callee?.fir?.origin == FirDeclarationOrigin.SamConstructor) {
            return (callee.fir as FirSimpleFunction).returnTypeRef.coneType.asKtType()
        }

        val arguments = firCall.resolvedArgumentMapping ?: return null
        val firParameterForExpression =
            arguments.entries.firstOrNull { (arg, _) ->
                when (arg) {
                    // TODO: better to utilize. See `createArgumentMapping` in [KtFirCallResolver]
                    is FirLambdaArgumentExpression, is FirNamedArgumentExpression, is FirSpreadArgumentExpression ->
                        arg.psi == argumentExpression.parent
                    else ->
                        arg.psi == argumentExpression
                }
            }?.value ?: return null
        val coneType = firParameterForExpression.returnTypeRef.coneType
        return if (firParameterForExpression.isVararg)
            coneType.varargElementType().asKtType()
        else
            coneType.asKtType()
    }

    private fun PsiElement.getFunctionCallAsWithThisAsParameter(): KtCallWithArgument? {
        val valueArgument = unwrapQualified<KtValueArgument> { valueArg, expr ->
            // If `valueArg` is [KtLambdaArgument], its [getArgumentExpression] could be labeled expression (e.g., l@{ ... }).
            // That is not exactly `expr`, which would be [KtLambdaExpression]. So, we need [unwrap] here.
            valueArg.getArgumentExpression()?.unwrap() == expr
        } ?: return null
        val callExpression =
            (valueArgument.parent as? KtValueArgumentList)?.parent as? KtCallExpression
                ?: valueArgument.parent as? KtCallExpression // KtLambdaArgument
                ?: return null
        val argumentExpression = valueArgument.getArgumentExpression() ?: return null
        return KtCallWithArgument(callExpression, argumentExpression)
    }

    private fun getExpectedTypeOfInfixFunctionParameter(expression: PsiElement): KtType? {
        val infixCallExpression =
            expression.unwrapQualified<KtBinaryExpression> { binaryExpr, expr -> binaryExpr.right == expr } ?: return null
        val firCall = infixCallExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveSession) ?: return null

        // There is only one parameter for infix functions; get its type
        val arguments = firCall.resolvedArgumentMapping ?: return null
        val firParameterForExpression = arguments.values.singleOrNull() ?: return null
        return firParameterForExpression.returnTypeRef.coneType.asKtType()
    }

    private fun getExpectedTypeByReturnExpression(expression: PsiElement): KtType? {
        val returnParent = expression.getReturnExpressionWithThisType() ?: return null
        val targetSymbol = with(analysisSession) { returnParent.getReturnTargetSymbol() } ?: return null
        return targetSymbol.returnType
    }

    private fun PsiElement.getReturnExpressionWithThisType(): KtReturnExpression? =
        unwrapQualified { returnExpr, target -> returnExpr.returnedExpression == target }

    private fun getExpressionTypeByIfOrBooleanCondition(expression: PsiElement): KtType? = when {
        expression.isWhileLoopCondition() || expression.isIfCondition() -> with(analysisSession) { builtinTypes.BOOLEAN }
        else -> null
    }

    private fun getExpectedTypeByVariableAssignment(expression: PsiElement): KtType? {
        // Given: `x = expression`
        // Expected type of `expression` is type of `x`
        val assignmentExpression =
            expression.unwrapQualified<KtBinaryExpression> { binaryExpr, expr -> binaryExpr.right == expr && binaryExpr.operationToken == KtTokens.EQ }
                ?: return null
        val variableExpression = assignmentExpression.left as? KtNameReferenceExpression ?: return null
        return getKtExpressionType(variableExpression)
    }

    private fun getExpectedTypeByPropertyDeclaration(expression: PsiElement): KtType? {
        // Given: `val x: T = expression`
        // Expected type of `expression` is `T`
        val property = expression.unwrapQualified<KtProperty> { property, expr -> property.initializer == expr } ?: return null
        return getReturnTypeForKtDeclaration(property)
    }

    private fun getExpectedTypeByFunctionExpressionBody(expression: PsiElement): KtType? {
        // Given: `fun f(): T = expression`
        // Expected type of `expression` is `T`
        val function = expression.unwrapQualified<KtFunction> { function, expr -> function.bodyExpression == expr } ?: return null
        if (function.bodyBlockExpression != null) {
            // Given `fun f(...): R { blockExpression }`, `{ blockExpression }` is mapped to the enclosing anonymous function,
            // which may raise an exception if we attempt to retrieve, e.g., callable declaration from it.
            return null
        }
        return getReturnTypeForKtDeclaration(function)
    }

    private fun PsiElement.isWhileLoopCondition() =
        unwrapQualified<KtWhileExpressionBase> { whileExpr, cond -> whileExpr.condition == cond } != null

    private fun PsiElement.isIfCondition() =
        unwrapQualified<KtIfExpression> { ifExpr, cond -> ifExpr.condition == cond } != null

    override fun isDefinitelyNull(expression: KtExpression): Boolean =
        getDefiniteNullability(expression) == DefiniteNullability.DEFINITELY_NULL

    override fun isDefinitelyNotNull(expression: KtExpression): Boolean =
        getDefiniteNullability(expression) == DefiniteNullability.DEFINITELY_NOT_NULL

    private fun getDefiniteNullability(expression: KtExpression): DefiniteNullability {
        fun FirExpression.isNotNullable() = with(analysisSession.useSiteSession.typeContext) {
            !typeRef.coneType.isNullableType()
        }

        when (val fir = expression.getOrBuildFir(analysisSession.firResolveSession)) {
            is FirSmartCastExpression -> if (fir.isStable) {
                if (fir.smartcastTypeWithoutNullableNothing != null) {
                    return DefiniteNullability.DEFINITELY_NULL
                } else if (fir.isNotNullable()) {
                    return DefiniteNullability.DEFINITELY_NOT_NULL
                }
            }
            is FirExpression -> if (fir.isNotNullable()) {
                return DefiniteNullability.DEFINITELY_NOT_NULL
            }
        }

        return DefiniteNullability.UNKNOWN
    }
}

private data class KtCallWithArgument(val call: KtCallExpression, val argument: KtExpression)

private inline fun <reified R : Any> PsiElement.unwrapQualified(check: (R, PsiElement) -> Boolean): R? {
    val parent = nonContainerParent
    return when {
        parent is R && check(parent, this) -> parent
        parent is KtQualifiedExpression && parent.selectorExpression == this -> {
            val grandParent = parent.nonContainerParent
            when {
                grandParent is R && check(grandParent, parent) -> grandParent
                else -> null
            }
        }
        else -> null
    }
}

private val PsiElement.nonContainerParent: PsiElement?
    get() = when (val parent = parent) {
        is KtContainerNode -> parent.nonContainerParent
        is KtLabeledExpression -> parent.nonContainerParent
        else -> parent
    }

private enum class DefiniteNullability { DEFINITELY_NULL, DEFINITELY_NOT_NULL, UNKNOWN }
