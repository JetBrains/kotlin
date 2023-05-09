/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLabeledParent
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.util.OperatorNameConventions

interface KtReference : PsiPolyVariantReference {
    val resolver: ResolveCache.PolyVariantResolver<KtReference>

    override fun getElement(): KtElement

    val resolvesByNames: Collection<Name>
}

abstract class AbstractKtReference<T : KtElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), KtReference {
    val expression: T
        get() = element

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)

    override fun getCanonicalText(): String = expression.text

    open fun canRename(): Boolean = false
    override fun handleElementRename(newElementName: String): PsiElement? =
        if (canRename())
            getKtReferenceMutateService().handleElementRename(this, newElementName)
        else
            null

    override fun bindToElement(element: PsiElement): PsiElement =
        getKtReferenceMutateService().bindToElement(this, element)

    protected fun getKtReferenceMutateService(): KtReferenceMutateService =
        ApplicationManager.getApplication().getService(KtReferenceMutateService::class.java)
            ?: throw IllegalStateException("Cannot handle element rename because KtReferenceMutateService is missing")

    @Suppress("UNCHECKED_CAST")
    override fun getVariants(): Array<Any> = PsiReference.EMPTY_ARRAY as Array<Any>

    override fun isSoft(): Boolean = false

    override fun toString() = this::class.java.simpleName + ": " + expression.text

    protected open fun canBeReferenceTo(candidateTarget: PsiElement): Boolean = true

    protected open fun isReferenceToImportAlias(alias: KtImportAlias): Boolean = false

    override fun isReferenceTo(candidateTarget: PsiElement): Boolean {
        if (!canBeReferenceTo(candidateTarget)) return false

        val unwrappedCandidate = candidateTarget.unwrapped?.originalElement ?: return false

        // Optimizations to return early for cases where this reference cannot
        // refer to the candidate target.
        when (this) {
            is KtInvokeFunctionReference -> {
                if (candidateTarget !is KtNamedFunction && candidateTarget !is PsiMethod) return false
                if ((candidateTarget as PsiNamedElement).name != OperatorNameConventions.INVOKE.asString()) {
                    return false
                }
            }
            is KtDestructuringDeclarationReference -> {
                if (candidateTarget !is KtNamedFunction && candidateTarget !is KtParameter && candidateTarget !is PsiMethod) return false
            }
            is KtSimpleNameReference -> {
                if (unwrappedCandidate is PsiMethod && !canBePsiMethodReference()) return false
            }
        }

        val element = element

        if (candidateTarget is KtImportAlias &&
            (element is KtSimpleNameExpression && element.getReferencedName() == candidateTarget.name ||
                    this is KDocReference && this.canonicalText == candidateTarget.name)
        ) {
            return isReferenceToImportAlias(candidateTarget)
        }

        if (element is KtLabelReferenceExpression) {
            when ((element.parent as? KtContainerNode)?.parent) {
                is KtReturnExpression -> unwrappedTargets.forEach {
                    if (it !is KtFunctionLiteral && !(it is KtNamedFunction && it.name.isNullOrEmpty())) return@forEach
                    it as KtFunction

                    val labeledExpression = it.getLabeledParent(element.getReferencedName())
                    if (labeledExpression != null) {
                        if (candidateTarget == labeledExpression) return true else return@forEach
                    }
                    val calleeReference = it.getCalleeByLambdaArgument()?.mainReference ?: return@forEach
                    if (calleeReference.isReferenceTo(candidateTarget)) return true
                }
                is KtBreakExpression, is KtContinueExpression -> unwrappedTargets.forEach {
                    val labeledExpression = (it as? KtExpression)?.getLabeledParent(element.getReferencedName()) ?: return@forEach
                    if (candidateTarget == labeledExpression) return true
                }
            }
        }

        val targets = unwrappedTargets
        val manager = candidateTarget.manager

        if (targets.any { manager.areElementsEquivalent(unwrappedCandidate, it) }) {
            return true
        }

        return targets.any {
            it.isConstructorOf(unwrappedCandidate) ||
                    it is KtObjectDeclaration && it.isCompanion() && it.getNonStrictParentOfType<KtClass>() == unwrappedCandidate
        }
    }

    private fun KtSimpleNameReference.canBePsiMethodReference(): Boolean {
        // NOTE: Accessor references are handled separately, see SyntheticPropertyAccessorReference
        if (element == (element.parent as? KtCallExpression)?.calleeExpression) return true

        val callableReference = element.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference }
        if (callableReference != null) return true

        val binaryOperator = element.getParentOfTypeAndBranch<KtBinaryExpression> { operationReference }
        if (binaryOperator != null) return true

        val unaryOperator = element.getParentOfTypeAndBranch<KtUnaryExpression> { operationReference }
        if (unaryOperator != null) return true

        if (element.getNonStrictParentOfType<KtImportDirective>() != null) return true

        return false
    }

    private fun PsiElement.isConstructorOf(unwrappedCandidate: PsiElement) =
        when {
            // call to Java constructor
            this is PsiMethod && isConstructor && containingClass == unwrappedCandidate -> true
            // call to Kotlin constructor
            this is KtConstructor<*> && getContainingClassOrObject().isEquivalentTo(unwrappedCandidate) -> true
            else -> false
        }
}

abstract class KtSimpleReference<T : KtReferenceExpression>(expression: T) : AbstractKtReference<T>(expression)

abstract class KtMultiReference<T : KtElement>(expression: T) : AbstractKtReference<T>(expression)
