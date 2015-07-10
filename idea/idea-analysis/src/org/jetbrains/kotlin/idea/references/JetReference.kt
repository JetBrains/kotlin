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
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.Collections

public trait JetReference : PsiPolyVariantReference {
    public fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor>
}

public abstract class AbstractJetReference<T : JetElement>(element: T)
: PsiPolyVariantReferenceBase<T>(element), JetReference {

    public val expression: T
        get() = getElement()

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return PsiElementResolveResult.createResults(resolveToPsiElements())
    }

    override fun resolve(): PsiElement? {
        val psiElements = resolveToPsiElements()
        if (psiElements.size() == 1) {
            return psiElements.iterator().next()
        }
        return null
    }

    override fun isReferenceTo(element: PsiElement?): Boolean {
        return element != null && matchesTarget(element)
    }

    override fun getCanonicalText(): String = "<TBD>"

    public open fun canRename(): Boolean = false
    override fun handleElementRename(newElementName: String?): PsiElement? = throw IncorrectOperationException()

    override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException()

    @suppress("CAST_NEVER_SUCCEEDS")
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
            return targetDescriptors flatMap { target -> resolveToPsiElements(target) }
        }

        val labelTargets = getLabelTargets(context)
        if (labelTargets != null) {
            return labelTargets
        }

        return Collections.emptySet()
    }

    private fun resolveToPsiElements(targetDescriptor: DeclarationDescriptor): Collection<PsiElement> {
        if (targetDescriptor is PackageViewDescriptor) {
            val psiFacade = JavaPsiFacade.getInstance(expression.getProject())
            val fqName = targetDescriptor.fqName.asString()
            return psiFacade.findPackage(fqName).singletonOrEmptyList()
        }
        else {
            return DescriptorToSourceUtilsIde.getAllDeclarations(expression.getProject(), targetDescriptor)
        }
    }

    protected abstract fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    private fun getLabelTargets(context: BindingContext): Collection<PsiElement>? {
        val reference = expression
        if (reference !is JetReferenceExpression) {
            return null
        }
        val labelTarget = context[BindingContext.LABEL_TARGET, reference]
        if (labelTarget != null) {
            return listOf(labelTarget)
        }
        return context[BindingContext.AMBIGUOUS_LABEL_TARGET, reference]
    }

    override fun toString() = javaClass.getSimpleName() + ": " + expression.getText()
}

public abstract class JetSimpleReference<T : JetReferenceExpression>(expression: T) : AbstractJetReference<T>(expression) {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

public abstract class JetMultiReference<T : JetElement>(expression: T) : AbstractJetReference<T>(expression)
