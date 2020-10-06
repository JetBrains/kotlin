/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search.usagesSearch.operators

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundleIndependent
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.forceResolveReferences
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.getReceiverTypeSearcherInfo
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinRequestResultProcessor
import org.jetbrains.kotlin.idea.search.ifTrue
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.logPresentation
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.testLog
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

abstract class OperatorReferenceSearcher<TReferenceElement : KtElement>(
    protected val targetDeclaration: PsiElement,
    private val searchScope: SearchScope,
    private val consumer: Processor<in PsiReference>,
    private val optimizer: SearchRequestCollector,
    private val options: KotlinReferencesSearchOptions,
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
    protected abstract fun extractReference(element: KtElement): PsiReference?

    /**
     * Check if reference may potentially resolve to our operator (no actual resolve to be performed)
     */
    protected abstract fun isReferenceToCheck(ref: PsiReference): Boolean

    protected fun processReferenceElement(element: TReferenceElement): Boolean {
        val reference = extractReference(element) ?: return true
        testLog { "Resolved ${logPresentation(element)}" }
        return if (reference.isReferenceTo(targetDeclaration)) {
            consumer.process(reference)
        } else {
            true
        }
    }

    companion object {
        fun create(
            declaration: PsiElement,
            searchScope: SearchScope,
            consumer: Processor<in PsiReference>,
            optimizer: SearchRequestCollector,
            options: KotlinReferencesSearchOptions
        ): OperatorReferenceSearcher<*>? {
            return runReadAction {
                declaration.isValid.ifTrue {
                    createInReadAction(declaration, searchScope, consumer, optimizer, options)
                }
            }
        }

        private fun createInReadAction(
            declaration: PsiElement,
            searchScope: SearchScope,
            consumer: Processor<in PsiReference>,
            optimizer: SearchRequestCollector,
            options: KotlinReferencesSearchOptions
        ): OperatorReferenceSearcher<*>? {
            val functionName = when (declaration) {
                is KtNamedFunction -> declaration.name
                is PsiMethod -> declaration.name
                else -> null
            } ?: return null

            if (!Name.isValidIdentifier(functionName)) return null
            val name = Name.identifier(functionName)

            val declarationToUse = if (declaration is KtLightMethod) {
                declaration.kotlinOrigin ?: return null
            } else {
                declaration
            }

            return createInReadAction(declarationToUse, name, consumer, optimizer, options, searchScope)
        }

        private fun createInReadAction(
            declaration: PsiElement,
            name: Name,
            consumer: Processor<in PsiReference>,
            optimizer: SearchRequestCollector,
            options: KotlinReferencesSearchOptions,
            searchScope: SearchScope
        ): OperatorReferenceSearcher<*>? {
            if (DataClassDescriptorResolver.isComponentLike(name)) {
                if (!options.searchForComponentConventions) return null
                val componentIndex = DataClassDescriptorResolver.getComponentIndex(name.asString())
                return DestructuringDeclarationReferenceSearcher(declaration, componentIndex, searchScope, consumer, optimizer, options)
            }

            if (!options.searchForOperatorConventions) return null

            val binaryOp = OperatorConventions.BINARY_OPERATION_NAMES.inverse()[name]
            val assignmentOp = OperatorConventions.ASSIGNMENT_OPERATIONS.inverse()[name]
            val unaryOp = OperatorConventions.UNARY_OPERATION_NAMES.inverse()[name]

            when {
                binaryOp != null -> {
                    val counterpartAssignmentOp = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.inverse()[binaryOp]
                    val operationTokens = listOfNotNull(binaryOp, counterpartAssignmentOp)
                    return BinaryOperatorReferenceSearcher(declaration, operationTokens, searchScope, consumer, optimizer, options)
                }

                assignmentOp != null ->
                    return BinaryOperatorReferenceSearcher(declaration, listOf(assignmentOp), searchScope, consumer, optimizer, options)

                unaryOp != null ->
                    return UnaryOperatorReferenceSearcher(declaration, unaryOp, searchScope, consumer, optimizer, options)

                name == OperatorNameConventions.INVOKE ->
                    return InvokeOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                name == OperatorNameConventions.GET ->
                    return IndexingOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options, isSet = false)

                name == OperatorNameConventions.SET ->
                    return IndexingOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options, isSet = true)

                name == OperatorNameConventions.CONTAINS ->
                    return ContainsOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                name == OperatorNameConventions.EQUALS ->
                    return BinaryOperatorReferenceSearcher(
                        declaration,
                        listOf(KtTokens.EQEQ, KtTokens.EXCLEQ),
                        searchScope,
                        consumer,
                        optimizer,
                        options
                    )

                name == OperatorNameConventions.COMPARE_TO ->
                    return BinaryOperatorReferenceSearcher(
                        declaration,
                        listOf(KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ),
                        searchScope,
                        consumer,
                        optimizer,
                        options
                    )

                name == OperatorNameConventions.ITERATOR ->
                    return IteratorOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                name == OperatorNameConventions.GET_VALUE || name == OperatorNameConventions.SET_VALUE || name == OperatorNameConventions.PROVIDE_DELEGATE ->
                    return PropertyDelegationOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                else ->
                    return null
            }

        }

        private object SearchesInProgress : ThreadLocal<HashSet<PsiElement>>() {
            override fun initialValue() = HashSet<PsiElement>()
        }
    }

    fun run() {

        val (psiClass, containsTypeOrDerivedInside) =
            targetDeclaration.getReceiverTypeSearcherInfo(this is DestructuringDeclarationReferenceSearcher) ?: return

        val inProgress = SearchesInProgress.get()
        if (psiClass != null) {
            if (!inProgress.add(psiClass)) {
                testLog {
                    "ExpressionOfTypeProcessor is already started for ${runReadAction { psiClass.qualifiedName }}. Exit for operator ${logPresentation(
                        targetDeclaration
                    )}."
                }
                return
            }
        } else {
            if (!inProgress.add(targetDeclaration)) {
                testLog { "ExpressionOfTypeProcessor is already started for operator ${logPresentation(targetDeclaration)}. Exit." }
                return //TODO: it's not quite correct
            }
        }

        try {
            ExpressionsOfTypeProcessor(
                containsTypeOrDerivedInside,
                psiClass,
                searchScope,
                project,
                possibleMatchHandler = { expression -> processPossibleReceiverExpression(expression) },
                possibleMatchesInScopeHandler = { searchScope -> doPlainSearch(searchScope) }
            ).run()
        } finally {
            inProgress.remove(psiClass ?: targetDeclaration)
        }
    }

    private fun doPlainSearch(scope: SearchScope) {
        testLog { "Used plain search of ${logPresentation(targetDeclaration)} in ${scope.logPresentation()}" }

        if (scope is LocalSearchScope) {
            for (element in scope.scope) {
                if (element is KtElement) {
                    runReadAction {
                        if (element.isValid) {
                            val refs = ArrayList<PsiReference>()
                            val elements = element.collectDescendantsOfType<KtElement> {
                                val ref = extractReference(it) ?: return@collectDescendantsOfType false
                                refs.add(ref)
                                true
                            }

                            // resolve all references at once
                            (element.containingFile as? KtFile)?.forceResolveReferences(elements)

                            refs
                                .filter { it.isReferenceTo(targetDeclaration) }
                                .forEach { consumer.process(it) }
                        }
                    }
                }
            }
        } else {
            scope as GlobalSearchScope
            if (wordsToSearch.isNotEmpty()) {
                val unwrappedElement = targetDeclaration.namedUnwrappedElement ?: return
                val resultProcessor = KotlinRequestResultProcessor(
                    unwrappedElement,
                    filter = { ref -> isReferenceToCheck(ref) },
                    options = options
                )
                wordsToSearch.forEach {
                    optimizer.searchWord(
                        it,
                        scope.restrictToKotlinSources(),
                        UsageSearchContext.IN_CODE,
                        true,
                        unwrappedElement,
                        resultProcessor
                    )
                }
            } else {
                val psiManager = PsiManager.getInstance(project)
                // we must unwrap progress indicator because ProgressWrapper does not do anything on changing text and fraction
                val progress = ProgressWrapper.unwrap(ProgressIndicatorProvider.getGlobalProgressIndicator())
                progress?.pushState()
                progress?.text = KotlinIdeaAnalysisBundleIndependent.message("searching.for.implicit.usages")

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
                } finally {
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
                        "    " + runReadAction {
                            when (element) {
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
                    }
                    .toList()
                    .sorted()
                    .joinToString("\n", "LocalSearchScope:\n")
            }

            else -> this.displayName
        }

    }
}