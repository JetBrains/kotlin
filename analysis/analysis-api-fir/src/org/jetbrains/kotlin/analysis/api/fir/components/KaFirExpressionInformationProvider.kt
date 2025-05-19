/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaExpressionInformationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirExpressionInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaExpressionInformationProvider, KaFirSessionComponent {
    override val KtReturnExpression.targetSymbol: KaCallableSymbol?
        get() = withPsiValidityAssertion {
            val fir = getOrBuildFirSafe<FirReturnExpression>(resolutionFacade) ?: return null
            val firTargetSymbol = fir.target.labeledElement
            if (firTargetSymbol is FirErrorFunction) return null
            return firSymbolBuilder.callableBuilder.buildCallableSymbol(firTargetSymbol.symbol)
        }

    override fun KtWhenExpression.computeMissingCases(): List<WhenMissingCase> = withPsiValidityAssertion {
        val firWhenExpression = getOrBuildFirSafe<FirWhenExpression>(analysisSession.resolutionFacade) ?: return emptyList()
        return FirWhenExhaustivenessTransformer.computeAllMissingCases(
            analysisSession.resolutionFacade.useSiteFirSession,
            firWhenExpression
        )
    }

    override val KtExpression.isUsedAsExpression: Boolean
        get() = withPsiValidityAssertion { isUsed(this) }

    /**
     * [isUsed] and [doesParentUseChild] are defined in mutual recursion,
     * climbing up the syntax tree, passing control back and forth between the
     * two.
     *
     * Whether an expression is used is defined by the context in which it
     * appears.
     * E.g., a "statement" in a block is considered used if it is the
     * last expression in that block AND the block itself is used -- a
     * recursive call to `isUsed`, one level higher in the syntax tree.
     *
     * The methods are _conservative_, erring on the side of answering `true`.
     */
    private fun isUsed(psiElement: PsiElement): Boolean = when (psiElement) {
        /**
         * DECLARATIONS
         */
        // Inner PSI of KtLambdaExpressions. Used if the containing KtLambdaExpression is.
        is KtFunctionLiteral ->
            doesParentUseChild(psiElement.parent, psiElement)

        // KtNamedFunction includes `fun() { ... }` lambda syntax. No other
        // "named" functions can be expressions.
        is KtNamedFunction ->
            doesParentUseChild(psiElement.parent, psiElement)

        // No other declarations are considered expressions
        is KtDeclaration ->
            false

        /**
         * EXPRESSIONS
         */
        // A handful of expressions are never considered used:

        //  - Everything of type `Nothing`
        is KtThrowExpression ->
            false
        is KtReturnExpression ->
            false
        is KtBreakExpression ->
            false
        is KtContinueExpression ->
            false

        // - Loops
        is KtLoopExpression ->
            false

        // - The `this` in `constructor(x: Int) : this(x)`
        is KtConstructorDelegationReferenceExpression ->
            false

        // - Administrative node for EnumEntries. Never used as an expression.
        is KtEnumEntrySuperclassReferenceExpression ->
            false

        // - The "reference" in a constructor call. E.g. `C` in `C()`
        is KtConstructorCalleeExpression ->
            false

        // - Labels themselves: `@label` in return`@label` or `label@`while...
        is KtLabelReferenceExpression ->
            false

        // - The operation symbol itself in binary and unary operations: `!!`, `+`...
        is KtOperationReferenceExpression ->
            false

        // All other expressions are used if their parent expression uses them.
        else ->
            doesParentUseChild(psiElement.parent, psiElement)
    }

    private fun doesParentUseChild(parent: PsiElement, child: PsiElement): Boolean = when (parent) {
        /**
         * NON-EXPRESSION PARENTS
         */
        // KtValueArguments are a container for call-sites and use exactly the
        // argument expression they wrap.
        is KtValueArgument ->
            parent.getArgumentExpression() == child

        // In `foo(param = value)`, `param` is not used as an expression
        is KtValueArgumentName ->
            false

        is KtDelegatedSuperTypeEntry ->
            parent.delegateExpression == child

        // KtContainerNode are containers used in `KtIfExpressions`, and should be regarded
        // as parentheses for this analysis.
        is KtContainerNode ->
            // !!!!CAUTION!!!! Not `parentUse(parent.parent, _parent_)`
            // Here we assume the parent (e.g., If condition) statement
            // ignores the ContainerNode when accessing the child
            doesParentUseChild(parent.parent, child)

        // KtWhenEntry/WhenCondition are containers used in KtWhenExpressions and
        // should be regarded as parentheses.
        is KtWhenEntry ->
            (parent.expression == child && isUsed(parent.parent)) || child in parent.conditions

        is KtWhenCondition ->
            doesParentUseChild(parent.parent, parent)

        // Type parameters, return types and other annotations are all contained in KtUserType
        // and are never considered used as expressions
        is KtUserType ->
            false

        // Only top-level named declarations have KtFile/KtScript parents and are never considered used
        is KtFile ->
            false
        is KtScript ->
            false

        // Only class members have KtClassBody parents, and are never considered used
        is KtClassBody ->
            false

        // $_ and ${_} contexts use their inner expression
        is KtStringTemplateEntry ->
            parent.expression == child

        // Catch blocks are used if the parent-try uses the catch block
        is KtCatchClause ->
            doesParentUseChild(parent.parent, parent)

        // Finally, blocks are never used
        is KtFinallySection ->
            false

        is KtPropertyDelegate ->
            parent.expression == child

        is KtWhenEntryGuard ->
            parent.getExpression() == child

        !is KtExpression ->
            errorWithAttachment("Unhandled Non-KtExpression parent of KtExpression: ${parent::class}") {
                withPsiEntry("parent", parent)
            }
        /**
         * EXPRESSIONS
         */
        // Enum entries, type parameters, lambda expressions and script
        // initializers never use any child expressions.
        is KtEnumEntry ->
            false
        is KtTypeParameter ->
            false
        is KtLambdaExpression ->
            false
        is KtScriptInitializer ->
            false

        // The last expression of a block is considered used iff the block itself is used.
        is KtBlockExpression ->
            parent.statements.lastOrNull() == child && isUsed(parent)

        // Destructuring declarations use their initializer.
        is KtDestructuringDeclaration ->
            parent.initializer == child

        // Backing field declarations use their initializer.
        is KtBackingField ->
            parent.initializer == child

        // Property accessors can use their bodies if not blocks.
        is KtPropertyAccessor ->
            parent.bodyExpression == child && doesPropertyAccessorUseBody(parent, child)

        // Lambdas do not use their expression-blocks if they are inferred
        // to be of the Unit type
        is KtFunctionLiteral ->
            parent.bodyBlockExpression == child && !analysisSession.returnsUnit(parent)

        /** See [doesNamedFunctionUseBody] */
        is KtNamedFunction ->
            analysisSession.doesNamedFunctionUseBody(parent, child)

        // Function parameter declarations use their default value expressions.
        is KtParameter ->
            parent.defaultValue == child

        // Variable declarations use their initializer.
        is KtVariableDeclaration ->
            parent.initializer == child

        // Binary expressions always use both operands.
        is KtBinaryExpression ->
            parent.left == child || parent.right == child

        // Binary expressions with type RHS always use its operand.
        is KtBinaryExpressionWithTypeRHS ->
            parent.left == child

        // Is expressions always use their LHS.
        is KtIsExpression ->
            parent.leftHandSide == child

        // Unary expressions always use its operand.
        is KtUnaryExpression ->
            parent.baseExpression == child

        // Qualified expressions always use its receiver. The selector is
        // used iff the qualified expression is.
        is KtQualifiedExpression ->
            parent.receiverExpression == child || (parent.selectorExpression == child && isUsed(parent))

        // Array accesses use both receiver and index.
        is KtArrayAccessExpression ->
            child in parent.indexExpressions || parent.arrayExpression == child

        // Calls use only the callee directly -- arguments are wrapped in a
        // KtValueArgument container
        is KtCallExpression ->
            parent.calleeExpression == child && analysisSession.doesCallExpressionUseCallee(child)

        // Collection literals use each of its constituent expressions.
        is KtCollectionLiteralExpression ->
            child in parent.getInnerExpressions()

        // Annotations are regarded as parentheses. The annotation itself is never used.
        is KtAnnotatedExpression ->
            parent.baseExpression == child && isUsed(parent)

        /** See [doesDoubleColonUseLHS] */
        is KtDoubleColonExpression ->
            parent.lhs == child && doesDoubleColonUseLHS(child)

        // Parentheses are ignored for this analysis.
        is KtParenthesizedExpression ->
            doesParentUseChild(parent.parent, parent)

        // When expressions use the subject expression _unless_ the first branch in the
        // `when` is an `else`.
        is KtWhenExpression ->
            parent.subjectExpression == child && parent.entries.firstOrNull()?.isElse == false

        // Throw expressions use the expression thrown.
        is KtThrowExpression ->
            parent.thrownExpression == child

        // Body and catch blocks of try-catch expressions are used if the try-catch itself
        // is used.
        is KtTryExpression ->
            (parent.tryBlock == child || child in parent.catchClauses) && isUsed(parent)

        // If expressions always use their condition, and the branches are used if the
        // `if` itself is used as an expression.
        is KtIfExpression ->
            parent.condition == child ||
                    ((parent.then == child ||
                            parent.`else` == child) && isUsed(parent))

        // For expressions use their loop range expression.
        is KtForExpression ->
            parent.loopRange == child

        // While, DoWhile loops use their conditions, not their bodies
        is KtWhileExpressionBase ->
            parent.condition == child

        // Return expressions use the return value
        is KtReturnExpression ->
            parent.returnedExpression == child

        // Labels are regarded as parentheses for this analysis. The label itself is never used.
        is KtLabeledExpression ->
            parent.baseExpression == child && isUsed(parent)

        // No children.
        is KtConstantExpression ->
            false

        // no children of class and script initializers are used
        is KtAnonymousInitializer ->
            false

        // no child expressions of primary constructors.
        is KtPrimaryConstructor ->
            false // error?
        // no children of secondary constructs are used.
        is KtSecondaryConstructor ->
            false

        // KtClass, KtObjectDeclaration, KtTypeAlias has no expression children
        is KtClassLikeDeclaration ->
            false // has no expression children

        // Simple names do not have expression children
        // Labels, operations, references by name
        is KtSimpleNameExpression ->
            false

        // this/super in constructor delegations. No expression children
        is KtConstructorDelegationReferenceExpression ->
            false

        // Object Literal expressions use none of its children.
        is KtObjectLiteralExpression ->
            false

        // `break`, `continue`, `super` and `this` do not have children
        is KtBreakExpression ->
            false
        is KtContinueExpression ->
            false
        is KtSuperExpression ->
            false
        is KtThisExpression ->
            false

        // No direct expression children
        is KtStringTemplateExpression ->
            false

        else ->
            errorWithAttachment("Unhandled KtElement subtype: ${parent::class}") {
                withPsiEntry("parent", parent)
            }
    }
}

/**
 *  The left-hand side of a `::` is regarded as used unless it refers to a type.
 *  We decide that the LHS is a type reference by checking if the left-hand
 *  side is a (qualified) name, and, in case it _is_, resolving that name.
 *
 *  If it resolves to a non-class declaration, it does _not_ refer to a type.
 */
private fun doesDoubleColonUseLHS(lhs: PsiElement): Boolean {
    val reference = when (val inner = lhs.unwrapParenthesesLabelsAndAnnotations()) {
        is KtReferenceExpression ->
            inner.mainReference
        is KtDotQualifiedExpression ->
            (inner.selectorExpression as? KtReferenceExpression)?.mainReference ?: return true
        else ->
            return true
    }

    val resolution = reference.resolve()
    return resolution != null && resolution !is KtClass
}

/**
 * Invocations of _statically named_ callables are not considered a use.
 * E.g., consider
 *
 *   1) fun f() { 54 }; f()
 *   2) val f = { 54 }; f()
 *
 * in which the `f` in 2) is regarded as used and `f` in 1) is not.
 */
private fun KaSession.doesCallExpressionUseCallee(callee: PsiElement): Boolean {
    return callee !is KtReferenceExpression || isSimpleVariableAccessCall(callee)
}

/**
 * The body of setters is always used. The body of getters is only used if they are expression bodies.
 */
private fun doesPropertyAccessorUseBody(propertyAccessor: KtPropertyAccessor, body: PsiElement): Boolean {
    return propertyAccessor.isSetter || (propertyAccessor.isGetter && body !is KtBlockExpression)
}

/**
 * Returns whether the function uses its body as an expression (i.e., the function uses the result value of the expression) or not.
 *
 * Named functions do not consider their bodies used if
 *  - the function body is a block e.g., `fun foo(): Int { return bar }` or
 *  - the function itself returns Unit
 */
private fun KaSession.doesNamedFunctionUseBody(namedFunction: KtNamedFunction, body: PsiElement): Boolean = when {
    // The body is a block expression e.g., fun foo(): Int { return bar }
    namedFunction.bodyBlockExpression == body ->
        false
    // Note that `namedFunction.hasBlockBody() == false` means the function definition uses `=` e.g., fun foo() = bar
    !returnsUnit(namedFunction) ->
        true
    namedFunction.bodyExpression == body ->
        (body as KtExpression).expressionType?.isUnitType == true
    else ->
        false
}


private fun KaSession.isSimpleVariableAccessCall(reference: KtReferenceExpression): Boolean =
    when (val resolution = reference.resolveToCall()) {
        is KaSuccessCallInfo ->
            resolution.call is KaSimpleVariableAccessCall
        else ->
            false
    }

private fun KaSession.returnsUnit(declaration: KtDeclaration): Boolean = declaration.returnType.isUnitType