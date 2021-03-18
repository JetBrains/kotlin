/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtExpressionTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypedSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtErrorType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

internal class KtFirExpressionTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtExpressionTypeProvider(), KtFirAnalysisSessionComponent {
    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType = withValidityAssertion {
        val firDeclaration = declaration.getOrBuildFirOfType<FirCallableDeclaration<*>>(firResolveState)
        firDeclaration.returnTypeRef.coneType.asKtType()
    }

    override fun getKtExpressionType(expression: KtExpression): KtType = withValidityAssertion {
        when (val fir = expression.getOrBuildFir(firResolveState)) {
            is FirExpression -> fir.typeRef.coneType.asKtType()
            is FirNamedReference -> fir.getReferencedElementType().asKtType()
            is FirStatement -> with(analysisSession) { builtinTypes.UNIT }
            else -> error("Unexpected ${fir::class}")
        }
    }

    private fun FirNamedReference.getReferencedElementType(): ConeKotlinType {
        val symbols = when (this) {
            is FirResolvedNamedReference -> listOf(resolvedSymbol)
            is FirErrorNamedReference -> FirReferenceResolveHelper.getFirSymbolsByErrorNamedReference(this)
            else -> error("Unexpected ${this::class}")
        }
        val firCallableDeclaration = symbols.singleOrNull()?.fir as? FirCallableDeclaration<*>
        return firCallableDeclaration?.returnTypeRef?.coneType
            ?: ConeClassErrorType(ConeUnresolvedNameError(name))
    }

    override fun getExpectedType(expression: PsiElement): KtType? {
        val expectedType = getExpectedTypeByReturnExpression(expression)
            ?: getExpressionTypeByIfOrBooleanCondition(expression)
            ?: getExpectedTypeOfFunctionParameter(expression)
            ?: getExpectedTypeOfInfixFunctionParameter(expression)
            ?: getExpectedTypeByVariableAssignment(expression)
            ?: getExpectedTypeByPropertyDeclaration(expression)
            ?: getExpectedTypeByFunctionExpressionBody(expression)
        return expectedType.takeIf { it !is KtErrorType }
    }

    private fun getExpectedTypeOfFunctionParameter(expression: PsiElement): KtType? {
        val (ktCallExpression, ktArgument) = expression.getFunctionCallAsWithThisAsParameter() ?: return null
        val firCall = ktCallExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveState) ?: return null
        val arguments = firCall.argumentMapping ?: return null
        val firParameterForExpression = arguments.entries.firstOrNull { (arg, _) -> arg.psi == ktArgument }?.value ?: return null
        return firParameterForExpression.returnTypeRef.coneType.asKtType()
    }

    private fun PsiElement.getFunctionCallAsWithThisAsParameter(): KtCallWithArgument? {
        val valueArgument = unwrapQualified<KtValueArgument> { valueArg, expr -> valueArg.getArgumentExpression() == expr } ?: return null
        val argumentsList = valueArgument.parent as? KtValueArgumentList ?: return null
        val callExpression = argumentsList.parent as? KtCallExpression ?: return null
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
        return (targetSymbol as? KtTypedSymbol)?.annotatedType?.type
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
        return getReturnTypeForKtDeclaration(function)
    }

    private fun PsiElement.isWhileLoopCondition() =
        unwrapQualified<KtWhileExpressionBase> { whileExpr, cond -> whileExpr.condition == cond } != null

    private fun PsiElement.isIfCondition() =
        unwrapQualified<KtIfExpression> { ifExpr, cond -> ifExpr.condition == cond } != null
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