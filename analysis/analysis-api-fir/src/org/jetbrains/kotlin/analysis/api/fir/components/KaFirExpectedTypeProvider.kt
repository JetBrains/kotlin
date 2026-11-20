/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.unwrapSafeCall
import org.jetbrains.kotlin.analysis.api.fir.utils.coneType
import org.jetbrains.kotlin.analysis.api.fir.utils.unwrap
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol
import org.jetbrains.kotlin.analysis.api.symbols.directlyOverriddenSymbols
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.createInheritanceTypeSubstitutor
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

context(expressionTypeProvider: KaFirExpressionTypeProvider)
internal fun computeExpectedType(element: PsiElement): KaType? {
    val unwrapped = element.unwrap()
    val expectedType = getExpectedTypeByReturnExpression(unwrapped)
        ?: getExpectedTypeByIfOrBooleanCondition(unwrapped)
        ?: getExpectedTypeByTypeCast(unwrapped)
        ?: getExpectedTypeOfFunctionParameter(unwrapped)
        ?: getExpectedTypeOfIndexingParameter(unwrapped)
        ?: getExpectedTypeOfInfixFunctionParameter(unwrapped)
        ?: getExpectedTypeOfCollectionLiteralElement(unwrapped)
        ?: getExpectedTypeOfArrayOfCallElement(unwrapped)
        ?: getExpectedTypeByVariableAssignment(unwrapped)
        ?: getExpectedTypeByPropertyDeclaration(unwrapped)
        ?: getExpectedTypeByCallableExpressionBody(unwrapped)
        ?: getExpectedTypeOfLastStatementInBlock(unwrapped)
        ?: getExpectedTypeByIfExpression(unwrapped)
        ?: getExpectedTypeOfWhenEntryExpression(unwrapped)
        ?: getExpectedTypeByTryExpression(unwrapped)
        ?: getExpectedTypeOfElvisOperand(unwrapped)
        ?: getExpectedTypeByWhenEntryValue(unwrapped)
        ?: getExpectedTypeByDelegatedSuperType(unwrapped)
        ?: getExpectedTypeOfParameterDefaultValue(unwrapped)
        ?: getExpectedTypeByThrowExpression(unwrapped)
    return expectedType
}

/**
 * Returns the expected type of expression nested inside a collection literal,
 * e.g., the `FOO` in `@Anno(arg = [ FOO ])` where `arg` has an array-like type.
 *
 * The expected type of the nested expression is the element type of the array
 * type that the collection literal itself is expected to produce.
 */
context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfCollectionLiteralElement(expression: PsiElement): KaType? {
    val collectionLiteral = expression.unwrapQualified<KtCollectionLiteralExpression> { collectionLiteral, currentExpression ->
        currentExpression in collectionLiteral.getInnerExpressions()
    } ?: return null

    val collectionLiteralType = expressionTypeProvider.expectedType(collectionLiteral) ?: return null
    return with(analysisSession) { collectionLiteralType.arrayElementType }
}

/**
 * Returns the expected type of an expression nested inside an `arrayOf(...)`-like
 * call used as an annotation argument, e.g., the `FOO` in `@Anno(arg = arrayOf(FOO))`
 * where `arg` has an array-like type.
 *
 * In annotation arguments the frontend rewrites `arrayOf(...)` (as well as the
 * primitive `intArrayOf(...)`, `doubleArrayOf(...)`, etc.) into a [FirCollectionLiteral],
 * which has neither a callee nor value parameters. Hence [getExpectedTypeOfFunctionParameter]
 * cannot derive the element's expected type, and we fall back to treating the call
 * like a collection literal: the expected type of a nested element is the element type
 * of the array type that the call itself is expected to produce.
 *
 * Note that for `arrayOf(...)` calls outside of annotations the frontend keeps the
 * regular call, so the [FirCollectionLiteral] check below filters those out (they are
 * already handled by [getExpectedTypeOfFunctionParameter]).
 */
context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfArrayOfCallElement(expression: PsiElement): KaType? {
    val call = expression.getFunctionCallAsWithThisAsParameter()?.call ?: return null
    if (call.getOrBuildFir(resolutionFacade)?.unwrapSafeCall() !is FirCollectionLiteral) return null

    val arrayType = expressionTypeProvider.expectedType(call) ?: return null
    return with(analysisSession) { arrayType.arrayElementType }
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByDelegatedSuperType(expression: PsiElement): KaType? {
    val entry =
        expression.unwrapQualified<KtDelegatedSuperTypeEntry> { delegated, expr -> delegated.delegateExpression == expr } ?: return null
    return with(analysisSession) { entry.typeReference?.type }
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfParameterDefaultValue(expression: PsiElement): KaType? {
    val parameter = expression.unwrapQualified<KtParameter> { param, expr -> param.defaultValue == expr }
    return parameter?.let { expressionTypeProvider.returnType(it) }
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByThrowExpression(expression: PsiElement): KaType? {
    expression.unwrapQualified<KtThrowExpression> { throwExpr, expr -> throwExpr.thrownExpression == expr } ?: return null
    return analysisSession.buildClassType(StandardClassIds.Throwable)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByTypeCast(expression: PsiElement): KaType? {
    val typeCastExpression =
        expression.unwrapQualified<KtBinaryExpressionWithTypeRHS> { castExpr, expr -> castExpr.left == expr } ?: return null
    return expressionTypeProvider.expressionType(typeCastExpression)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfFunctionParameter(expression: PsiElement): KaType? {
    (val ktCallElement = call, val argumentExpression = argument) = expression.getFunctionCallAsWithThisAsParameter() ?: return null
    val firCall = ktCallElement.getOrBuildFir(resolutionFacade)?.unwrapSafeCall() as? FirCall ?: return null

    val callee = (firCall.toReference(resolutionFacade.useSiteFirSession) as? FirResolvedNamedReference)?.resolvedSymbol
    if (callee?.fir?.origin == FirDeclarationOrigin.SamConstructor) {
        val substitutor = (firCall as? FirQualifiedAccessExpression)
            ?.createConeSubstitutorFromTypeArguments(rootModuleSession, discardErrorTypes = true)
            ?: ConeSubstitutor.Empty
        return substitutor.substituteOrSelf((callee.fir as FirNamedFunction).returnTypeRef.coneType).asKaType()
    }

    val argumentsToParameters = firCall.argumentsToSubstitutedValueParameters(substituteWithErrorTypes = false) ?: return null
    val [substitutedType, shouldUnwrapVararg] =
        argumentsToParameters.entries.firstNotNullOfOrNull { [arg, parameter] ->
            val substitutedParameterType = parameter.substitutedType
            when {
                arg is FirVarargArgumentsExpression -> arg.arguments.firstNotNullOfOrNull { varargArgument ->
                    when {
                        varargArgument is FirSpreadArgumentExpression && varargArgument.psi == argumentExpression.parent -> substitutedParameterType to false
                        varargArgument.psi == argumentExpression.unwrap() -> substitutedParameterType to true
                        else -> null
                    }
                }
                arg.psi == argumentExpression.unwrap() -> substitutedParameterType to false
                else -> null
            }
        } ?: return null
    return if (shouldUnwrapVararg)
        substitutedType.varargElementType().asKaType()
    else
        substitutedType.asKaType()
}

/**
 * The expected type of the indexing parameter in array access, for example, in the following code:
 * ```
 * val map = mapOf<Int, String>()
 * map[k] = v
 * ```
 * `k` is indexing parameter and its expected type is `Int`.
 */
context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfIndexingParameter(expression: PsiElement): KaType? {
    val arrayAccessExpression = expression.unwrapQualified<KtArrayAccessExpression> { arrayAccessExpression, currentExpression ->
        currentExpression in arrayAccessExpression.indexExpressions
    } ?: return null
    val firCall = arrayAccessExpression.getOrBuildFirSafe<FirFunctionCall>(resolutionFacade) ?: return null
    val firArgument = firCall.argumentList.arguments.firstOrNull { it.psi == expression } ?: return null

    val argumentsToParameters = firCall.argumentsToSubstitutedValueParameters(substituteWithErrorTypes = false) ?: return null
    return argumentsToParameters[firArgument]?.substitutedType?.asKaType()
}

private fun PsiElement.getFunctionCallAsWithThisAsParameter(): KtCallWithArgument? {
    val valueArgument = unwrapQualified<KtValueArgument> { valueArg, expr ->
        // If `valueArg` is [KtLambdaArgument], its [getArgumentExpression] could be labeled expression (e.g., l@{ ... }).
        // That is not exactly `expr`, which would be [KtLambdaExpression].
        // So, we need to [unwrap] here.
        valueArg.getArgumentExpression()?.unwrap() == expr
    } ?: return null
    val callExpression =
        (valueArgument.parent as? KtValueArgumentList)?.parent as? KtCallElement
            ?: valueArgument.parent as? KtCallElement // KtLambdaArgument
            ?: return null
    val argumentExpression = valueArgument.getArgumentExpression() ?: return null
    return KtCallWithArgument(callExpression, argumentExpression)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfInfixFunctionParameter(expression: PsiElement): KaType? {
    val infixCallExpression =
        expression.unwrapQualified<KtBinaryExpression> { binaryExpr, expr -> binaryExpr.right == expr } ?: return null
    val firCall = infixCallExpression.getOrBuildFirSafe<FirFunctionCall>(resolutionFacade) ?: return null

    // There is only one parameter for infix functions; get its type
    val argumentsToParameters = firCall.argumentsToSubstitutedValueParameters(substituteWithErrorTypes = false) ?: return null
    return argumentsToParameters.values.singleOrNull()?.substitutedType?.asKaType()
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByReturnExpression(expression: PsiElement): KaType? {
    val returnParent = expression.getReturnExpressionWithThisType() ?: return null
    val targetSymbol = with(analysisSession) { returnParent.resolveSymbol() } ?: return null
    return targetSymbol.returnType
}

private fun PsiElement.getReturnExpressionWithThisType(): KtReturnExpression? =
    unwrapQualified { returnExpr, target -> returnExpr.returnedExpression == target }

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByIfOrBooleanCondition(expression: PsiElement): KaType? = when {
    expression.isWhileLoopCondition() || expression.isIfCondition() -> with(analysisSession) { builtinTypes.boolean }
    else -> null
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByVariableAssignment(expression: PsiElement): KaType? {
    // Given: `x = expression` or `foo.x = expression`
    // Expected type of `expression` is type of `x`
    val assignmentExpression =
        expression.unwrapQualified<KtBinaryExpression> { binaryExpr, expr -> binaryExpr.right == expr && binaryExpr.operationToken == KtTokens.EQ }
            ?: return null
    val leftExpression = assignmentExpression.left ?: return null
    return getKtExpressionNonErrorType(leftExpression)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByPropertyDeclaration(expression: PsiElement): KaType? {
    // Given: `val x: T = expression`
    // Expected type of `expression` is `T`
    val property = expression.unwrapQualified<KtProperty> { property, expr -> property.initializer == expr } ?: return null

    if (property.typeReference != null) {
        return expressionTypeProvider.returnType(property).nonErrorTypeOrNull()
    }

    if (property.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
        return with(analysisSession) { property.symbol.returnTypeFromOverride() }
    }

    return null
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByCallableExpressionBody(expression: PsiElement): KaType? {
    // Given: `fun f(): T = expression` or `val prop: T get() = expression`
    // Expected type of `expression` is `T`
    val declaration = expression.unwrapQualified<KtDeclarationWithBody> { declaration, expr ->
        (declaration is KtFunction || (declaration as? KtPropertyAccessor)?.isGetter == true) &&
                declaration.bodyExpression == expr
    } ?: return null

    if (declaration.bodyBlockExpression != null) {
        // Given `fun f(...): R { blockExpression }`, `{ blockExpression }` is mapped to the enclosing anonymous function,
        // which may raise an exception if we attempt to retrieve, e.g., callable declaration from it.
        return null
    }

    val hasExplicitReturnType = when (declaration) {
        is KtFunction -> declaration.typeReference != null
        is KtPropertyAccessor -> declaration.typeReference != null || declaration.property.typeReference != null
        else -> false
    }

    if (hasExplicitReturnType) {
        return expressionTypeProvider.returnType(declaration as KtDeclarationWithReturnType).nonErrorTypeOrNull()
    }

    val hasOverrideModifier = when (declaration) {
        is KtFunction -> declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        is KtPropertyAccessor -> declaration.property.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        else -> false
    }

    if (hasOverrideModifier) {
        return with(analysisSession) {
            val callableSymbol = when (declaration) {
                is KtFunction -> declaration.symbol
                is KtPropertyAccessor -> declaration.property.symbol
                else -> null
            } as? KaCallableSymbol ?: return null

            callableSymbol.returnTypeFromOverride()
        }
    }

    return null
}

context(_: KaFirSession)
private fun KaCallableSymbol.returnTypeFromOverride(): KaType? {
    val overriddenSymbol = directlyOverriddenSymbols.firstOrNull() ?: return null
    val overriddenReturnType = overriddenSymbol.returnType.nonErrorTypeOrNull() ?: return null

    val subClass = containingSymbol as? KaClassSymbol ?: return overriddenReturnType
    val superClass = overriddenSymbol.containingSymbol as? KaClassSymbol ?: return overriddenReturnType

    val substitutor = createInheritanceTypeSubstitutor(subClass, superClass) ?: return overriddenReturnType
    return substitutor.substitute(overriddenReturnType).nonErrorTypeOrNull() ?: overriddenReturnType
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfLastStatementInBlock(expression: PsiElement): KaType? {
    val blockExpression = expression.unwrapQualified<KtBlockExpression> { blockExpression, currentExpression ->
        currentExpression == blockExpression.statements.lastOrNull()
    } ?: return null

    val functionLiteral = blockExpression.parent as? KtFunctionLiteral
    return if (functionLiteral != null) {
        val functionType = expressionTypeProvider.expectedType(functionLiteral) as? KaFunctionType
        functionType?.returnType
    } else {
        expressionTypeProvider.expectedType(blockExpression)
    }
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByIfExpression(expression: PsiElement): KaType? {
    val ifExpression = expression.unwrapQualified<KtIfExpression> { ifExpression, currentExpression ->
        currentExpression == ifExpression.then || currentExpression == ifExpression.`else`
    } ?: return null
    expressionTypeProvider.expectedType(ifExpression)?.let { return it }

    // if `KtIfExpression` doesn't have an expected type, get the expected type of the current branch from the other branch
    val otherBranch = (if (expression == ifExpression.then) ifExpression.`else` else ifExpression.then) ?: return null
    return getKtExpressionNonErrorType(otherBranch)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfWhenEntryExpression(expression: PsiElement): KaType? {
    val whenEntry = expression.unwrapQualified<KtWhenEntry> { whenEntry, currentExpression ->
        currentExpression == whenEntry.expression
    } ?: return null
    val whenExpression = whenEntry.parent as? KtWhenExpression ?: return null
    expressionTypeProvider.expectedType(whenExpression)?.let { return it }

    // if `KtWhenExpression` doesn't have an expected type, get the expected type of the current entry from the other entries
    val entryExpressions = whenExpression.entries
        .mapNotNull { it.expression }
        .filter { entryExpression -> entryExpression != expression }
    val types = entryExpressions.mapNotNull { getKtExpressionNonErrorType(it) }
    return analysisSession.firSession.typeContext.intersectTypesOrNull(types.map { it.coneType })?.asKaType()
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByTryExpression(expression: PsiElement): KaType? {
    val tryExpression = expression.unwrapQualified<KtTryExpression> { tryExpression, currentExpression ->
        currentExpression == tryExpression.tryBlock
    } ?: return null
    return expressionTypeProvider.expectedType(tryExpression)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeOfElvisOperand(expression: PsiElement): KaType? {
    val binaryExpression = expression.unwrapQualified<KtBinaryExpression> { binaryExpression, operand ->
        binaryExpression.operationToken == KtTokens.ELVIS && (operand == binaryExpression.left || operand == binaryExpression.right)
    } ?: return null
    if (expression !is KtExpression) return null
    val type = expressionTypeProvider.expectedType(binaryExpression) ?: getElvisOperandExpectedTypeByOtherOperand(expression, binaryExpression)

    return type?.applyIf(expression == binaryExpression.left) { withNullability(nullable = true) }
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getElvisOperandExpectedTypeByOtherOperand(operand: KtExpression, elvisExpression: KtBinaryExpression): KaType? {
    val leftOperand = elvisExpression.left ?: return null
    val rightOperand = elvisExpression.right ?: return null
    return if (operand == leftOperand) {
        getKtExpressionNonErrorType(rightOperand)
    } else {
        getKtExpressionNonErrorType(leftOperand)?.withNullability(nullable = false)
    }
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun KaType.withNullability(nullable: Boolean): KaType =
    coneType.withNullability(nullable, analysisSession.firSession.typeContext).asKaType()

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getExpectedTypeByWhenEntryValue(expression: PsiElement): KaType? {
    val condition = expression.parent as? KtWhenConditionWithExpression ?: return null
    val whenExpression = (condition.parent as? KtWhenEntry)?.parent as? KtWhenExpression ?: return null
    val subject = whenExpression.subjectExpression ?: return with(analysisSession) { builtinTypes.boolean }
    return getKtExpressionNonErrorType(subject)
}

context(expressionTypeProvider: KaFirExpressionTypeProvider)
private fun getKtExpressionNonErrorType(expression: KtExpression): KaType? =
    expressionTypeProvider.expressionType(expression)?.nonErrorTypeOrNull()

private fun KaType.nonErrorTypeOrNull(): KaType? = takeUnless { it is KaErrorType }

private fun PsiElement.isWhileLoopCondition() =
    unwrapQualified<KtWhileExpressionBase> { whileExpr, cond -> whileExpr.condition == cond } != null

private fun PsiElement.isIfCondition() =
    unwrapQualified<KtIfExpression> { ifExpr, cond -> ifExpr.condition == cond } != null

private data class KtCallWithArgument(val call: KtCallElement, val argument: KtExpression)

private inline fun <reified R : Any> PsiElement.unwrapQualified(check: (R, PsiElement) -> Boolean): R? {
    return when (val parent = nonContainerParent) {
        is R if check(parent, this) -> parent
        is KtQualifiedExpression if parent.selectorExpression == this -> {
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
