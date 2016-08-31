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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.search.*
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinRequestResultProcessor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.logPresentation
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.testLog
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.util.isValidOperator
import java.util.*

abstract class OperatorReferenceSearcher<TReferenceElement : KtElement>(
        private val targetDeclaration: KtDeclaration,
        private val searchScope: SearchScope,
        private val consumer: Processor<PsiReference>,
        private val optimizer: SearchRequestCollector,
        private val wordToSearch: String?
) {
    private val project = targetDeclaration.project

    protected abstract fun processSuspiciousExpression(expression: KtExpression)

    protected abstract fun extractReference(element: PsiElement): PsiReference?

    protected abstract fun isReferenceToCheck(ref: PsiReference): Boolean

    protected fun processReferenceElement(element: TReferenceElement): Boolean {
        val reference = extractReference(element) ?: return true
        testLog?.add("Resolved ${logPresentation(element)}")
        if (reference.isReferenceTo(targetDeclaration)) {
            return consumer.process(reference)
        }
        else {
            return true
        }
    }

    companion object {
        private object SearchesInProgress : ThreadLocal<HashSet<KtDeclaration>>() {
            override fun initialValue() = HashSet<KtDeclaration>()
        }
    }

    open fun run() {
        val inProgress = SearchesInProgress.get()
        try {
            if (!inProgress.add(targetDeclaration)) return //TODO: it's not quite correct

            val usePlainSearch = when (ExpressionsOfTypeProcessor.mode) {
                ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART -> false
                ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN -> true
                ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED -> searchScope is LocalSearchScope // for local scope it's faster to use plain search
            }
            if (usePlainSearch) {
                doPlainSearch(searchScope)
                return
            }

            val descriptor = targetDeclaration.resolveToDescriptor() as? CallableDescriptor ?: return
            if (descriptor is FunctionDescriptor && !descriptor.isValidOperator()) return

            val dataType = if (descriptor.isExtension) {
                descriptor.fuzzyExtensionReceiverType()!!
            }
            else {
                val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return
                classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
            }

            ExpressionsOfTypeProcessor(
                    dataType,
                    searchScope,
                    suspiciousExpressionHandler = { expression -> processSuspiciousExpression(expression) },
                    suspiciousScopeHandler = { searchScope -> doPlainSearch(searchScope) },
                    resolutionFacade = targetDeclaration.getResolutionFacade()
            ).run()
        }
        finally {
            inProgress.remove(targetDeclaration)
        }
    }

    private fun doPlainSearch(scope: SearchScope) {
        if (scope is LocalSearchScope) {
            for (element in scope.scope) {
                element.accept(object : PsiRecursiveElementWalkingVisitor() {
                    override fun visitElement(element: PsiElement) {
                        val reference = extractReference(element)
                        if (reference != null && reference.isReferenceTo(targetDeclaration)) {
                            consumer.process(reference)
                        }

                        super.visitElement(element)
                    }
                })
            }
        }
        else {
            scope as GlobalSearchScope
            if (wordToSearch != null) {
                val unwrappedElement = targetDeclaration.namedUnwrappedElement ?: return
                val resultProcessor = KotlinRequestResultProcessor(unwrappedElement,
                                                                   filter = { ref -> isReferenceToCheck(ref) })
                optimizer.searchWord(wordToSearch, scope.restrictToKotlinSources(), UsageSearchContext.IN_CODE, true, unwrappedElement, resultProcessor)
            }
            else {
                val psiManager = PsiManager.getInstance(project)
                ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
                    if (file in scope) {
                        val ktFile = psiManager.findFile(file) as? KtFile
                        if (ktFile != null) {
                            doPlainSearch(LocalSearchScope(ktFile))
                        }
                    }
                    true
                }
            }
        }
    }
}
