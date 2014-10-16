/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import java.util.HashSet;
import com.intellij.psi.util.PsiTreeUtil
import java.util.ArrayList
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.renderer.DescriptorRenderer.FQ_NAMES_IN_TYPES
import org.jetbrains.jet.lang.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import java.util.Collections
import org.jetbrains.jet.analyzer.analyzeInContext
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCalleeExpressionIfAny

public object ShortenReferences {
    public fun process(element: JetElement) {
        process(listOf(element))
    }

    public fun process(elements: Iterable<JetElement>) {
        process(elements, { FilterResult.PROCESS })
    }

    public fun process(file: JetFile, startOffset: Int, endOffset: Int) {
        val documentManager = PsiDocumentManager.getInstance(file.getProject())
        val document = documentManager.getDocument(file)!!
        if (!documentManager.isCommitted(document)) {
            throw IllegalStateException("Document should be committed to shorten references in range")
        }

        val rangeMarker = document.createRangeMarker(startOffset, endOffset)
        rangeMarker.setGreedyToLeft(true)
        rangeMarker.setGreedyToRight(true)
        try {
            process(listOf(file), { element ->
                if (rangeMarker.isValid()) {
                    val range = TextRange(rangeMarker.getStartOffset(), rangeMarker.getEndOffset())

                    val elementRange = element.getTextRange()!!
                    when {
                        range.contains(elementRange) -> FilterResult.PROCESS

                        range.intersects(elementRange) -> {
                            // for qualified call expression allow to shorten only the part without parenthesis
                            val calleeExpression = ((element as? JetDotQualifiedExpression)
                                    ?.getSelectorExpression() as? JetCallExpression)
                                    ?.getCalleeExpression()
                            if (calleeExpression != null) {
                                val rangeWithoutParenthesis = TextRange(elementRange.getStartOffset(), calleeExpression.getTextRange()!!.getEndOffset())
                                if (range.contains(rangeWithoutParenthesis)) FilterResult.PROCESS else FilterResult.GO_INSIDE
                            }
                            else {
                                FilterResult.GO_INSIDE
                            }
                        }

                        else -> FilterResult.SKIP
                    }
                }
                else {
                    FilterResult.SKIP
                }
            })
        }
        finally {
            rangeMarker.dispose()
        }
    }

    private enum class FilterResult {
        SKIP
        GO_INSIDE
        PROCESS
    }

    private fun process(elements: Iterable<JetElement>, elementFilter: (PsiElement) -> FilterResult) {
        for ((file, fileElements) in elements.groupBy { element -> element.getContainingJetFile() }) {
            ImportInsertHelper.getInstance().optimizeImportsOnTheFly(file)

            // first resolve all qualified references - optimization
            val referenceToContext = JetFileReferencesResolver.resolve(file, fileElements, resolveShortNames = false)

            val shortenTypesVisitor = ShortenTypesVisitor(file, elementFilter, referenceToContext)
            processElements(fileElements, shortenTypesVisitor)
            shortenTypesVisitor.finish()

            processElements(fileElements, ShortenQualifiedExpressionsVisitor(file, elementFilter, referenceToContext))
        }
    }

    private fun processElements(elements: Iterable<JetElement>, visitor: JetVisitorVoid) {
        for (element in elements) {
            element.accept(visitor)
        }
    }

    private class ShortenTypesVisitor(val file: JetFile,
                                      val elementFilter: (PsiElement) -> FilterResult,
                                      val resolveMap: Map<JetReferenceExpression, BindingContext>) : JetVisitorVoid() {
        private val resolveSession : ResolveSessionForBodies
            get() = file.getLazyResolveSession()

        private val typesToShorten = ArrayList<JetUserType>()

        public fun finish() {
            for (userType in typesToShorten) {
                shortenType(userType)
            }
        }

        private fun bindingContext(expression: JetReferenceExpression): BindingContext = resolveMap[expression]!!

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        override fun visitUserType(userType: JetUserType) {
            val filterResult = elementFilter(userType)
            if (filterResult == FilterResult.SKIP) return

            userType.getTypeArgumentList()?.accept(this)

            if (filterResult == FilterResult.PROCESS && canShortenType(userType)) {
                typesToShorten.add(userType)
            }
            else{
                userType.getQualifier()?.accept(this)
            }
        }

        private fun canShortenType(userType: JetUserType): Boolean {
            if (userType.getQualifier() == null) return false
            val referenceExpression = userType.getReferenceExpression()
            if (referenceExpression == null) return false

            val target = bindingContext(referenceExpression)[BindingContext.REFERENCE_TARGET, referenceExpression]?.let { desc ->
                if (desc is ConstructorDescriptor) desc.getContainingDeclaration() else desc
            }
            if (target == null) return false

            val typeReference = PsiTreeUtil.getParentOfType(userType, javaClass<JetTypeReference>())!!
            val scope = resolveSession.resolveToElement(typeReference)[BindingContext.TYPE_RESOLUTION_SCOPE, typeReference]!!
            val name = target.getName()
            val targetByName = scope.getClassifier(name)
            if (targetByName == null) {
                if (target.getContainingDeclaration() is ClassDescriptor) return false

                addImport(target, file)
                return true
            }
            else if (target.asString() == targetByName.asString()) {
                return true
            }
            else {
                // leave FQ name
                return false
            }
        }

        private fun shortenType(userType: JetUserType) {
            val referenceExpression = userType.getReferenceExpression()
            if (referenceExpression == null) return
            val typeArgumentList = userType.getTypeArgumentList()
            val text = referenceExpression.getText() + (if (typeArgumentList != null) typeArgumentList.getText() else "")
            val newUserType = JetPsiFactory(userType).createType(text).getTypeElement()!!
            userType.replace(newUserType)
        }
    }

    private class ShortenQualifiedExpressionsVisitor(val file: JetFile,
                                                     val elementFilter: (PsiElement) -> FilterResult,
                                                     val resolveMap: Map<JetReferenceExpression, BindingContext>) : JetVisitorVoid() {
        private val resolveSession : ResolveSessionForBodies
            get() = file.getLazyResolveSession()

        private fun bindingContext(element: JetElement): BindingContext
                = resolveMap[element] ?: resolveSession.resolveToElement(element) // binding context can be absent in the map if some references have been shortened already

        private fun JetReferenceExpression.getTargets(context: BindingContext): Collection<DeclarationDescriptor> {
            return context[BindingContext.REFERENCE_TARGET, this]?.let { Collections.singletonList(adjustDescriptor(it)) }
                   ?: context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this]?.mapTo(HashSet<DeclarationDescriptor>()) { adjustDescriptor(it) }
                   ?: Collections.emptyList()
        }

        private fun adjustDescriptor(it: DeclarationDescriptor): DeclarationDescriptor {
            return (it as? ConstructorDescriptor)?.getContainingDeclaration() ?: it
        }

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                acceptChildren(element)
            }
        }

        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            val filterResult = elementFilter(expression)
            val resultElement = if (filterResult == FilterResult.PROCESS) processDotQualifiedExpression(expression) else expression
            if (filterResult != FilterResult.SKIP) {
                acceptChildren(resultElement)
            }
        }

        private fun JetQualifiedExpression.doShorten(): JetExpression = replace(getSelectorExpression()!!) as JetExpression

        private fun processDotQualifiedExpression(qualifiedExpression: JetDotQualifiedExpression): PsiElement {
            val context = bindingContext(qualifiedExpression)

            if (context[BindingContext.QUALIFIER, qualifiedExpression.getReceiverExpression()] == null) return qualifiedExpression

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) != null) return qualifiedExpression

            val selector = qualifiedExpression.getSelectorExpression() ?: return qualifiedExpression
            val callee = selector.getCalleeExpressionIfAny() as? JetReferenceExpression ?: return qualifiedExpression
            val targetBefore = callee.getTargets(context).singleOrNull() ?: return qualifiedExpression
            val isClassMember = targetBefore.getContainingDeclaration() is ClassDescriptor
            val isClassOrPackage = targetBefore is ClassDescriptor || targetBefore is PackageViewDescriptor

            val scope = context[BindingContext.RESOLUTION_SCOPE, qualifiedExpression] ?: return qualifiedExpression
            val selectorCopy = selector.copy() as JetReferenceExpression
            val newContext = selectorCopy.analyzeInContext(scope)
            val targetsAfter = (selectorCopy.getCalleeExpressionIfAny() as JetReferenceExpression).getTargets(newContext)

            return when (targetsAfter.size) {
                0 -> {
                    if (!isClassMember && isClassOrPackage) {
                        addImport(targetBefore, file)
                        qualifiedExpression.doShorten()
                    }
                    else {
                        qualifiedExpression
                    }
                }

                1 -> if (targetBefore == targetsAfter.first()) qualifiedExpression.doShorten() else qualifiedExpression

                else -> qualifiedExpression
            }
        }

        // we do not use standard PsiElement.acceptChildren because it won't work correctly if the element is replaced by the visitor
        private fun acceptChildren(element: PsiElement) {
            var child = element.getFirstChild()
            while(child != null) {
                val nextChild = child!!.getNextSibling()
                child!!.accept(this)
                child = nextChild
            }
        }
    }

    private fun DeclarationDescriptor.asString()
            = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

    private fun addImport(descriptor: DeclarationDescriptor, file: JetFile) {
        ImportInsertHelper.getInstance().writeImportToFile(ImportPath(DescriptorUtils.getFqNameSafe(descriptor), false), file)
    }
}
