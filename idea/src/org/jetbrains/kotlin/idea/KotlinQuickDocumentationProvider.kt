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

package org.jetbrains.kotlin.idea

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.completion.DeclarationDescriptorLookupObject
import org.jetbrains.kotlin.idea.kdoc.KDocFinder
import org.jetbrains.kotlin.idea.kdoc.KDocRenderer
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

public class KotlinQuickDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement): String? {
        return getText(element, originalElement, true)
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return getText(element, originalElement, false)
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement?): PsiElement? {
        if (context !is JetElement) {
            return null
        }

        val bindingContext = context.analyze(BodyResolveMode.PARTIAL)
        val contextDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, context]
        if (contextDescriptor == null) {
            return null
        }
        val descriptors = resolveKDocLink(context.getResolutionFacade(), contextDescriptor, null, StringUtil.split(link, ","))
        val target = descriptors.firstOrNull()
        if (target is DeclarationDescriptorWithSource) {
            val source = target.getSource()
            return (source as? PsiSourceElement)?.psi
        }
        return null
    }

    override fun getDocumentationElementForLookupItem(psiManager: PsiManager, `object`: Any?, element: PsiElement?): PsiElement? {
        if (`object` is DeclarationDescriptorLookupObject) {
            return `object`.psiElement
        }
        return null
    }

    companion object {
        private val LOG = Logger.getInstance(javaClass<KotlinQuickDocumentationProvider>())

        val quickDocNameFormat = DescriptorRenderer.withOptions {
            withDefinedIn = true
            nameShortness = NameShortness.SHORT
            renderCompanionObjectName = true
            textFormat = DescriptorRenderer.TextFormat.HTML
        }


        private fun getText(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean): String? {
            if (element is JetDeclaration) {
                return renderKotlinDeclaration(element, quickNavigation)
            }
            else if (element is KotlinLightElement<*, *>) {
                val origin = element.getOrigin() ?: return null
                return renderKotlinDeclaration(origin, quickNavigation)
            }

            if (quickNavigation) {
                val referenceExpression = originalElement?.getNonStrictParentOfType<JetReferenceExpression>()
                if (referenceExpression != null) {
                    val context = referenceExpression.analyze(BodyResolveMode.PARTIAL)
                    val declarationDescriptor = context[BindingContext.REFERENCE_TARGET, referenceExpression]
                    if (declarationDescriptor != null) {
                        return mixKotlinToJava(declarationDescriptor, element, originalElement)
                    }
                }
            }
            else {
                // This element was resolved to non-kotlin element, it will be rendered with own provider
            }

            return null
        }

        private fun renderKotlinDeclaration(declaration: JetDeclaration, quickNavigation: Boolean): String {
            val context = declaration.analyze(BodyResolveMode.PARTIAL)
            val declarationDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]

            if (declarationDescriptor == null) {
                LOG.info("Failed to find descriptor for declaration " + declaration.getElementTextWithContext())
                return "No documentation available"
            }

            var renderedDecl = quickDocNameFormat.render(declarationDescriptor)
            if (!quickNavigation) {
                renderedDecl = "<pre>" + renderedDecl + "</pre>"
            }
            val comment = KDocFinder.findKDoc(declarationDescriptor)
            if (comment != null) {
                renderedDecl = renderedDecl + "<br/>" + KDocRenderer.renderKDoc(comment)
            }

            return renderedDecl
        }

        private fun mixKotlinToJava(declarationDescriptor: DeclarationDescriptor, element: PsiElement, originalElement: PsiElement?): String? {
            val originalInfo = JavaDocumentationProvider().getQuickNavigateInfo(element, originalElement)
            if (originalInfo != null) {
                val renderedDecl = DescriptorRenderer.HTML_NAMES_WITH_SHORT_TYPES.render(declarationDescriptor)
                return renderedDecl + "<br/>Java declaration:<br/>" + originalInfo
            }

            return null
        }
    }
}
