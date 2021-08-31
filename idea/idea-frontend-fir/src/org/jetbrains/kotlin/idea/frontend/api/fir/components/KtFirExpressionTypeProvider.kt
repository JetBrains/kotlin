/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.constructFunctionalType
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.components.KtExpressionTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.getReferencedElementType
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.unwrap
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassErrorType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

internal class KtFirExpressionTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtExpressionTypeProvider(), KtFirAnalysisSessionComponent {

    override fun getKtExpressionType(expression: KtExpression): KtType? = withValidityAssertion {
        when (val fir = expression.unwrap().getOrBuildFir(firResolveState)) {
            is FirExpression -> fir.typeRef.coneType.asKtType()
            is FirNamedReference -> fir.getReferencedElementType(firResolveState).asKtType()
            is FirStatement -> with(analysisSession) { builtinTypes.UNIT }
            is FirTypeRef, is FirImport, is FirPackageDirective, is FirLabel -> null
            else -> error("Unexpected ${fir?.let { it::class }} for ${expression::class} with text `${expression.text}`")
        }
    }

    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType = withValidityAssertion {
        val firDeclaration = declaration.getOrBuildFirOfType<FirCallableDeclaration>(firResolveState)
        firDeclaration.returnTypeRef.coneType.asKtType()
    }

    override fun getFunctionalTypeForKtFunction(declaration: KtFunction): KtType = withValidityAssertion {
        val firFunction = declaration.getOrBuildFirOfType<FirFunction>(firResolveState)
        firFunction.constructFunctionalType(firFunction.isSuspend).asKtType()
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
        return expectedType.takeIf { it !is KtClassErrorType }
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
        val firCall = ktCallExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return null

        val callee = (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        if (callee?.fir?.origin == FirDeclarationOrigin.SamConstructor) {
            return (callee.fir as FirSimpleFunction).returnTypeRef.coneType.asKtType()
        }

        val arguments = firCall.argumentMapping ?: return null
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
        return firParameterForExpression.returnTypeRef.coneType.asKtType()
    }

    private fun PsiElement.getFunctionCallAsWithThisAsParameter(): KtCallWithArgument? {
        val valueArgument = unwrapQualified<KtValueArgument> { valueArg, expr -> valueArg.getArgumentExpression() == expr } ?: return null
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
        val firCall = infixCallExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return null

        // There is only one parameter for infix functions; get its type
        val arguments = firCall.argumentMapping ?: return null
        val firParameterForExpression = arguments.values.singleOrNull() ?: return null
        return firParameterForExpression.returnTypeRef.coneType.asKtType()
    }

    private fun getExpectedTypeByReturnExpression(expression: PsiElement): KtType? {
        val returnParent = expression.getReturnExpressionWithThisType() ?: return null
        val targetSymbol = with(analysisSession) { returnParent.getReturnTargetSymbol() } ?: return null
        return targetSymbol.annotatedType.type
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

    private fun getDefiniteNullability(expression: KtExpression): DefiniteNullability = withValidityAssertion {
        fun FirExpression.isNotNullable() = with(analysisSession.rootModuleSession.typeContext) {
            !typeRef.coneType.isNullableType()
        }

        when (val fir = expression.getOrBuildFir(analysisSession.firResolveState)) {
            is FirExpressionWithSmartcastToNull -> if (fir.isStable) {
                return DefiniteNullability.DEFINITELY_NULL
            }
            is FirExpressionWithSmartcast -> if (fir.isStable && fir.isNotNullable()) {
                return DefiniteNullability.DEFINITELY_NOT_NULL
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
        is KtContainerNode -> parent.parent
        else -> parent
    }

private enum class DefiniteNullability { DEFINITELY_NULL, DEFINITELY_NOT_NULL, UNKNOWN }
