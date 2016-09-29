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
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

interface KtReference : PsiPolyVariantReference {
    fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor>

    override fun getElement(): KtElement

    val resolvesByNames: Collection<Name>
}

abstract class AbstractKtReference<T : KtElement>(element: T)
: PsiPolyVariantReferenceBase<T>(element), KtReference {

    val expression: T
        get() = element

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return PsiElementResolveResult.createResults(resolveToPsiElements())
    }

    override fun resolve(): PsiElement? {
        val psiElements = resolveToPsiElements()
        if (psiElements.size == 1) {
            return psiElements.iterator().next()
        }
        return null
    }

    override fun isReferenceTo(element: PsiElement?): Boolean {
        return element != null && matchesTarget(element)
    }

    override fun getCanonicalText(): String = "<TBD>"

    open fun canRename(): Boolean = false
    override fun handleElementRename(newElementName: String?): PsiElement? = throw IncorrectOperationException()

    override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun getVariants(): Array<Any> = PsiReference.EMPTY_ARRAY as Array<Any>

    override fun isSoft(): Boolean = false

    private fun resolveToPsiElements(): Collection<PsiElement> {
        val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
        return resolveToPsiElements(bindingContext, getTargetDescriptors(bindingContext))
    }

    override fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
        return getTargetDescriptors(bindingContext)
    }

    private fun resolveToPsiElements(context: BindingContext, targetDescriptors: Collection<DeclarationDescriptor>): Collection<PsiElement> {
        if (targetDescriptors.isNotEmpty()) {
            return targetDescriptors.flatMap { target -> resolveToPsiElements(target) }
        }

        val labelTargets = getLabelTargets(context)
        if (labelTargets != null) {
            return labelTargets
        }

        return Collections.emptySet()
    }

    private fun resolveToPsiElements(targetDescriptor: DeclarationDescriptor): Collection<PsiElement> {
        if (targetDescriptor is PackageViewDescriptor) {
            val psiFacade = JavaPsiFacade.getInstance(expression.project)
            val fqName = targetDescriptor.fqName.asString()
            return psiFacade.findPackage(fqName).singletonOrEmptyList()
        }
        else {
            return DescriptorToSourceUtilsIde.getAllDeclarations(expression.project, targetDescriptor)
        }
    }

    protected abstract fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    private fun getLabelTargets(context: BindingContext): Collection<PsiElement>? {
        val reference = expression
        if (reference !is KtReferenceExpression) {
            return null
        }
        val labelTarget = context[BindingContext.LABEL_TARGET, reference]
        if (labelTarget != null) {
            return listOf(labelTarget)
        }
        return context[BindingContext.AMBIGUOUS_LABEL_TARGET, reference]
    }

    override fun toString() = javaClass.simpleName + ": " + expression.text
}

abstract class KtSimpleReference<T : KtReferenceExpression>(expression: T) : AbstractKtReference<T>(expression) {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

abstract class KtMultiReference<T : KtElement>(expression: T) : AbstractKtReference<T>(expression)
