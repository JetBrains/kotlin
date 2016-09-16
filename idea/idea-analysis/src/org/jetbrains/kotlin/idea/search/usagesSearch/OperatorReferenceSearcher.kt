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

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getJavaOrKotlinMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinRequestResultProcessor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.logPresentation
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.testLog
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.dataClassUtils.getComponentIndex
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

abstract class OperatorReferenceSearcher<TReferenceElement : KtElement>(
        protected val targetDeclaration: PsiElement,
        private val searchScope: SearchScope,
        private val consumer: Processor<PsiReference>,
        private val optimizer: SearchRequestCollector,
        private val wordsToSearch: List<String>
) {
    private val project = targetDeclaration.project

    /**
     * Invoked for all expressions that may have type matching receiver type of our operator
     */
    protected abstract fun processPossibleReceiverExpression(expression: KtExpression)

    /**
     * Extract reference that may resolve to our operator (no actual resolve to be performed)
     */
    protected abstract fun extractReference(element: PsiElement): PsiReference?

    /**
     * Check if reference may potentially resolve to our operator (no actual resolve to be performed)
     */
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
        fun create(
                declaration: PsiElement,
                searchScope: SearchScope,
                consumer: Processor<PsiReference>,
                optimizer: SearchRequestCollector,
                options: KotlinReferencesSearchOptions
        ): OperatorReferenceSearcher<*>? {
            return runReadAction {
                if (declaration.isValid)
                    createInReadAction(declaration, searchScope, consumer, optimizer, options)
                else
                    null
            }
        }

        private fun createInReadAction(
                declaration: PsiElement,
                searchScope: SearchScope,
                consumer: Processor<PsiReference>,
                optimizer: SearchRequestCollector,
                options: KotlinReferencesSearchOptions
        ): OperatorReferenceSearcher<*>? {
            val functionName =  when (declaration) {
                is KtNamedFunction -> declaration.name
                is PsiMethod -> declaration.name
                else -> null
            } ?: return null

            if (!Name.isValidIdentifier(functionName)) return null
            val name = Name.identifier(functionName)

            val declarationToUse = if (declaration is KtLightMethod) {
                declaration.kotlinOrigin ?: return null
            }
            else {
                declaration
            }

            return createInReadAction(declarationToUse, name, consumer, optimizer, options, searchScope)
        }

        private fun createInReadAction(
                declaration: PsiElement,
                name: Name,
                consumer: Processor<PsiReference>,
                optimizer: SearchRequestCollector,
                options: KotlinReferencesSearchOptions,
                searchScope: SearchScope
        ): OperatorReferenceSearcher<*>? {
            if (isComponentLike(name)) {
                if (!options.searchForComponentConventions) return null
                val componentIndex = getComponentIndex(name.asString())
                return DestructuringDeclarationReferenceSearcher(declaration, componentIndex, searchScope, consumer, optimizer)
            }

            if (!options.searchForOperatorConventions) return null

            val binaryOp = OperatorConventions.BINARY_OPERATION_NAMES.inverse()[name]
            val assignmentOp = OperatorConventions.ASSIGNMENT_OPERATIONS.inverse()[name]
            val unaryOp = OperatorConventions.UNARY_OPERATION_NAMES.inverse()[name]

            when {
                binaryOp != null -> {
                    val counterpartAssignmentOp = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.inverse()[binaryOp]
                    val operationTokens = listOf(binaryOp, counterpartAssignmentOp).filterNotNull()
                    return BinaryOperatorReferenceSearcher(declaration, operationTokens, searchScope, consumer, optimizer)
                }

                assignmentOp != null -> return BinaryOperatorReferenceSearcher(declaration, listOf(assignmentOp), searchScope, consumer, optimizer)

                unaryOp != null -> return UnaryOperatorReferenceSearcher(declaration, unaryOp, searchScope, consumer, optimizer)

                name == OperatorNameConventions.INVOKE -> return InvokeOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer)

                name == OperatorNameConventions.GET -> return IndexingOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, isSet = false)

                name == OperatorNameConventions.SET -> return IndexingOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, isSet = true)

                name == OperatorNameConventions.CONTAINS -> return ContainsOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer)

                else -> return null
            }

        }

        //TODO: check no light elements here
        private object SearchesInProgress : ThreadLocal<HashSet<PsiElement>>() {
            override fun initialValue() = HashSet<PsiElement>()
        }
    }

    open protected fun resolveTargetToDescriptor(): FunctionDescriptor? {
        return when (targetDeclaration) {
            is KtDeclaration -> targetDeclaration.resolveToDescriptor()
            is PsiMember -> targetDeclaration.getJavaOrKotlinMemberDescriptor()
            else -> null
        }  as? FunctionDescriptor
    }

    fun run() {
        val inProgress = SearchesInProgress.get()
        if (!inProgress.add(targetDeclaration)) return //TODO: it's not quite correct

        try {
            val receiverType = runReadAction { extractReceiverType() } ?: return

            ExpressionsOfTypeProcessor(
                    receiverType,
                    searchScope,
                    project,
                    possibleMatchHandler = { expression -> processPossibleReceiverExpression(expression) },
                    possibleMatchesInScopeHandler = { searchScope -> doPlainSearch(searchScope) }
            ).run()
        }
        finally {
            inProgress.remove(targetDeclaration)
        }
    }

    private fun extractReceiverType(): FuzzyType? {
        val descriptor = resolveTargetToDescriptor()?.check { it.isValidOperator() } ?: return null

        return if (descriptor.isExtension) {
            descriptor.fuzzyExtensionReceiverType()!!
        }
        else {
            val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return null
            classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
        }
    }

    private fun doPlainSearch(scope: SearchScope) {
        testLog?.add("Used plain search of ${logPresentation(targetDeclaration)} in ${scope.logPresentation()}")

        if (scope is LocalSearchScope) {
            for (element in scope.scope) {
                runReadAction {
                    if (element.isValid) {
                        element.accept(object : PsiRecursiveElementWalkingVisitor() {
                            override fun visitElement(element: PsiElement) {
                                //TODO: resolve of multiple references at once
                                val reference = extractReference(element)
                                if (reference != null && reference.isReferenceTo(targetDeclaration)) {
                                    consumer.process(reference)
                                }

                                super.visitElement(element)
                            }
                        })
                    }
                }
            }
        }
        else {
            scope as GlobalSearchScope
            if (wordsToSearch.isNotEmpty()) {
                val unwrappedElement = targetDeclaration.namedUnwrappedElement ?: return
                val resultProcessor = KotlinRequestResultProcessor(unwrappedElement,
                                                                   filter = { ref -> isReferenceToCheck(ref) })
                wordsToSearch.forEach {
                    optimizer.searchWord(it, scope.restrictToKotlinSources(), UsageSearchContext.IN_CODE, true, unwrappedElement, resultProcessor)
                }
            }
            else {
                val psiManager = PsiManager.getInstance(project)
                // we must unwrap progress indicator because ProgressWrapper does not do anything on changing text and fraction
                val progress = ProgressWrapper.unwrap(ProgressIndicatorProvider.getGlobalProgressIndicator())
                progress?.pushState()
                progress?.text = "Searching implicit usages..."

                try {
                    val files = runReadAction { FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope) }
                    for ((index, file) in files.withIndex()) {
                        progress?.checkCanceled()
                        runReadAction {
                            if (file.isValid) {
                                progress?.fraction = index / files.size.toDouble()
                                progress?.text2 = file.path
                                val psiFile = psiManager.findFile(file) as? KtFile
                                if (psiFile != null) {
                                    doPlainSearch(LocalSearchScope(psiFile))
                                }
                            }
                        }
                    }
                }
                finally {
                    progress?.popState()
                }
            }
        }
    }

    private fun SearchScope.logPresentation(): String {
        return when (this) {
            searchScope -> "whole search scope"

            is LocalSearchScope -> {
                scope
                        .map { element ->
                            "    " + when (element) {
                                is KtFunctionLiteral -> element.text
                                is KtWhenEntry -> {
                                    if (element.isElse)
                                        "KtWhenEntry \"else\""
                                    else
                                        "KtWhenEntry \"" + element.conditions.joinToString(", ") { it.text } + "\""
                                }
                                is KtNamedDeclaration -> element.node.elementType.toString() + ":" + element.name
                                else -> element.toString()
                            }
                        }
                        .toList()
                        .sorted()
                        .joinToString("\n", "LocalSearchScope:\n")
            }

            else -> this.displayName
        }

    }
}
