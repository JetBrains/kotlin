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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import java.util.Collections
import org.jetbrains.jet.analyzer.analyzeInContext
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCalleeExpressionIfAny
import java.util.LinkedHashSet
import org.jetbrains.jet.lang.resolve.ImportPath

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
            val importInserter = ImportInserter(file)

            // first resolve all qualified references - optimization
            val referenceToContext = JetFileReferencesResolver.resolve(file, fileElements, resolveShortNames = false)

            processElements(fileElements, ShortenTypesVisitor(file, elementFilter, referenceToContext, importInserter))
            processElements(fileElements, ShortenQualifiedExpressionsVisitor(file, elementFilter, referenceToContext, importInserter))
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
        protected val resolveSession: ResolveSessionForBodies
            get() = file.getLazyResolveSession()

        protected val elementsToShorten: MutableSet<T> = LinkedHashSet()

        protected fun bindingContext(element: JetElement): BindingContext
                = resolveMap[element] ?: resolveSession.resolveToElement(element)

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

            val typeReference = PsiTreeUtil.getParentOfType(userType, javaClass<JetTypeReference>())!!
            val scope = resolveSession.resolveToElement(typeReference)[BindingContext.TYPE_RESOLUTION_SCOPE, typeReference]!!
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
        private fun adjustDescriptor(it: DeclarationDescriptor): DeclarationDescriptor {
            return (it as? ConstructorDescriptor)?.getContainingDeclaration() ?: it
        }

        private fun JetReferenceExpression.getTargets(context: BindingContext): Collection<DeclarationDescriptor> {
            return context[BindingContext.REFERENCE_TARGET, this]?.let { Collections.singletonList(adjustDescriptor(it)) }
                   ?: context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this]?.mapTo(HashSet<DeclarationDescriptor>()) { adjustDescriptor(it) }
                   ?: Collections.emptyList()
        }

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

            when (targetsAfter.size) {
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
            ImportInsertHelper.getInstance().writeImportToFile(ImportPath(DescriptorUtils.getFqNameSafe(realTarget), false), file)
            return true
        }

        fun optimizeImports(): Boolean {
            if (!optimizeImports) return false
            optimizeImports = false
            return ImportInsertHelper.getInstance().optimizeImportsOnTheFly(file)
        }
    }
}
