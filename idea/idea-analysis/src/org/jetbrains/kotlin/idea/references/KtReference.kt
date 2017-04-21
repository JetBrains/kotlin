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
import com.intellij.psi.impl.source.resolve.ResolveCache
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
import java.util.*

interface KtReference : PsiPolyVariantReference {
    fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor>

    override fun getElement(): KtElement

    val resolvesByNames: Collection<Name>
}

abstract class AbstractKtReference<T : KtElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), KtReference {
    val expression: T
        get() = element

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        @Suppress("UNCHECKED_CAST")
        val kotlinResolver = KOTLIN_RESOLVER as ResolveCache.PolyVariantResolver<AbstractKtReference<T>>
        return ResolveCache.getInstance(expression.project).resolveWithCaching(this, kotlinResolver, false, incompleteCode)
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

    override fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
        return getTargetDescriptors(bindingContext)
    }

    protected abstract fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    override fun toString() = this::class.java.simpleName + ": " + expression.text

    companion object {
        class KotlinReferenceResolver : ResolveCache.PolyVariantResolver<AbstractKtReference<KtElement>> {
            class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

            private fun resolveToPsiElements(ref: AbstractKtReference<KtElement>): Collection<PsiElement> {
                val bindingContext = ref.expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
                return resolveToPsiElements(ref, bindingContext, ref.getTargetDescriptors(bindingContext))
            }

            private fun resolveToPsiElements(ref: AbstractKtReference<KtElement>, context: BindingContext, targetDescriptors: Collection<DeclarationDescriptor>): Collection<PsiElement> {
                if (targetDescriptors.isNotEmpty()) {
                    return targetDescriptors.flatMap { target -> resolveToPsiElements(ref, target) }.toSet()
                }

                val labelTargets = getLabelTargets(ref, context)
                if (labelTargets != null) {
                    return labelTargets
                }

                return Collections.emptySet()
            }

            private fun resolveToPsiElements(ref: AbstractKtReference<KtElement>, targetDescriptor: DeclarationDescriptor): Collection<PsiElement> {
                if (targetDescriptor is PackageViewDescriptor) {
                    val psiFacade = JavaPsiFacade.getInstance(ref.expression.project)
                    val fqName = targetDescriptor.fqName.asString()
                    return listOfNotNull(psiFacade.findPackage(fqName))
                }
                else {
                    return DescriptorToSourceUtilsIde.getAllDeclarations(ref.expression.project, targetDescriptor, ref.expression.resolveScope)
                }
            }

            private fun getLabelTargets(ref: AbstractKtReference<KtElement>, context: BindingContext): Collection<PsiElement>? {
                val reference = ref.expression as? KtReferenceExpression ?: return null
                val labelTarget = context[BindingContext.LABEL_TARGET, reference]
                if (labelTarget != null) {
                    return listOf(labelTarget)
                }
                return context[BindingContext.AMBIGUOUS_LABEL_TARGET, reference]
            }

            override fun resolve(ref: AbstractKtReference<KtElement>, incompleteCode: Boolean): Array<ResolveResult> {
                val resolveToPsiElements = resolveToPsiElements(ref)
                return resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
            }
        }

        val KOTLIN_RESOLVER = KotlinReferenceResolver()
    }
}

abstract class KtSimpleReference<T : KtReferenceExpression>(expression: T) : AbstractKtReference<T>(expression) {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

abstract class KtMultiReference<T : KtElement>(expression: T) : AbstractKtReference<T>(expression)
