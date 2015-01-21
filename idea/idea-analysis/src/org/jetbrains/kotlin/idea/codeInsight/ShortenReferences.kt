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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.idea.quickfix.ImportInsertHelper;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import java.util.HashSet;
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.Collections
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import java.util.LinkedHashSet
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

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

    fun processAllReferencesInFile(descriptors: List<DeclarationDescriptor>, file: JetFile) {
        val renderedDescriptors = descriptors.map { it.asString() }
        val referenceToContext = resolveReferencesInFile(file).filter { e ->
            val (ref, context) = e
            val renderedTargets = ref.getTargets(context).map { it.asString() }
            ref is JetSimpleNameExpression && renderedDescriptors.any { it in renderedTargets }
        }
        shortenReferencesInFile(
                file,
                referenceToContext.keySet().map { (it as JetSimpleNameExpression).getQualifiedElement() },
                referenceToContext,
                { FilterResult.PROCESS }
        )
    }

    private enum class FilterResult {
        SKIP
        GO_INSIDE
        PROCESS
    }

    private fun resolveReferencesInFile(
            file: JetFile,
            fileElements: List<JetElement> = Collections.singletonList(file)
    ): Map<JetReferenceExpression, BindingContext> {
        ImportInsertHelper.INSTANCE.optimizeImportsOnTheFly(file)
        return JetFileReferencesResolver.resolve(file, fileElements, resolveShortNames = false)
    }

    private fun shortenReferencesInFile(
            file: JetFile,
            elements: List<JetElement>,
            referenceToContext: Map<JetReferenceExpression, BindingContext>,
            elementFilter: (PsiElement) -> FilterResult
    ) {
        val importInserter = ImportInserter(file)
        
        processElements(elements, ShortenTypesVisitor(file, elementFilter, referenceToContext, importInserter))
        processElements(elements, ShortenQualifiedExpressionsVisitor(file, elementFilter, referenceToContext, importInserter))
    }

    private fun process(elements: Iterable<JetElement>, elementFilter: (PsiElement) -> FilterResult) {
        for ((file, fileElements) in elements.groupBy { element -> element.getContainingJetFile() }) {
            // first resolve all qualified references - optimization
            val referenceToContext = resolveReferencesInFile(file, fileElements)
            shortenReferencesInFile(file, fileElements, referenceToContext, elementFilter)
        }
    }

    private fun processElements(elements: Iterable<JetElement>, visitor: ShorteningVisitor<*>) {
        for (element in elements) {
            element.accept(visitor)
        }
        visitor.finish()
    }

    private abstract class ShorteningVisitor<T : JetElement>(
            val file: JetFile,
            val elementFilter: (PsiElement) -> FilterResult,
            val resolveMap: Map<JetReferenceExpression, BindingContext>,
            val importInserter: ImportInserter) : JetVisitorVoid() {
        protected val resolutionFacade: ResolutionFacade
            get() = file.getResolutionFacade()

        protected val elementsToShorten: MutableSet<T> = LinkedHashSet()

        protected fun bindingContext(element: JetElement): BindingContext
                = resolveMap[element] ?: resolutionFacade.analyze(element)

        protected abstract fun getShortenedElement(element: T): JetElement?

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        public fun finish() {
            for (element in elementsToShorten) {
                getShortenedElement(element)?.let { element.replace(it) }
            }
        }
    }

    private class ShortenTypesVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            resolveMap: Map<JetReferenceExpression, BindingContext>,
            importInserter: ImportInserter
    ) : ShorteningVisitor<JetUserType>(file, elementFilter, resolveMap, importInserter) {
        private fun canShortenType(userType: JetUserType): Boolean {
            if (userType.getQualifier() == null) return false
            val referenceExpression = userType.getReferenceExpression()
            if (referenceExpression == null) return false

            val target = bindingContext(referenceExpression)[BindingContext.REFERENCE_TARGET, referenceExpression]?.let { desc ->
                if (desc is ConstructorDescriptor) desc.getContainingDeclaration() else desc
            }
            if (target == null) return false

            val typeReference = userType.getStrictParentOfType<JetTypeReference>()!!
            val scope = resolutionFacade.analyze(typeReference)[BindingContext.TYPE_RESOLUTION_SCOPE, typeReference]!!
            val name = target.getName()
            val targetByName = scope.getClassifier(name)
            if (targetByName == null) {
                return importInserter.addImport(target)
            }
            else if (target.asString() == targetByName.asString()) {
                return true
            }
            else {
                if (importInserter.optimizeImports()) {
                    return canShortenType(userType) // if we have optimized imports then try again
                }

                // leave FQ name
                return false
            }
        }

        override fun visitUserType(userType: JetUserType) {
            val filterResult = elementFilter(userType)
            if (filterResult == FilterResult.SKIP) return

            userType.getTypeArgumentList()?.accept(this)

            if (filterResult == FilterResult.PROCESS && canShortenType(userType)) {
                elementsToShorten.add(userType)
            }
            else{
                userType.getQualifier()?.accept(this)
            }
        }

        override fun getShortenedElement(element: JetUserType): JetElement? {
            val referenceExpression = element.getReferenceExpression() ?: return null
            val typeArgumentList = element.getTypeArgumentList()
            val text = referenceExpression.getText() + (if (typeArgumentList != null) typeArgumentList.getText() else "")
            return JetPsiFactory(element).createType(text).getTypeElement()!!
        }
    }

    private class ShortenQualifiedExpressionsVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            resolveMap: Map<JetReferenceExpression, BindingContext>,
            importInserter: ImportInserter
    ) : ShorteningVisitor<JetQualifiedExpression>(file, elementFilter, resolveMap, importInserter) {
        private fun canShorten(qualifiedExpression: JetDotQualifiedExpression): Boolean {
            val context = bindingContext(qualifiedExpression)

            if (context[BindingContext.QUALIFIER, qualifiedExpression.getReceiverExpression()] == null) return false

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) != null) return false

            val selector = qualifiedExpression.getSelectorExpression() ?: return false
            val callee = selector.getCalleeExpressionIfAny() as? JetReferenceExpression ?: return false
            val targetBefore = callee.getTargets(context).singleOrNull() ?: return false

            val scope = context[BindingContext.RESOLUTION_SCOPE, qualifiedExpression] ?: return false
            val selectorCopy = selector.copy() as JetReferenceExpression
            val newContext = selectorCopy.analyzeInContext(scope)
            val targetsAfter = (selectorCopy.getCalleeExpressionIfAny() as JetReferenceExpression).getTargets(newContext)

            when (targetsAfter.size()) {
                0 -> return importInserter.addImport(targetBefore)

                1 -> if (targetBefore == targetsAfter.first()) return true
            }

            if (importInserter.optimizeImports()) {
                return canShorten(qualifiedExpression) // if we have optimized imports then try again
            }

            return false
        }

        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            val filterResult = elementFilter(expression)
            if (filterResult == FilterResult.SKIP) return

            expression.getSelectorExpression()?.acceptChildren(this)

            if (filterResult == FilterResult.PROCESS && canShorten(expression)) {
                elementsToShorten.add(expression)
            }
            else {
                expression.getReceiverExpression().accept(this)
            }
        }

        override fun getShortenedElement(element: JetQualifiedExpression): JetElement = element.getSelectorExpression()!!
    }

    private fun DeclarationDescriptor.asString()
            = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

    private fun JetReferenceExpression.getTargets(context: BindingContext): Collection<DeclarationDescriptor> {
        return context[BindingContext.REFERENCE_TARGET, this]?.let { Collections.singletonList(it.getImportableDescriptor()) }
               ?: context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this]?.mapTo(HashSet<DeclarationDescriptor>()) { it.getImportableDescriptor() }
               ?: Collections.emptyList()
    }

    // this class is needed to optimize imports only when we actually insert any import (optimization)
    private class ImportInserter(val file: JetFile) {
        private var optimizeImports = true

        fun addImport(target: DeclarationDescriptor): Boolean {
            val realTarget = if (DescriptorUtils.isClassObject(target)) // references to class object are treated as ones to its owner class
                target.getContainingDeclaration() as? ClassDescriptor ?: return false
            else
                target

            if (realTarget !is ClassDescriptor && realTarget !is PackageViewDescriptor) return false
            if (realTarget.getContainingDeclaration() is ClassDescriptor) return false // do not insert imports for nested classes

            optimizeImports()
            ImportInsertHelper.INSTANCE.writeImportToFile(ImportPath(DescriptorUtils.getFqNameSafe(realTarget), false), file)
            return true
        }

        fun optimizeImports(): Boolean {
            if (!optimizeImports) return false
            optimizeImports = false
            return ImportInsertHelper.INSTANCE.optimizeImportsOnTheFly(file)
        }
    }
}
