/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isOrdinaryAssignment
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*
import org.jetbrains.kotlin.idea.references.resolveToDescriptors

fun generateLambda(inputVariable: KtCallableDeclaration, expression: KtExpression, reformat: Boolean): KtLambdaExpression {
    val psiFactory = KtPsiFactory(expression)

    val lambdaExpression = psiFactory.createExpressionByPattern(
        "{ $0 -> $1 }", inputVariable.nameAsSafeName, expression,
        reformat = reformat
    ) as KtLambdaExpression

    val isItUsedInside = expression.anyDescendantOfType<KtNameReferenceExpression> {
        it.getQualifiedExpressionForSelector() == null && it.getReferencedName() == "it"
    }

    if (isItUsedInside) return lambdaExpression

    val usages = lambdaExpression.findParameterUsages(lambdaExpression.valueParameters.single(), inputVariable)

    val itExpr = psiFactory.createSimpleName("it")
    for (usage in usages) {
        val replaced = usage.replaced(itExpr)

        // we need to copy user data for checkSmartCastsPreserved() to work
        (usage.node as UserDataHolderBase).copyCopyableDataTo(replaced.node as UserDataHolderBase)
    }

    val lambdaBodyExpression = lambdaExpression.bodyExpression!!.statements.single()
    return psiFactory.createExpressionByPattern("{ $0 }", lambdaBodyExpression, reformat = reformat) as KtLambdaExpression
}

fun generateLambda(
    inputVariable: KtCallableDeclaration,
    indexVariable: KtCallableDeclaration?,
    expression: KtExpression,
    reformat: Boolean
): KtLambdaExpression {
    if (indexVariable == null) {
        return generateLambda(inputVariable, expression, reformat)
    }

    val lambdaExpression = generateLambda(expression, *arrayOf(indexVariable, inputVariable), reformat = reformat)

    // replace "index++" with "index" or "index + 1" (see IntroduceIndexMatcher)
    val indexPlusPlus = lambdaExpression.findDescendantOfType<KtUnaryExpression> { unaryExpression ->
        val operand = unaryExpression.isPlusPlusOf() as? KtNameReferenceExpression
        if (operand != null && operand.getReferencedName() == indexVariable.name) {
            val bindingContext = lambdaExpression.analyzeInContext(inputVariable)
            val parameter = lambdaExpression.valueParameters[0]
            val parameterDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parameter]
            operand.mainReference.resolveToDescriptors(bindingContext).singleOrNull() == parameterDescriptor
        } else {
            false
        }
    }
    if (indexPlusPlus != null) {
        removePlusPlus(indexPlusPlus, reformat)
    }

    return lambdaExpression
}

fun removePlusPlus(indexPlusPlus: KtUnaryExpression, reformat: Boolean) {
    val operand = indexPlusPlus.baseExpression!!
    val replacement = if (indexPlusPlus is KtPostfixExpression) // index++
        operand
    else // ++index
        KtPsiFactory(operand).createExpressionByPattern("$0 + 1", operand, reformat = reformat)
    indexPlusPlus.replace(replacement)
}

fun generateLambda(expression: KtExpression, vararg inputVariables: KtCallableDeclaration, reformat: Boolean): KtLambdaExpression {
    return KtPsiFactory(expression).buildExpression(reformat = reformat) {
        appendFixedText("{")

        for ((index, variable) in inputVariables.withIndex()) {
            if (index > 0) {
                appendFixedText(",")
            }
            appendName(variable.nameAsSafeName)
        }

        appendFixedText("->")

        appendExpression(expression)

        appendFixedText("}")
    } as KtLambdaExpression
}

private fun KtLambdaExpression.findParameterUsages(lambdaParam: KtParameter, context: KtExpression): Collection<KtNameReferenceExpression> {
    val bindingContext = analyzeInContext(context)
    val lambdaParamDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, lambdaParam]
    return collectDescendantsOfType {
        it.mainReference.resolveToDescriptors(bindingContext).singleOrNull() == lambdaParamDescriptor
    }
}

private fun KtLambdaExpression.analyzeInContext(context: KtExpression): BindingContext {
    val resolutionScope = context.getResolutionScope(context.analyze(BodyResolveMode.FULL), context.getResolutionFacade())
    return analyzeInContext(resolutionScope, contextExpression = context)
}

data class VariableInitialization(
    val variable: KtProperty,
    val initializationStatement: KtExpression,
    val initializer: KtExpression
)

//TODO: we need more correctness checks (if variable is non-local or is local but can be changed by some local functions)
fun KtExpression?.findVariableInitializationBeforeLoop(
    loop: KtForExpression,
    checkNoOtherUsagesInLoop: Boolean
): VariableInitialization? {
    if (this !is KtNameReferenceExpression) return null
    if (getQualifiedExpressionForSelector() != null) return null
    val variable = this.mainReference.resolve() as? KtProperty ?: return null

    // do not allow any other usages of this variable inside the loop
    if (checkNoOtherUsagesInLoop && variable.countUsages(loop) > 1) return null

    val unwrapped = loop.unwrapIfLabeled()
    if (unwrapped.parent !is KtBlockExpression) return null
    val prevStatements = unwrapped
        .siblings(forward = false, withItself = false)
        .filterIsInstance<KtExpression>()

    val statementsBetween = ArrayList<KtExpression>()
    for (statement in prevStatements) {
        val variableInitialization = extractVariableInitialization(statement, variable)
        if (variableInitialization != null) {
            return variableInitialization.takeIf {
                statementsBetween.all { canSwapExecutionOrder(variableInitialization.initializationStatement, it) }
            }
        }

        statementsBetween.add(statement)
    }

    return null
}

private fun extractVariableInitialization(statement: KtExpression, variable: KtProperty): VariableInitialization? {
    if (statement == variable) {
        val initializer = variable.initializer ?: return null
        return VariableInitialization(variable, variable, initializer)
    }

    val assignment = statement.asAssignment() ?: return null
    if (!assignment.left.isVariableReference(variable)) return null

    val initializer = assignment.right ?: return null
    return VariableInitialization(variable, assignment, initializer)
}

enum class CollectionKind {
    LIST, SET/*, MAP*/
}

fun KtExpression.isSimpleCollectionInstantiation(): CollectionKind? {
    val callExpression = this as? KtCallExpression ?: return null //TODO: it can be qualified too
    if (callExpression.valueArguments.isNotEmpty()) return null

    val resolvedCall = callExpression.resolveToCall() ?: return null

    return when (val descriptor = resolvedCall.resultingDescriptor) {
        is ConstructorDescriptor -> {
            val classDescriptor = descriptor.containingDeclaration
            when (classDescriptor.importableFqName?.asString()) {
                "java.util.ArrayList" -> CollectionKind.LIST
                "java.util.HashSet", "java.util.LinkedHashSet" -> CollectionKind.SET
                else -> null
            }
        }

        is FunctionDescriptor -> {
            when (descriptor.importableFqName?.asString()) {
                "kotlin.collections.arrayListOf", "kotlin.collections.mutableListOf" -> CollectionKind.LIST
                "kotlin.collections.hashSetOf", "kotlin.collections.mutableSetOf" -> CollectionKind.SET
                else -> null
            }
        }

        else -> null
    }
}

fun canChangeLocalVariableType(variable: KtProperty, newTypeText: String, loop: KtForExpression): Boolean {
    return tryChangeAndCheckErrors(variable, loop) {
        it.typeReference = KtPsiFactory(it).createType(newTypeText)
    }
}

fun <TExpression : KtExpression> tryChangeAndCheckErrors(
    expressionToChange: TExpression,
    scopeToExclude: KtElement? = null,
    performChange: (TExpression) -> Unit
): Boolean {
    val bindingContext = expressionToChange.analyze(BodyResolveMode.FULL)

    // analyze the closest block whose value is not used
    val block = expressionToChange.parents
        .filterIsInstance<KtBlockExpression>()
        .firstOrNull { !it.isUsedAsExpression(bindingContext) }
        ?: return true

    // we declare these keys locally to avoid possible race-condition problems if this code is executed in 2 threads simultaneously
    val EXPRESSION = Key<Unit>("EXPRESSION")
    val SCOPE_TO_EXCLUDE = Key<Unit>("SCOPE_TO_EXCLUDE")
    val ERRORS_BEFORE = Key<Collection<DiagnosticFactory<*>>>("ERRORS_BEFORE")

    expressionToChange.putCopyableUserData(EXPRESSION, Unit)
    scopeToExclude?.putCopyableUserData(SCOPE_TO_EXCLUDE, Unit)

    block.forEachDescendantOfType<PsiElement> { element ->
        val errors = bindingContext.diagnostics.forElement(element)
            .filter { it.severity == Severity.ERROR }
        if (errors.isNotEmpty()) {
            element.putCopyableUserData(ERRORS_BEFORE, errors.map { it.factory })
        }
    }

    val blockCopy = block.copied()
    val expressionCopy: TExpression
    val scopeToExcludeCopy: KtElement?
    @Suppress("UNCHECKED_CAST")
    try {
        expressionCopy = blockCopy.findDescendantOfType<KtExpression> { it.getCopyableUserData(EXPRESSION) != null } as TExpression
        scopeToExcludeCopy = blockCopy.findDescendantOfType<KtElement> { it.getCopyableUserData(SCOPE_TO_EXCLUDE) != null }
    } finally {
        expressionToChange.putCopyableUserData(EXPRESSION, null)
        scopeToExclude?.putCopyableUserData(SCOPE_TO_EXCLUDE, null)
    }

    performChange(expressionCopy)

    val newBindingContext = blockCopy.analyzeAsReplacement(block, bindingContext)
    return newBindingContext.diagnostics.none {
        it.severity == Severity.ERROR
                && !scopeToExcludeCopy.isAncestor(it.psiElement)
                && it.factory !in (it.psiElement.getCopyableUserData(ERRORS_BEFORE) ?: emptyList())
    }
}

private val NO_SIDE_EFFECT_STANDARD_CLASSES = setOf(
    "java.util.ArrayList",
    "java.util.LinkedList",
    "java.util.HashSet",
    "java.util.LinkedHashSet",
    "java.util.HashMap",
    "java.util.LinkedHashMap"
)

fun KtExpression.hasNoSideEffect(): Boolean {
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    if (ConstantExpressionEvaluator.getConstant(this, bindingContext) != null) return true

    val callExpression = this as? KtCallExpression ?: return false//TODO: it can be qualified too
    if (callExpression.valueArguments.any { it.getArgumentExpression()?.hasNoSideEffect() == false }) return false

    val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false
    val constructorDescriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return false
    val classDescriptor = constructorDescriptor.containingDeclaration
    val classFqName = classDescriptor.importableFqName?.asString()
    return classFqName in NO_SIDE_EFFECT_STANDARD_CLASSES
}

//TODO: we need more correctness checks (if variable is non-local or is local but can be changed by some local functions)
fun canSwapExecutionOrder(expressionBefore: KtExpression, expressionAfter: KtExpression): Boolean {
    assert(expressionBefore.isPhysical)
    assert(expressionAfter.isPhysical)

    if (expressionBefore is KtDeclaration) {
        if (expressionBefore !is KtProperty) return false // local function, class or destructuring declaration - do not bother to handle these rare cases
        if (expressionBefore.hasUsages(expressionAfter)) return false
        return canSwapExecutionOrder(expressionBefore.initializer ?: return true, expressionAfter)
    }

    if (expressionAfter is KtDeclaration) {
        if (expressionAfter !is KtProperty) return false // local function, class or destructuring declaration - do not bother to handle these rare cases
        return canSwapExecutionOrder(expressionBefore, expressionAfter.initializer ?: return true)
    }

    if (expressionBefore is KtBinaryExpression && isOrdinaryAssignment(expressionBefore)) {
        val leftName = expressionBefore.left as? KtSimpleNameExpression ?: return false
        val target = leftName.mainReference.resolve() as? KtProperty ?: return false
        if (target.hasUsages(expressionAfter)) return false
        return canSwapExecutionOrder(expressionBefore.right ?: return true, expressionAfter)
    }

    if (expressionAfter is KtBinaryExpression && isOrdinaryAssignment(expressionAfter)) {
        val leftName = expressionAfter.left as? KtSimpleNameExpression ?: return false
        val target = leftName.mainReference.resolve() as? KtProperty ?: return false
        if (target.hasUsages(expressionBefore)) return false
        return canSwapExecutionOrder(expressionBefore, expressionAfter.right ?: return true)
    }

    if (expressionBefore.hasNoSideEffect() || expressionAfter.hasNoSideEffect()) return true

    //TODO: more cases
    return false
}

fun KtExpression.isStableInLoop(loop: KtLoopExpression, checkNoOtherUsagesInLoop: Boolean): Boolean {
    when {
        isConstant() -> return true

        this is KtSimpleNameExpression -> {
            val declaration = mainReference.resolve() as? KtCallableDeclaration ?: return false
            if (loop.isAncestor(declaration)) return false // should be declared outside the loop
            val variable = declaration.resolveToDescriptorIfAny() as? VariableDescriptor ?: return false

            if (checkNoOtherUsagesInLoop && declaration.countUsages(loop) > 1) return false

            if (!variable.isVar) return true
            if (declaration !is KtVariableDeclaration) return false
            if (!KtPsiUtil.isLocal(declaration)) return false // it's difficult to analyze non-local declarations
            //TODO: check that there are no local functions or lambdas that can modify it implicitly
            return !declaration.hasWriteUsages(loop)
        }

        //TODO: qualified expression?
        //TODO: this

        else -> return false
    }
}

fun KtExpression.containsEmbeddedBreakOrContinue(): Boolean {
    return anyDescendantOfType(::isEmbeddedBreakOrContinue)
}

fun KtExpression.countEmbeddedBreaksAndContinues(): Int {
    return collectDescendantsOfType(::isEmbeddedBreakOrContinue).size
}

private fun isEmbeddedBreakOrContinue(expression: KtExpressionWithLabel): Boolean {
    if (expression !is KtBreakExpression && expression !is KtContinueExpression) return false
    return when (val parent = expression.parent) {
        is KtBlockExpression -> false

        is KtContainerNode -> {
            val containerExpression = parent.parent as KtExpression
            containerExpression.isUsedAsExpression(containerExpression.analyze(BodyResolveMode.PARTIAL_WITH_CFA))
        }

        else -> true
    }
}

fun MatchingState.unwrapBlock(): MatchingState {
    val block = statements.singleOrNull() as? KtBlockExpression ?: return this
    return this.copy(statements = block.statements)
}
