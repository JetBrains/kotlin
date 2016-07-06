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

package org.jetbrains.kotlin.idea

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.asJava.KtLightDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.kdoc.KDocRenderer
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.constant

class KotlinQuickDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return if (element == null) null else getText(element, originalElement, true)
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return getText(element, originalElement, false)
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement?): PsiElement? {
        if (context !is KtElement) {
            return null
        }

        val bindingContext = context.analyze(BodyResolveMode.PARTIAL)
        val contextDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, context] ?: return null
        val descriptors = resolveKDocLink(bindingContext, context.getResolutionFacade(),
                                          contextDescriptor, null, StringUtil.split(link, ","))
        val target = descriptors.firstOrNull() ?: return null
        return DescriptorToSourceUtilsIde.getAnyDeclaration(psiManager.project, target)
    }

    override fun getDocumentationElementForLookupItem(psiManager: PsiManager, `object`: Any?, element: PsiElement?): PsiElement? {
        if (`object` is DeclarationLookupObject) {
            return `object`.psiElement
        }
        return null
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinQuickDocumentationProvider::class.java)

        private val DESCRIPTOR_RENDERER = DescriptorRenderer.HTML.withOptions {
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            renderCompanionObjectName = true
        }

        private fun getText(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean): String? {
            if (element is KtDeclaration) {
                return renderKotlinDeclaration(element, quickNavigation)
            }
            else if (element is KtNameReferenceExpression && element.getReferencedName() == "it") {
                return renderKotlinImplicitLambdaParameter(element, quickNavigation)
            }
            else if (element is KtLightDeclaration<*, *>) {
                val origin = element.kotlinOrigin ?: return null
                return renderKotlinDeclaration(origin, quickNavigation)
            }

            if (quickNavigation) {
                val referenceExpression = originalElement?.getNonStrictParentOfType<KtReferenceExpression>()
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

        private fun renderKotlinDeclaration(declaration: KtExpression, quickNavigation: Boolean): String {
            val context = declaration.analyze(BodyResolveMode.PARTIAL)
            val declarationDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]

            if (declarationDescriptor == null) {
                LOG.info("Failed to find descriptor for declaration " + declaration.getElementTextWithContext())
                return "No documentation available"
            }

            return renderKotlin(context, declarationDescriptor, quickNavigation)
        }

        private fun renderKotlinImplicitLambdaParameter(element: KtReferenceExpression, quickNavigation: Boolean): String? {
            val context = element.analyze(BodyResolveMode.PARTIAL)
            val target = element.mainReference.resolveToDescriptors(context).singleOrNull() as? ValueParameterDescriptor? ?: return null
            return renderKotlin(context, target, quickNavigation)
        }

        private fun renderKotlin(context: BindingContext, declarationDescriptor: DeclarationDescriptor, quickNavigation: Boolean): String {
            @Suppress("NAME_SHADOWING")
            var declarationDescriptor = declarationDescriptor
            if (declarationDescriptor is ValueParameterDescriptor) {
                val property = context[BindingContext.VALUE_PARAMETER_AS_PROPERTY, declarationDescriptor]
                if (property != null) {
                    declarationDescriptor = property
                }
            }

            var renderedDecl = DESCRIPTOR_RENDERER.render(declarationDescriptor)
            if (!quickNavigation) {
                renderedDecl = "<pre>$renderedDecl</pre>"
            }
            val comment = declarationDescriptor.findKDoc()
            if (comment != null) {
                val renderedComment = KDocRenderer.renderKDoc(comment)
                if (renderedComment.startsWith("<p>")) {
                    renderedDecl += renderedComment
                }
                else {
                    renderedDecl = "$renderedDecl<br/>$renderedComment"
                }
            }

            return renderedDecl
        }

        private fun mixKotlinToJava(declarationDescriptor: DeclarationDescriptor, element: PsiElement, originalElement: PsiElement?): String? {
            val originalInfo = JavaDocumentationProvider().getQuickNavigateInfo(element, originalElement)
            if (originalInfo != null) {
                val renderedDecl = constant { DESCRIPTOR_RENDERER.withOptions { withDefinedIn = false } }.render(declarationDescriptor)
                return renderedDecl + "<br/>Java declaration:<br/>" + originalInfo
            }

            return null
        }
    }
}
