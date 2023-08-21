/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.KtExpressionTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.unwrap
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtErrorType
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.constructFunctionType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KtFirExpressionTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtExpressionTypeProvider(), KtFirAnalysisSessionComponent {

    override fun getKtExpressionType(expression: KtExpression): KtType? {
        // There are various cases where we have no corresponding fir due to invalid code
        // Some examples:
        // ```
        // when {
        //   true, false -> {}
        // }
        // ```
        // `false` does not have a corresponding elements on the FIR side and hence the containing `FirWhenBranch` is returned.
        // ```
        // @Volatile
        // private var
        // ```
        // Volatile does not have corresponding element, so `FirFileImpl` is returned
        val fir = expression.unwrap().getOrBuildFir(firResolveSession) ?: return null
        return try {
            getKtExpressionType(expression, fir)
        } catch (e: Exception) {
            rethrowExceptionWithDetails("Exception during resolving ${expression::class.simpleName}", e) {
                withPsiEntry("expression", expression)
                withFirEntry("fir", fir)
            }
        }
    }

    private fun getKtExpressionType(expression: KtExpression, fir: FirElement): KtType? = when (fir) {
        is FirFunctionCall -> getReturnTypeForArrayStyleAssignmentTarget(expression, fir) ?: fir.resolvedType.asKtType()
        is FirPropertyAccessExpression -> {
            // For unresolved `super`, we manually create an intersection type so that IDE features like completion can work correctly.
            val containingClass = (fir.dispatchReceiver as? FirThisReceiverExpression)?.calleeReference?.boundSymbol as? FirClassSymbol<*>

            if (fir.calleeReference is FirSuperReference && fir.coneTypeOrNull is ConeErrorType && containingClass != null) {
                val superTypes = containingClass.resolvedSuperTypes
                when (superTypes.size) {
                    0 -> analysisSession.builtinTypes.ANY
                    1 -> superTypes.single().asKtType()
                    else -> ConeIntersectionType(superTypes).asKtType()
                }
            } else {
                fir.resolvedType.asKtType()
            }
        }
        is FirVariableAssignment -> {
            if (fir.lValue.source?.psi == expression) {
                fir.lValue.resolvedType.asKtType()
            } else if (expression is KtUnaryExpression && expression.operationToken in KtTokens.INCREMENT_AND_DECREMENT) {
                fir.rValue.resolvedType.asKtType()
            } else {
                analysisSession.builtinTypes.UNIT
            }
        }
        is FirExpression -> fir.resolvedType.asKtType()
        is FirNamedReference -> fir.getCorrespondingTypeIfPossible()?.asKtType()
        is FirStatement -> with(analysisSession) { builtinTypes.UNIT }
        is FirTypeRef, is FirImport, is FirPackageDirective, is FirLabel, is FirTypeParameterRef -> null

        // `listOf<_>(1)` where `expression` is `_`
        is FirPlaceholderProjection -> null
        else -> null
    }

    /**
     * It only makes sense to provide type for the references which reference some actual properties/variables.
     *
     * In cases when the name reference references a function (a REAL function, not a functional type variable), it does not
     * make sense to provide any type for it.
     *
     * ---
     *
     * Why not just always provide null for name references? In such case, the following case would be a problem:
     *
     * ```kt
     * fun usage(action: String.(Int) -> String) {
     *   "hello".<expr>action</expr>(10)
     * }
     * ```
     *
     * The user might want to know the type of the `action` callback. If we always return null for the named references,
     * we won't be able to handle this request, and just return null. So the user will only be able to see the type
     * of the whole expression instead, and that is not what he wants.
     */
    private fun FirNamedReference.getCorrespondingTypeIfPossible(): ConeKotlinType? =
        findOuterPropertyAccessExpression()?.resolvedType

    /**
     * Finds an outer expression for [this] named reference in cases when it is a part of a property access.
     *
     * Otherwise, return null.
     */
    private fun FirNamedReference.findOuterPropertyAccessExpression(): FirExpression? {
        val referenceExpression = psi as? KtExpression ?: return null
        val outerExpression = referenceExpression.getOutermostParenthesizerOrThis().parent as? KtElement ?: return null

        return when (val outerFirElement = outerExpression.getOrBuildFir(firResolveSession)) {
            is FirVariableAssignment -> outerFirElement.lValue
            is FirPropertyAccessExpression -> outerFirElement
            is FirImplicitInvokeCall -> outerFirElement.explicitReceiver
            is FirSafeCallExpression -> {
                if (outerFirElement.selector is FirPropertyAccessExpression) outerFirElement else null
            }
            else -> null
        }
    }

    private fun getReturnTypeForArrayStyleAssignmentTarget(
        expression: KtExpression,
        fir: FirFunctionCall,
    ): KtType? {
        if (fir.calleeReference !is FirResolvedNamedReference) return null
        if (expression !is KtArrayAccessExpression) return null
        val assignment = expression.parent as? KtBinaryExpression ?: return null
        if (assignment.operationToken !in KtTokens.ALL_ASSIGNMENTS) return null
        if (assignment.left != expression) return null
        val setTargetParameterType = fir.argumentsToSubstitutedValueParameters()?.values?.last()?.substitutedType ?: return null
        return setTargetParameterType.asKtType()
    }

    private data class SubstitutedValueParameter(val parameter: FirValueParameter, val substitutedType: ConeKotlinType)

    private fun FirCall.argumentsToSubstitutedValueParameters(
        substituteWithErrorTypes: Boolean = true,
    ): LinkedHashMap<FirExpression, SubstitutedValueParameter>? {
        val substitutor = (this as? FirQualifiedAccessExpression)
            ?.createConeSubstitutorFromTypeArguments(discardErrorTypes = !substituteWithErrorTypes)
            ?: ConeSubstitutor.Empty

        return resolvedArgumentMapping?.mapValuesTo(LinkedHashMap()) { (_, parameter) ->
            SubstitutedValueParameter(parameter, substitutor.substituteOrSelf(parameter.returnTypeRef.coneType))
        }
    }

    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType {
        val firDeclaration = if (declaration is KtParameter && declaration.ownerFunction == null) {
            declaration.getOrBuildFir(firResolveSession)
        } else {
            declaration.resolveToFirSymbol(firResolveSession, FirResolvePhase.TYPES).fir
        }
        return when (firDeclaration) {
            is FirCallableDeclaration -> firDeclaration.symbol.resolvedReturnType.asKtType()
            is FirFunctionTypeParameter -> firDeclaration.returnTypeRef.coneType.asKtType()
            else -> unexpectedElementError<FirElement>(firDeclaration)
        }
    }

    override fun getFunctionalTypeForKtFunction(declaration: KtFunction): KtType {
        val firFunction = declaration.resolveToFirSymbol(firResolveSession, FirResolvePhase.TYPES).fir as FirFunction
        firFunction.symbol.calculateReturnType()
        return firFunction.constructFunctionType(firFunction.specialFunctionTypeKind(firResolveSession.useSiteFirSession)).asKtType()
    }

    override fun getExpectedType(expression: PsiElement): KtType? {
        val unwrapped = expression.unwrap()
        val expectedType = getExpectedTypeByReturnExpression(unwrapped)
            ?: getExpressionTypeByIfOrBooleanCondition(unwrapped)
            ?: getExpectedTypeByTypeCast(unwrapped)
            ?: getExpectedTypeOfFunctionParameter(unwrapped)
            ?: getExpectedTypeOfIndexingParameter(unwrapped)
            ?: getExpectedTypeOfInfixFunctionParameter(unwrapped)
            ?: getExpectedTypeByVariableAssignment(unwrapped)
            ?: getExpectedTypeByPropertyDeclaration(unwrapped)
            ?: getExpectedTypeByFunctionExpressionBody(unwrapped)
            ?: getExpectedTypeOfLastStatementInBlock(unwrapped)
            ?: getExpectedTypeByIfExpression(unwrapped)
            ?: getExpectedTypeOfWhenEntryExpression(unwrapped)
            ?: getExpectedTypeByTryExpression(unwrapped)
            ?: getExpectedTypeOfElvisOperand(unwrapped)
            ?: getExpectedTypeByWhenEntryValue(unwrapped)
        return expectedType
    }

    private fun getExpectedTypeByTypeCast(expression: PsiElement): KtType? {
        val typeCastExpression =
            expression.unwrapQualified<KtBinaryExpressionWithTypeRHS> { castExpr, expr -> castExpr.left == expr } ?: return null
        return getKtExpressionType(typeCastExpression)
    }

    private fun getExpectedTypeOfFunctionParameter(expression: PsiElement): KtType? {
        val (ktCallElement, argumentExpression) = expression.getFunctionCallAsWithThisAsParameter() ?: return null
        val firCall = ktCallElement.getOrBuildFirSafe<FirCall>(firResolveSession) ?: return null

        val callee = (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        if (callee?.fir?.origin == FirDeclarationOrigin.SamConstructor) {
            return (callee.fir as FirSimpleFunction).returnTypeRef.coneType.asKtType()
        }

        val argumentsToParameters = firCall.argumentsToSubstitutedValueParameters(substituteWithErrorTypes = false) ?: return null
        val (firParameterForExpression, substitutedType) =
            argumentsToParameters.entries.firstOrNull { (arg, _) ->
                when (arg) {
                    // TODO: better to utilize. See `createArgumentMapping` in [KtFirCallResolver]
                    is FirLambdaArgumentExpression, is FirNamedArgumentExpression, is FirSpreadArgumentExpression ->
                        arg.psi == argumentExpression.parent
                    else ->
                        arg.psi == argumentExpression
                }
            }?.value ?: return null
        return if (firParameterForExpression.isVararg)
            substitutedType.varargElementType().asKtType()
        else
            substitutedType.asKtType()
    }

    /**
     * Expected type of the indexing parameter in array access, for example, in the following code:
     * ```
     * val map = mapOf<Int, String>()
     * map[k] = v
     * ```
     * `k` is indexing parameter and its expected type is `Int`.
     */
    private fun getExpectedTypeOfIndexingParameter(expression: PsiElement): KtType? {
        val arrayAccessExpression = expression.unwrapQualified<KtArrayAccessExpression> { arrayAccessExpression, currentExpression ->
            currentExpression in arrayAccessExpression.indexExpressions
        } ?: return null
        val firCall = arrayAccessExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveSession) ?: return null
        val firArgument = firCall.argumentList.arguments.firstOrNull { it.psi == expression } ?: return null

        val argumentsToParameters = firCall.argumentsToSubstitutedValueParameters(substituteWithErrorTypes = false) ?: return null
        return argumentsToParameters[firArgument]?.substitutedType?.asKtType()
    }

    private fun PsiElement.getFunctionCallAsWithThisAsParameter(): KtCallWithArgument? {
        val valueArgument = unwrapQualified<KtValueArgument> { valueArg, expr ->
            // If `valueArg` is [KtLambdaArgument], its [getArgumentExpression] could be labeled expression (e.g., l@{ ... }).
            // That is not exactly `expr`, which would be [KtLambdaExpression]. So, we need [unwrap] here.
            valueArg.getArgumentExpression()?.unwrap() == expr
        } ?: return null
        val callExpression =
            (valueArgument.parent as? KtValueArgumentList)?.parent as? KtCallElement
                ?: valueArgument.parent as? KtCallElement // KtLambdaArgument
                ?: return null
        val argumentExpression = valueArgument.getArgumentExpression() ?: return null
        return KtCallWithArgument(callExpression, argumentExpression)
    }

    private fun getExpectedTypeOfInfixFunctionParameter(expression: PsiElement): KtType? {
        val infixCallExpression =
            expression.unwrapQualified<KtBinaryExpression> { binaryExpr, expr -> binaryExpr.right == expr } ?: return null
        val firCall = infixCallExpression.getOrBuildFirSafe<FirFunctionCall>(firResolveSession) ?: return null

        // There is only one parameter for infix functions; get its type
        val argumentsToParameters = firCall.argumentsToSubstitutedValueParameters(substituteWithErrorTypes = false) ?: return null
        return argumentsToParameters.values.singleOrNull()?.substitutedType?.asKtType()
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
        return getKtExpressionNonErrorType(variableExpression)
    }

    private fun getExpectedTypeByPropertyDeclaration(expression: PsiElement): KtType? {
        // Given: `val x: T = expression`
        // Expected type of `expression` is `T`
        val property = expression.unwrapQualified<KtProperty> { property, expr -> property.initializer == expr } ?: return null
        return getReturnTypeForKtDeclaration(property).nonErrorTypeOrNull()
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
        return getReturnTypeForKtDeclaration(function).nonErrorTypeOrNull()
    }

    private fun getExpectedTypeOfLastStatementInBlock(expression: PsiElement): KtType? {
        val blockExpression = expression.unwrapQualified<KtBlockExpression> { blockExpression, currentExpression ->
            currentExpression == blockExpression.statements.lastOrNull()
        } ?: return null

        val functionLiteral = blockExpression.parent as? KtFunctionLiteral
        return if (functionLiteral != null) {
            val functionalType = getExpectedType(functionLiteral) as? KtFunctionalType
            functionalType?.returnType
        } else {
            getExpectedType(blockExpression)
        }
    }

    private fun getExpectedTypeByIfExpression(expression: PsiElement): KtType? {
        val ifExpression = expression.unwrapQualified<KtIfExpression> { ifExpression, currentExpression ->
            currentExpression == ifExpression.then || currentExpression == ifExpression.`else`
        } ?: return null
        getExpectedType(ifExpression)?.let { return it }

        // if `KtIfExpression` doesn't have an expected type, get the expected type of the current branch from the other branch
        val otherBranch = (if (expression == ifExpression.then) ifExpression.`else` else ifExpression.then) ?: return null
        return getKtExpressionNonErrorType(otherBranch)
    }

    private fun getExpectedTypeOfWhenEntryExpression(expression: PsiElement): KtType? {
        val whenEntry = expression.unwrapQualified<KtWhenEntry> { whenEntry, currentExpression ->
            currentExpression == whenEntry.expression
        } ?: return null
        val whenExpression = whenEntry.parent as? KtWhenExpression ?: return null
        getExpectedType(whenExpression)?.let { return it }

        // if `KtWhenExpression` doesn't have an expected type, get the expected type of the current entry from the other entries
        val entryExpressions = whenExpression.entries
            .mapNotNull { it.expression }
            .filter { entryExpression -> entryExpression != expression }
        val types = entryExpressions.mapNotNull { getKtExpressionNonErrorType(it) }
        return analysisSession.useSiteSession.typeContext.intersectTypesOrNull(types.map { it.coneType })?.asKtType()
    }

    private fun getExpectedTypeByTryExpression(expression: PsiElement): KtType? {
        val tryExpression = expression.unwrapQualified<KtTryExpression> { tryExpression, currentExpression ->
            currentExpression == tryExpression.tryBlock
        } ?: return null
        return getExpectedType(tryExpression)
    }

    private fun getExpectedTypeOfElvisOperand(expression: PsiElement): KtType? {
        val binaryExpression = expression.unwrapQualified<KtBinaryExpression> { binaryExpression, operand ->
            binaryExpression.operationToken == KtTokens.ELVIS && (operand == binaryExpression.left || operand == binaryExpression.right)
        } ?: return null
        if (expression !is KtExpression) return null
        val type = getExpectedType(binaryExpression) ?: getElvisOperandExpectedTypeByOtherOperand(expression, binaryExpression)

        return type?.applyIf(expression == binaryExpression.left) { withNullability(ConeNullability.NULLABLE) }
    }

    private fun getElvisOperandExpectedTypeByOtherOperand(operand: KtExpression, elvisExpression: KtBinaryExpression): KtType? {
        val leftOperand = elvisExpression.left ?: return null
        val rightOperand = elvisExpression.right ?: return null
        return if (operand == leftOperand) {
            getKtExpressionNonErrorType(rightOperand)
        } else {
            getKtExpressionNonErrorType(leftOperand)?.withNullability(ConeNullability.NOT_NULL)
        }
    }

    private fun KtType.withNullability(nullability: ConeNullability): KtType =
        coneType.withNullability(nullability, analysisSession.useSiteSession.typeContext).asKtType()

    private fun getExpectedTypeByWhenEntryValue(expression: PsiElement): KtType? {
        val condition = expression.parent as? KtWhenConditionWithExpression ?: return null
        val whenExpression = (condition.parent as? KtWhenEntry)?.parent as? KtWhenExpression ?: return null
        val subject = whenExpression.subjectExpression ?: return with(analysisSession) { builtinTypes.BOOLEAN }
        return getKtExpressionNonErrorType(subject)
    }

    private fun getKtExpressionNonErrorType(expression: KtExpression): KtType? =
        getKtExpressionType(expression)?.nonErrorTypeOrNull()

    private fun KtType.nonErrorTypeOrNull(): KtType? = takeUnless { it is KtErrorType }

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
            !resolvedType.isNullableType()
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

private data class KtCallWithArgument(val call: KtCallElement, val argument: KtExpression)

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
