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

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.idea.util.ImportInsertHelper;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import java.util.LinkedHashSet
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.util.ArrayList

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
            shortenReferencesInFile(file, fileElements, elementFilter)
        }
    }

    private fun shortenReferencesInFile(
            file: JetFile,
            elements: List<JetElement>,
            elementFilter: (PsiElement) -> FilterResult
    ) {
        val importInserter = ImportInserter(file)

        val failedToImportDescriptors = LinkedHashSet<DeclarationDescriptor>()

        while (true) {
            val visitor1 = ShortenTypesVisitor(file, elementFilter, failedToImportDescriptors)
            val visitor2 = ShortenQualifiedExpressionsVisitor(file, elementFilter, failedToImportDescriptors)

            val descriptorsToImport1 = analyzeReferences(elements, visitor1)
            val descriptorsToImport2 = analyzeReferences(elements, visitor2)

            visitor1.shortenElements()
            visitor2.shortenElements()

            if (descriptorsToImport1.isEmpty() && descriptorsToImport2.isEmpty()) break

            val descriptorsToImport = (descriptorsToImport1 + descriptorsToImport2).toSet()
            var anyChange = false
            for (descriptor in descriptorsToImport) {
                assert(descriptor !in failedToImportDescriptors)

                val result = importInserter.addImport(descriptor)
                if (result != ImportInsertHelper.ImportDescriptorResult.ALREADY_IMPORTED) {
                    anyChange = true
                }
                if (result == ImportInsertHelper.ImportDescriptorResult.FAIL) {
                    failedToImportDescriptors.add(descriptor)
                }
            }
            if (!anyChange) break
        }
    }

    private fun analyzeReferences(elements: Iterable<JetElement>, visitor: ShorteningVisitor<*>): Set<DeclarationDescriptor> {
        for (element in elements) {
            element.accept(visitor)
        }
        return visitor.getDescriptorsToImport()
    }

    private abstract class ShorteningVisitor<T : JetElement>(
            protected val file: JetFile,
            protected val elementFilter: (PsiElement) -> FilterResult,
            protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : JetVisitorVoid() {
        private val elementsToShorten = ArrayList<T>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        protected val resolutionFacade: ResolutionFacade = file.getResolutionFacade()

        protected fun processQualifiedElement(element: T, target: DeclarationDescriptor, canShortenNow: Boolean) {
            if (canShortenNow) {
                elementsToShorten.add(element)
            }
            else if (target !in failedToImportDescriptors && mayImport(target, file)) {
                descriptorsToImport.add(target)
            }
            else {
                qualifier(element).accept(this)
            }
        }

        protected abstract fun qualifier(element: T): JetElement

        protected abstract fun shortenElement(element: T)

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        public fun shortenElements() {
            for (element in elementsToShorten) {
                assert (element.isValid())
                shortenElement(element)
            }
        }

        public fun getDescriptorsToImport(): Set<DeclarationDescriptor> = descriptorsToImport
    }

    private class ShortenTypesVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<JetUserType>(file, elementFilter, failedToImportDescriptors) {

        override fun visitUserType(userType: JetUserType) {
            val filterResult = elementFilter(userType)
            if (filterResult == FilterResult.SKIP) return

            userType.getTypeArgumentList()?.accept(this)

            if (filterResult == FilterResult.PROCESS) {
                processType(userType)
            }
            else{
                userType.getQualifier()?.accept(this)
            }
        }

        private fun processType(type: JetUserType) {
            if (type.getQualifier() == null) return
            val referenceExpression = type.getReferenceExpression() ?: return

            val bindingContext = resolutionFacade.analyze(referenceExpression)
            val target = referenceExpression.targets(bindingContext).singleOrNull() ?: return

            val typeReference = type.getStrictParentOfType<JetTypeReference>()!!
            val scope = bindingContext[BindingContext.TYPE_RESOLUTION_SCOPE, typeReference]!!
            val name = target.getName()
            val targetByName = if (target is ClassifierDescriptor) scope.getClassifier(name) else scope.getPackage(name)
            val canShortenNow = targetByName?.asString() == target.asString()

            processQualifiedElement(type, target, canShortenNow)
        }

        override fun qualifier(element: JetUserType) = element.getQualifier()!!

        override fun shortenElement(element: JetUserType) {
            element.deleteQualifier()
        }
    }

    private class ShortenQualifiedExpressionsVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<JetQualifiedExpression>(file, elementFilter, failedToImportDescriptors) {

        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            val filterResult = elementFilter(expression)
            if (filterResult == FilterResult.SKIP) return

            expression.getSelectorExpression()?.acceptChildren(this)

            if (filterResult == FilterResult.PROCESS) {
                if (process(expression)) return
            }

            expression.getReceiverExpression().accept(this)
        }

        private fun process(qualifiedExpression: JetDotQualifiedExpression): Boolean {
            val bindingContext = resolutionFacade.analyze(qualifiedExpression)

            if (bindingContext[BindingContext.QUALIFIER, qualifiedExpression.getReceiverExpression()] == null) return false

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) != null) return true

            val selector = qualifiedExpression.getSelectorExpression() ?: return false
            val callee = selector.getCalleeExpressionIfAny() as? JetReferenceExpression ?: return false
            val target = callee.targets(bindingContext).singleOrNull() ?: return false

            val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, qualifiedExpression] ?: return false
            val selectorCopy = selector.copy() as JetReferenceExpression
            val newContext = selectorCopy.analyzeInContext(scope)
            val targetsWhenShort = (selectorCopy.getCalleeExpressionIfAny() as JetReferenceExpression).targets(newContext)

            val canShortenNow = targetsWhenShort.singleOrNull()?.asString() == target.asString()

            if (!canShortenNow && targetsWhenShort.any { it !is ClassDescriptor && it !is PackageViewDescriptor }) {
                // it makes no sense to insert import when there is a conflict with function, property etc
                return false
            }

            processQualifiedElement(qualifiedExpression, target, canShortenNow)
            return true
        }

        override fun qualifier(element: JetQualifiedExpression) = element.getReceiverExpression()

        override fun shortenElement(element: JetQualifiedExpression) {
            element.replace(element.getSelectorExpression()!!)
        }
    }

    private fun DeclarationDescriptor.asString()
            = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

    private fun JetReferenceExpression.targets(context: BindingContext): Collection<DeclarationDescriptor> {
        return context[BindingContext.REFERENCE_TARGET, this]?.let { listOf(it.getImportableDescriptor()) }
               ?: context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this]?.map { it.getImportableDescriptor() }?.toSet()
               ?: listOf()
    }

    private fun mayImport(descriptor: DeclarationDescriptor, file: JetFile): Boolean {
        if (descriptor !is ClassDescriptor && descriptor !is PackageViewDescriptor) return false
        return ImportInsertHelper.getInstance(file.getProject()).mayImportByCodeStyle(descriptor)
    }

    // this class is needed to optimize imports only when we actually insert any import (optimization)
    private class ImportInserter(val file: JetFile) {
        private var optimizeImports = true
        private val helper = ImportInsertHelper.getInstance(file.getProject())

        fun addImport(target: DeclarationDescriptor): ImportInsertHelper.ImportDescriptorResult {
            optimizeImports()
            return helper.importDescriptor(file, target)
        }

        fun optimizeImports(): Boolean {
            if (!optimizeImports) return false
            optimizeImports = false
            return helper.optimizeImportsOnTheFly(file)
        }
    }
}
