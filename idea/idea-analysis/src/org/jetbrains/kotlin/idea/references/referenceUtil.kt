/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.emptyOrSingletonList
import java.util.*

// Navigation element of the resolved reference
// For property accessor return enclosing property
val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? {
            val target = unwrapped?.originalElement
            return when {
                target is KtPropertyAccessor -> target.getNonStrictParentOfType<KtProperty>()
                else -> target
            }
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(HashSet<PsiElement>()) { it.element?.adjust() }
            else -> emptyOrSingletonList(resolve()?.adjust()).toSet()
        }
    }

//
fun PsiReference.canBeReferenceTo(candidateTarget: PsiElement): Boolean {
    // optimization
    return element.containingFile == candidateTarget.containingFile
           || ProjectRootsUtil.isInProjectOrLibSource(element)
}

fun PsiReference.matchesTarget(candidateTarget: PsiElement): Boolean {
    if (!canBeReferenceTo(candidateTarget)) return false

    val unwrappedCandidate = candidateTarget.unwrapped?.originalElement ?: return false

    // Optimizations
    when (this) {
        is KtInvokeFunctionReference -> {
            if (candidateTarget !is KtNamedFunction && candidateTarget !is PsiMethod) return false
        }
        is KtDestructuringDeclarationReference -> {
            if (candidateTarget !is KtNamedFunction && candidateTarget !is KtParameter && candidateTarget !is PsiMethod) return false
        }
    }

    val targets = unwrappedTargets
    if (unwrappedCandidate in targets) return true
    // TODO: Investigate why PsiCompiledElement identity changes
    if (unwrappedCandidate is PsiCompiledElement && targets.any { it.isEquivalentTo(unwrappedCandidate) }) return true

    if (this is KtReference) {
        return targets.any {
            it.isConstructorOf(unwrappedCandidate)
            || it is KtObjectDeclaration && it.isCompanion() && it.getNonStrictParentOfType<KtClass>() == unwrappedCandidate
        }
    }
    // TODO: Workaround for Kotlin constructor search in Java code. To be removed after refactoring of the search API
    else if (this is PsiJavaCodeReferenceElement && unwrappedCandidate is KtConstructor<*>) {
        var parent = getElement().parent
        if (parent is PsiAnonymousClass) {
            parent = parent.getParent()
        }
        if ((parent as? PsiNewExpression)?.resolveConstructor()?.unwrapped == unwrappedCandidate) return true
    }
    if (this is PsiJavaCodeReferenceElement && candidateTarget is KtObjectDeclaration && unwrappedTargets.size == 1) {
        val referredClass = unwrappedTargets.first()
        if (referredClass is KtClass && candidateTarget in referredClass.getCompanionObjects()) {
            if (parent is PsiImportStaticStatement) return true

            return parent.reference?.unwrappedTargets?.any {
                (it is KtProperty || it is KtNamedFunction) && it.parent?.parent == candidateTarget
            } ?: false
        }
    }
    return false
}

private fun PsiElement.isConstructorOf(unwrappedCandidate: PsiElement) =
    // call to Java constructor
        (this is PsiMethod && isConstructor && containingClass == unwrappedCandidate) ||
        // call to Kotlin constructor
    (this is KtConstructor<*> && getContainingClassOrObject() == unwrappedCandidate)

fun AbstractKtReference<out KtExpression>.renameImplicitConventionalCall(newName: String?): KtExpression {
    if (newName == null) return expression

    val (newExpression, newNameElement) = OperatorToFunctionIntention.convert(expression)
    newNameElement.mainReference.handleElementRename(newName)
    return newExpression
}

val KtSimpleNameExpression.mainReference: KtSimpleNameReference
    get() = references.firstIsInstance()

val KtReferenceExpression.mainReference: KtReference
    get() = if (this is KtSimpleNameExpression) mainReference else references.firstIsInstance<KtReference>()

val KDocName.mainReference: KDocReference
    get() = references.firstIsInstance()

val KtElement.mainReference: KtReference?
    get() {
        return when {
            this is KtReferenceExpression -> mainReference
            this is KDocName -> mainReference
            else -> references.firstIsInstanceOrNull<KtReference>()
        }
    }

// ----------- Read/write access -----------------------------------------------------------------------------------------------------------------------

enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false), WRITE(false, true), READ_WRITE(true, true)
}

fun KtExpression.readWriteAccess(useResolveForReadWrite: Boolean): ReferenceAccess {
    var expression = getQualifiedExpressionForSelectorOrThis()
    loop@ while (true) {
        val parent = expression.parent
        when (parent) {
            is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression = parent as KtExpression
            else -> break@loop
        }
    }

    val assignment = expression.getAssignmentByLHS()
    if (assignment != null) {
        when (assignment.operationToken) {
            KtTokens.EQ -> return ReferenceAccess.WRITE

            else -> {
                if (!useResolveForReadWrite) return ReferenceAccess.READ_WRITE

                val bindingContext = assignment.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = assignment.getResolvedCall(bindingContext) ?: return ReferenceAccess.READ_WRITE
                if (!resolvedCall.isReallySuccess()) return ReferenceAccess.READ_WRITE
                return if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
                    ReferenceAccess.READ
                else
                    ReferenceAccess.READ_WRITE
            }
        }
    }

    return if ((expression.parent as? KtUnaryExpression)?.operationToken in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) })
        ReferenceAccess.READ_WRITE
    else
        ReferenceAccess.READ
}

fun KtReference.canBeResolvedViaImport(target: DeclarationDescriptor): Boolean {
    if (!target.canBeReferencedViaImport()) return false

    if (this is KDocReference) return element.getQualifiedName().size == 1

    if (target.isExtension) return true // assume that any type of reference can use imports when resolved to extension

    val referenceExpression = this.element as? KtNameReferenceExpression ?: return false
    if (CallTypeAndReceiver.detect(referenceExpression).receiver != null) return false
    if (element.parent is KtThisExpression || element.parent is KtSuperExpression) return false // TODO: it's a bad design of PSI tree, we should change it
    return true
}