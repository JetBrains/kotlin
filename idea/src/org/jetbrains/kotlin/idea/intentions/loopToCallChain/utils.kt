/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun generateLambda(workingVariable: KtCallableDeclaration, expression: KtExpression): KtLambdaExpression {
    val psiFactory = KtPsiFactory(expression)

    val lambdaExpression = psiFactory.createExpressionByPattern("{ $0 -> $1 }", workingVariable.nameAsSafeName, expression) as KtLambdaExpression

    val isItUsedInside = expression.anyDescendantOfType<KtNameReferenceExpression> {
        it.getQualifiedExpressionForSelector() == null && it.getReferencedName() == "it"
    }

    if (isItUsedInside) return lambdaExpression

    val resolutionScope = workingVariable.getResolutionScope(workingVariable.analyze(BodyResolveMode.FULL), workingVariable.getResolutionFacade())
    val bindingContext = lambdaExpression.analyzeInContext(resolutionScope, contextExpression = workingVariable)
    val lambdaParam = lambdaExpression.valueParameters.single()
    val lambdaParamDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, lambdaParam]
    val usages = lambdaExpression.collectDescendantsOfType<KtNameReferenceExpression> {
        it.mainReference.resolveToDescriptors(bindingContext).singleOrNull() == lambdaParamDescriptor
    }

    val itExpr = psiFactory.createSimpleName("it")
    for (usage in usages) {
        usage.replace(itExpr)
    }

    return psiFactory.createExpressionByPattern("{ $0 }", lambdaExpression.bodyExpression!!) as KtLambdaExpression
}

fun KtExpression?.isTrueConstant()
        = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "true"

fun KtExpression?.isFalseConstant()
        = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "false"

fun KtExpression?.isVariableReference(variable: KtCallableDeclaration): Boolean {
    return this is KtNameReferenceExpression && this.mainReference.isReferenceTo(variable)
}

fun KtExpression?.isSimpleName(name: Name): Boolean {
    return this is KtNameReferenceExpression && this.getQualifiedExpressionForSelector() == null && this.getReferencedNameAsName() == name
}

fun KtCallableDeclaration.hasUsages(inElements: Collection<KtElement>): Boolean {
    // TODO: it's a temporary workaround about strange dead-lock when running inspections
    return inElements.any { ReferencesSearch.search(this, LocalSearchScope(it)).any() }
//    return ReferencesSearch.search(this, LocalSearchScope(inElements.toTypedArray())).any()
}

fun KtProperty.hasWriteUsages(): Boolean {
    if (!isVar) return false
    return ReferencesSearch.search(this, useScope).any {
        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
    }
}

fun stdlibFunNameForFind(valueIfFound: KtExpression, valueIfNotFound: KtExpression, workingVariable: KtCallableDeclaration): String? {
    return when {
        valueIfNotFound.isNullExpression() && valueIfFound.isVariableReference(workingVariable) -> "firstOrNull"

        valueIfFound.isTrueConstant() && valueIfNotFound.isFalseConstant() -> "any"

        valueIfFound.isFalseConstant() && valueIfNotFound.isTrueConstant() -> "none"

        else -> /*TODO: allow other constants*/ null
    }
}

fun KtExpressionWithLabel.isBreakOrContinueOfLoop(loop: KtLoopExpression): Boolean {
    val label = getTargetLabel()
    if (label == null) {
        val closestLoop = parents.firstIsInstance<KtLoopExpression>()
        return loop == closestLoop
    }
    else {
        //TODO: does PARTIAL always work here?
        val targetLoop = analyze(BodyResolveMode.PARTIAL)[BindingContext.LABEL_TARGET, label]
        return targetLoop == loop
    }
}

fun KtExpression.previousStatement(): KtExpression? {
    var statement = unwrapIfLabeled()
    if (statement.parent !is KtBlockExpression) return null
    return statement.siblings(forward = false, withItself = false).firstIsInstanceOrNull<KtExpression>()
}

fun KtExpression.nextStatement(): KtExpression? {
    var statement = unwrapIfLabeled()
    if (statement.parent !is KtBlockExpression) return null
    return statement.siblings(forward = true, withItself = false).firstIsInstanceOrNull<KtExpression>()
}

fun KtExpression.unwrapIfLabeled(): KtExpression {
    var statement = this
    while (true) {
        statement = statement.parent as? KtLabeledExpression ?: return statement
    }
}

fun KtLoopExpression.deleteWithLabels() {
    unwrapIfLabeled().delete()
}

fun PsiChildRange.withoutFirstStatement(): PsiChildRange {
    val newFirst = first!!.siblings(forward = true, withItself = false).first { it !is PsiWhiteSpace }
    return PsiChildRange(newFirst, last)
}

fun PsiChildRange.withoutLastStatement(): PsiChildRange {
    val newLast = last!!.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
    return PsiChildRange(first, newLast)
}
