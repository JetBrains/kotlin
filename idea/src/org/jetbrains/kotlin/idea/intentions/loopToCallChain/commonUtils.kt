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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun KtExpression.isConstant(): Boolean {
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    return ConstantExpressionEvaluator.getConstant(this, bindingContext) != null
}

fun KtExpression?.isTrueConstant()
        = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "true"

fun KtExpression?.isFalseConstant()
        = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "false"

private val ZERO_VALUES = setOf(0, 0L, 0f, 0.0)

fun KtExpression.isZeroConstant(): Boolean {
    if (this !is KtConstantExpression) return false
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    val type = bindingContext.getType(this) ?: return false
    val constant = ConstantExpressionEvaluator.getConstant(this, bindingContext) ?: return false
    return constant.getValue(type) in ZERO_VALUES
}

fun KtExpression?.isVariableReference(variable: KtCallableDeclaration): Boolean {
    return this is KtNameReferenceExpression && this.mainReference.isReferenceTo(variable)
}

fun KtExpression?.isSimpleName(name: Name): Boolean {
    return this is KtNameReferenceExpression && this.getQualifiedExpressionForSelector() == null && this.getReferencedNameAsName() == name
}

fun KtCallableDeclaration.hasUsages(inElement: KtElement): Boolean {
    assert(inElement.isPhysical)
    return hasUsages(listOf(inElement))
}

fun KtCallableDeclaration.hasUsages(inElements: Collection<KtElement>): Boolean {
    assert(this.isPhysical)
    // TODO: it's a temporary workaround about strange dead-lock when running inspections
    return inElements.any { ReferencesSearch.search(this, LocalSearchScope(it)).any() }
//    return ReferencesSearch.search(this, LocalSearchScope(inElements.toTypedArray())).any()
}

fun KtVariableDeclaration.hasWriteUsages(): Boolean {
    assert(this.isPhysical)
    if (!isVar) return false
    return ReferencesSearch.search(this, useScope).any {
        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
    }
}

fun KtCallableDeclaration.countUsages(inElement: KtElement): Int {
    assert(this.isPhysical)
    return ReferencesSearch.search(this, LocalSearchScope(inElement)).count()
}

fun KtCallableDeclaration.countUsages(inElements: Collection<KtElement>): Int {
    assert(this.isPhysical)
    // TODO: it's a temporary workaround about strange dead-lock when running inspections
    return inElements.sumBy { ReferencesSearch.search(this, LocalSearchScope(it)).count() }
}

fun KtCallableDeclaration.countUsages(): Int {
    assert(this.isPhysical)
    return ReferencesSearch.search(this, useScope).count()
}

fun KtVariableDeclaration.countWriteUsages(): Int {
    assert(this.isPhysical)
    if (!isVar) return 0
    return ReferencesSearch.search(this, useScope).count {
        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
    }
}

fun KtVariableDeclaration.countWriteUsages(inElement: KtElement): Int {
    assert(this.isPhysical)
    if (!isVar) return 0
    return ReferencesSearch.search(this, LocalSearchScope(inElement)).count {
        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
    }
}

fun KtVariableDeclaration.hasWriteUsages(inElement: KtElement): Boolean {
    assert(this.isPhysical)
    if (!isVar) return false
    return ReferencesSearch.search(this, LocalSearchScope(inElement)).any {
        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
    }
}

fun KtCallableDeclaration.hasDifferentSetsOfUsages(elements1: Collection<KtElement>, elements2: Collection<KtElement>): Boolean {
    return countUsages(elements1 - elements2) != countUsages(elements2 - elements1)
}

fun KtExpressionWithLabel.targetLoop(): KtLoopExpression? {
    val label = getTargetLabel()
    return if (label == null) {
        parents.firstIsInstance<KtLoopExpression>()
    }
    else {
        //TODO: does PARTIAL always work here?
        analyze(BodyResolveMode.PARTIAL)[BindingContext.LABEL_TARGET, label] as? KtLoopExpression
    }
}

fun KtExpression.isPlusPlusOf(): KtExpression? {
    if (this !is KtUnaryExpression) return null
    if (operationToken != KtTokens.PLUSPLUS) return null
    return baseExpression
}

fun KtExpression.previousStatement(): KtExpression? {
    val statement = unwrapIfLabeled()
    if (statement.parent !is KtBlockExpression) return null
    return statement.siblings(forward = false, withItself = false).firstIsInstanceOrNull<KtExpression>()
}

fun KtExpression.nextStatement(): KtExpression? {
    val statement = unwrapIfLabeled()
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

fun KtExpression?.extractStaticFunctionCallArguments(functionFqName: String): List<KtExpression?>? {
    val callExpression = when (this) {
        is KtDotQualifiedExpression -> selectorExpression as? KtCallExpression
        is KtCallExpression -> this
        else -> null
    } ?: return null

    val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
    val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
    val functionDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null
    if (functionDescriptor.dispatchReceiverParameter != null || functionDescriptor.extensionReceiverParameter != null) return null
    if (functionDescriptor.importableFqName?.asString() != functionFqName) return null

    return resolvedCall.valueArgumentsByIndex?.map { it?.arguments?.singleOrNull()?.getArgumentExpression() }
}
