/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.allowResolveInDispatchThread
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.getImportableTargets
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.Companion.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstClassifierWithDeprecationStatus
import org.jetbrains.kotlin.resolve.scopes.utils.findPackage
import org.jetbrains.kotlin.resolve.source.getPsi

class ShortenReferences(val options: (KtElement) -> Options = { Options.DEFAULT }) {
    data class Options(
        val removeThisLabels: Boolean = false,
        val removeThis: Boolean = false,
        // TODO: remove this option and all related stuff (RETAIN_COMPANION etc.) after KT-13934 fixed
        val removeExplicitCompanion: Boolean = true,
        val dropBracesInStringTemplates: Boolean = true
    ) {
        companion object {
            val DEFAULT = Options()
            val ALL_ENABLED = Options(removeThisLabels = true, removeThis = true)
        }
    }

    companion object {
        @JvmField
        val DEFAULT = ShortenReferences()

        val RETAIN_COMPANION = ShortenReferences { Options(removeExplicitCompanion = false) }

        fun canBePossibleToDropReceiver(element: KtDotQualifiedExpression, bindingContext: BindingContext): Boolean {
            val nameRef = when (val receiver = element.receiverExpression) {
                is KtThisExpression -> return true
                is KtNameReferenceExpression -> receiver
                is KtDotQualifiedExpression -> receiver.selectorExpression as? KtNameReferenceExpression ?: return false
                else -> return false
            }
            if (nameRef.name == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) return true

            when (val targetDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, nameRef]) {
                is ClassDescriptor -> {
                    if (targetDescriptor.kind != ClassKind.OBJECT) return true
                    // for object receiver we should additionally check that it's dispatch receiver (that is the member is inside the object) or not a receiver at all
                    val resolvedCall = element.getResolvedCall(bindingContext)
                        ?: return element.getQualifiedElementSelector()?.mainReference?.resolveToDescriptors(bindingContext) != null

                    val receiverKind = resolvedCall.explicitReceiverKind
                    return receiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER || receiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                }

                is PackageViewDescriptor -> return true

                else -> return false
            }
        }

        private fun DeclarationDescriptor.asString() = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

        private fun KtReferenceExpression.targets(context: BindingContext) = getImportableTargets(context)

        private fun mayImport(descriptor: DeclarationDescriptor, file: KtFile): Boolean {
            return descriptor.canBeReferencedViaImport()
                    && ImportInsertHelper.getInstance(file.project).mayImportOnShortenReferences(descriptor)
        }
    }

    @JvmOverloads
    fun process(element: KtElement, elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }): KtElement {
        return process(listOf(element), elementFilter).single()
    }

    @JvmOverloads
    fun process(file: KtFile, startOffset: Int, endOffset: Int, additionalFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = file.viewProvider.document!!
        check(documentManager.isCommitted(document)) { "Document should be committed to shorten references in range" }

        val rangeMarker = document.createRangeMarker(startOffset, endOffset)
        rangeMarker.isGreedyToLeft = true
        rangeMarker.isGreedyToRight = true

        val rangeFilter = { element: PsiElement ->
            if (rangeMarker.isValid) {
                val range = TextRange(rangeMarker.startOffset, rangeMarker.endOffset)

                val elementRange = element.textRange!!
                when {
                    range.contains(elementRange) -> FilterResult.PROCESS

                    range.intersects(elementRange) -> {
                        // for qualified call expression allow to shorten only the part without parenthesis
                        val calleeExpression = ((element as? KtDotQualifiedExpression)
                            ?.selectorExpression as? KtCallExpression)
                            ?.calleeExpression
                        if (calleeExpression != null) {
                            val rangeWithoutParenthesis = TextRange(elementRange.startOffset, calleeExpression.textRange!!.endOffset)
                            if (range.contains(rangeWithoutParenthesis)) FilterResult.PROCESS else FilterResult.GO_INSIDE
                        } else {
                            FilterResult.GO_INSIDE
                        }
                    }
                    else -> FilterResult.SKIP
                }
            } else {
                FilterResult.SKIP
            }
        }
        try {
            process(listOf(file)) { element ->
                minOf(rangeFilter(element), additionalFilter(element))
            }
        } finally {
            rangeMarker.dispose()
        }
    }

    enum class FilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    @JvmOverloads
    fun process(
        elements: Iterable<KtElement>,
        elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }
    ): Collection<KtElement> {
        return elements.groupBy(KtElement::getContainingKtFile)
            .flatMap { shortenReferencesInFile(it.key, it.value, elementFilter) }
    }

    private fun shortenReferencesInFile(
        file: KtFile,
        elements: List<KtElement>,
        elementFilter: (PsiElement) -> FilterResult
    ): Collection<KtElement> {
        //TODO: that's not correct since we have options!
        val elementsToUse = dropNestedElements(elements)

        val helper = ImportInsertHelper.getInstance(file.project)

        val failedToImportDescriptors = LinkedHashSet<DeclarationDescriptor>()

        val companionElementFilter = { element: PsiElement ->
            if (element is KtElement && !options(element).removeExplicitCompanion) {
                FilterResult.SKIP
            } else {
                elementFilter(element)
            }
        }

        while (true) {
            // Processors order is important here so that enclosing elements are not shortened before their children are, e.g.
            // test.foo(this@A) -> foo(this)
            val processors: List<ShorteningProcessor<*>> = listOf(
                ShortenTypesProcessor(file, elementFilter, failedToImportDescriptors),
                ShortenThisExpressionsProcessor(file, elementFilter, failedToImportDescriptors),
                ShortenQualifiedExpressionsProcessor(file, elementFilter, failedToImportDescriptors),
                RemoveExplicitCompanionObjectReferenceProcessor(file, companionElementFilter, failedToImportDescriptors)
            )

            // step 1: collect qualified elements to analyze (no resolve at this step)
            val visitors = processors.map { it.collectElementsVisitor }
            for (visitor in visitors) {
                for (element in elementsToUse) {
                    visitor.options = options(element)
                    element.accept(visitor)
                }
            }

            // step 2: analyze collected elements with resolve and decide which can be shortened now and which need descriptors to be imported before shortening
            val allElementsToAnalyze = visitors.flatMap { visitor -> visitor.getElementsToAnalyze().map { it.element } }.toSet()
            val bindingContext = allowResolveInDispatchThread {
                file.getResolutionFacade().analyze(allElementsToAnalyze, BodyResolveMode.PARTIAL_WITH_CFA)
            }

            processors.forEach { it.analyzeCollectedElements(bindingContext) }

            // step 3: shorten elements that can be shortened right now
            runWriteAction {
                processors.forEach { it.shortenElements(elementSetToUpdate = elementsToUse, options = options) }
            }

            // step 4: try to import descriptors needed to shorten other elements
            val descriptorsToImport = processors.flatMap { it.getDescriptorsToImport() }.toSet()
            var anyChange = false
            runWriteAction {
                for (descriptor in descriptorsToImport) {
                    assert(descriptor !in failedToImportDescriptors)

                    val result = helper.importDescriptor(file, descriptor)
                    if (result != ImportDescriptorResult.ALREADY_IMPORTED) {
                        anyChange = true
                    }
                    if (result == ImportDescriptorResult.FAIL) {
                        failedToImportDescriptors.add(descriptor)
                    }
                }
            }
            if (!anyChange) break
        }

        return elementsToUse
    }

    private fun dropNestedElements(elements: List<KtElement>): LinkedHashSet<KtElement> {
        val elementSet = elements.toSet()
        return elementSet.filterTo(LinkedHashSet(elementSet.size)) { element ->
            element.parents.none { it in elementSet }
        }
    }

    private data class ElementToAnalyze<TElement>(val element: TElement, val level: Int)

    private abstract class CollectElementsVisitor<TElement : KtElement>(
        protected val elementFilter: (PsiElement) -> FilterResult
    ) : KtVisitorVoid() {

        var options: Options = Options.DEFAULT

        private val elementsToAnalyze = ArrayList<ElementToAnalyze<TElement>>()

        private var level = 0

        protected fun nextLevel() {
            level++
        }

        protected fun prevLevel() {
            level--
            assert(level >= 0)
        }

        /**
         * Should be invoked by implementors when visiting the PSI tree for those elements that can potentially be shortened
         */
        protected fun addQualifiedElementToAnalyze(element: TElement) {
            elementsToAnalyze.add(ElementToAnalyze(element, level))
        }

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        fun getElementsToAnalyze(): List<ElementToAnalyze<TElement>> = elementsToAnalyze
    }

    private abstract class ShorteningProcessor<TElement : KtElement>(
        protected val file: KtFile,
        protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) {
        protected val resolutionFacade = file.getResolutionFacade()
        private val elementsToShorten = ArrayList<SmartPsiElementPointer<TElement>>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        abstract val collectElementsVisitor: CollectElementsVisitor<TElement>

        fun analyzeCollectedElements(bindingContext: BindingContext) {
            val elements = collectElementsVisitor.getElementsToAnalyze()

            var index = 0
            while (index < elements.size) {
                val (element, level) = elements[index++]

                val result = analyzeQualifiedElement(element, bindingContext)

                val toBeShortened: Boolean
                when (result) {
                    AnalyzeQualifiedElementResult.ShortenNow -> {
                        elementsToShorten.add(element.createSmartPointer())
                        toBeShortened = true
                    }

                    is AnalyzeQualifiedElementResult.ImportDescriptors -> {
                        val tryImport = result.descriptors.isNotEmpty()
                                && result.descriptors.none { it in failedToImportDescriptors }
                                && result.descriptors.all { mayImport(it, file) }
                        toBeShortened = if (tryImport) {
                            descriptorsToImport.addAll(result.descriptors)
                            true
                        } else {
                            false
                        }
                    }

                    AnalyzeQualifiedElementResult.Skip -> {
                        toBeShortened = false
                    }
                }

                if (toBeShortened) {
                    // we are going to shorten qualified element - we must skip all elements inside its qualifier
                    while (index < elements.size && elements[index].level > level) {
                        index++
                    }
                }
            }
        }

        /**
         * This method is invoked for all qualified elements added by [CollectElementsVisitor.addQualifiedElementToAnalyze]
         */
        protected abstract fun analyzeQualifiedElement(element: TElement, bindingContext: BindingContext): AnalyzeQualifiedElementResult

        protected sealed class AnalyzeQualifiedElementResult {
            object Skip : AnalyzeQualifiedElementResult()

            object ShortenNow : AnalyzeQualifiedElementResult()

            class ImportDescriptors(val descriptors: Collection<DeclarationDescriptor>) : AnalyzeQualifiedElementResult()
        }

        protected abstract fun shortenElement(element: TElement, options: Options): KtElement

        fun shortenElements(elementSetToUpdate: MutableSet<KtElement>, options: (KtElement) -> Options) {
            for (elementPointer in elementsToShorten) {
                val element = elementPointer.element ?: continue
                if (!element.isValid) continue

                var newElement: KtElement? = null
                // we never want any reformatting to happen because sometimes it causes strange effects (see KT-11633)
                PostprocessReformattingAspect.getInstance(element.project).disablePostprocessFormattingInside {
                    newElement = shortenElement(element, options(element))
                }

                if (element in elementSetToUpdate && newElement != element) {
                    elementSetToUpdate.remove(element)
                    elementSetToUpdate.add(newElement!!)
                }
            }
        }

        fun getDescriptorsToImport(): Set<DeclarationDescriptor> = descriptorsToImport
    }

    private class ShortenTypesProcessor(
        file: KtFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<KtUserType>(file, failedToImportDescriptors) {

        override val collectElementsVisitor: CollectElementsVisitor<KtUserType> =
            object : CollectElementsVisitor<KtUserType>(elementFilter) {
                override fun visitUserType(userType: KtUserType) {
                    val filterResult = elementFilter(userType)
                    if (filterResult == FilterResult.SKIP) return

                    userType.typeArgumentList?.accept(this)

                    if (filterResult == FilterResult.PROCESS) {
                        addQualifiedElementToAnalyze(userType)
                        nextLevel()
                    }

                    // elements in qualifier must be under
                    userType.qualifier?.accept(this)
                    if (filterResult == FilterResult.PROCESS) {
                        prevLevel()
                    }
                }
            }

        override fun analyzeQualifiedElement(element: KtUserType, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
            fun eval(element: KtUserType, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
                if (element.qualifier == null) return AnalyzeQualifiedElementResult.Skip
                val referenceExpression = element.referenceExpression ?: return AnalyzeQualifiedElementResult.Skip

                val target = referenceExpression.targets(bindingContext).singleOrNull()
                    ?: return AnalyzeQualifiedElementResult.Skip

                val scope = element.getResolutionScope(bindingContext, resolutionFacade)
                val name = target.name

                val targetByName: DeclarationDescriptor?
                val isDeprecated: Boolean

                if (target is ClassifierDescriptor) {
                    val classifierWithDeprecation = scope.findFirstClassifierWithDeprecationStatus(name, NoLookupLocation.FROM_IDE)
                    targetByName = classifierWithDeprecation?.descriptor
                    isDeprecated = classifierWithDeprecation?.isDeprecated ?: false
                } else {
                    targetByName = scope.findPackage(name)
                    isDeprecated = false
                }

                val canShortenNow = targetByName?.asString() == target.asString() && !isDeprecated
                return if (canShortenNow) AnalyzeQualifiedElementResult.ShortenNow else AnalyzeQualifiedElementResult.ImportDescriptors(
                    listOfNotNull(target)
                )
            }

            val result = eval(element, bindingContext)
            if (result is AnalyzeQualifiedElementResult.Skip &&
                element.qualifier?.text == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
            ) return AnalyzeQualifiedElementResult.ShortenNow

            return result
        }

        override fun shortenElement(element: KtUserType, options: Options): KtElement {
            element.deleteQualifier()
            return element
        }
    }

    private abstract class QualifiedExpressionShorteningProcessor(
        file: KtFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<KtDotQualifiedExpression>(file, failedToImportDescriptors) {

        protected open class MyVisitor(elementFilter: (PsiElement) -> FilterResult) :
            CollectElementsVisitor<KtDotQualifiedExpression>(elementFilter) {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val filterResult = elementFilter(expression)
                if (filterResult == FilterResult.SKIP) return

                expression.selectorExpression?.acceptChildren(this)

                if (filterResult == FilterResult.PROCESS) {
                    addQualifiedElementToAnalyze(expression)
                    nextLevel()
                }

                // elements in receiver must be under
                expression.receiverExpression.accept(this)
                if (filterResult == FilterResult.PROCESS) {
                    prevLevel()
                }
            }
        }

        override val collectElementsVisitor = MyVisitor(elementFilter)
    }

    private class ShortenQualifiedExpressionsProcessor(
        file: KtFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningProcessor(file, elementFilter, failedToImportDescriptors) {

        override val collectElementsVisitor = object : MyVisitor(elementFilter) {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                if (expression.receiverExpression is KtThisExpression && !options.removeThis) {
                    val filterResult = elementFilter(expression)
                    if (filterResult == FilterResult.SKIP) return
                    expression.selectorExpression?.acceptChildren(this)
                    return
                }
                super.visitDotQualifiedExpression(expression)
            }
        }

        override fun analyzeQualifiedElement(
            element: KtDotQualifiedExpression,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            if (element.receiverExpression.text == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) return AnalyzeQualifiedElementResult.ShortenNow

            if (PsiTreeUtil.getParentOfType(
                    element,
                    KtImportDirective::class.java, KtPackageDirective::class.java
                ) != null || !canBePossibleToDropReceiver(element, bindingContext)
            ) return AnalyzeQualifiedElementResult.Skip

            val selector = element.selectorExpression ?: return AnalyzeQualifiedElementResult.Skip
            val callee = selector.getCalleeExpressionIfAny() as? KtReferenceExpression ?: return AnalyzeQualifiedElementResult.Skip


            val targets = callee.targets(bindingContext)
            val resolvedCall = callee.getResolvedCall(bindingContext)

            if (targets.isEmpty()) return AnalyzeQualifiedElementResult.Skip

            val (newContext, selectorAfterShortening) = copyShortenAndAnalyze(element, bindingContext)
            val newCallee = selectorAfterShortening.getCalleeExpressionIfAny() as KtReferenceExpression

            val targetsWhenShort = newCallee.targets(newContext)

            val resolvedCallWhenShort = newCallee.getResolvedCall(newContext)
            val targetsMatch = targetsMatch(targets, targetsWhenShort) &&
                    (resolvedCall !is VariableAsFunctionResolvedCall || (
                            resolvedCallWhenShort is VariableAsFunctionResolvedCall? &&
                                    resolvedCallsMatch(resolvedCall, resolvedCallWhenShort)))

            // Don't shorten references if it will result to call to deprecated classifier by short name
            val isShortenedReferenceResolvesToDeprecated = newContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, newCallee] == true
            if (isShortenedReferenceResolvesToDeprecated) return AnalyzeQualifiedElementResult.Skip

            // If before and after shorten call can be resolved unambiguously, then preform comparing of such calls,
            // if it matches, then we can preform shortening
            // Otherwise, collecting candidates both before and after shorten,
            // then filtering out candidates hidden by more specific signature
            // (for ex local fun, and extension fun on same receiver with matching names and signature)
            // Then, checking that any of resolved calls when shorten matches with calls before shorten, it means that there can be
            // targetMatch == false, but shorten still can be preformed
            // TODO: Add possibility to check if descriptor from completion can't be resolved after shorten and not preform shorten than
            val resolvedCallsMatch = if (resolvedCall != null && resolvedCallWhenShort != null) {
                resolvedCall.resultingDescriptor.original == resolvedCallWhenShort.resultingDescriptor.original
            } else {
                val resolvedCalls = selector.getCall(bindingContext)?.resolveCandidates(bindingContext, resolutionFacade) ?: emptyList()
                val callWhenShort = selectorAfterShortening.getCall(newContext)
                val resolvedCallsWhenShort =
                    selectorAfterShortening.getCall(newContext)?.resolveCandidates(newContext, resolutionFacade) ?: emptyList()

                val descriptorsOfResolvedCallsWhenShort = resolvedCallsWhenShort.map { it.resultingDescriptor.original }
                val descriptorsOfResolvedCalls = resolvedCalls.mapTo(mutableSetOf()) { it.resultingDescriptor.original }

                val filter =
                    ShadowedDeclarationsFilter(newContext, resolutionFacade, newCallee, callWhenShort?.explicitReceiver as? ReceiverValue)
                val availableDescriptorsWhenShort = filter.filter(descriptorsOfResolvedCallsWhenShort)

                availableDescriptorsWhenShort.any { it in descriptorsOfResolvedCalls }
            }


            val receiver = element.receiverExpression
            if (receiver is KtThisExpression) {
                if (!targetsMatch) return AnalyzeQualifiedElementResult.Skip
                val originalCall = selector.getResolvedCall(bindingContext) ?: return AnalyzeQualifiedElementResult.Skip
                val newCall = selectorAfterShortening.getResolvedCall(newContext) ?: return AnalyzeQualifiedElementResult.Skip
                val receiverKind = originalCall.explicitReceiverKind
                val newReceiver = when (receiverKind) {
                    ExplicitReceiverKind.BOTH_RECEIVERS, ExplicitReceiverKind.EXTENSION_RECEIVER -> newCall.extensionReceiver
                    ExplicitReceiverKind.DISPATCH_RECEIVER -> newCall.dispatchReceiver
                    else -> return AnalyzeQualifiedElementResult.Skip
                } as? ImplicitReceiver ?: return AnalyzeQualifiedElementResult.Skip

                val thisTarget = receiver.instanceReference.targets(bindingContext).singleOrNull()
                if (newReceiver.declarationDescriptor.asString() != thisTarget?.asString()) return AnalyzeQualifiedElementResult.Skip
            }

            return when {
                targetsMatch || resolvedCallsMatch ->
                    AnalyzeQualifiedElementResult.ShortenNow

                // Function doesn't conflict with property
                targets.all { it is FunctionDescriptor } && targetsWhenShort.all { it is PropertyDescriptor } ->
                    AnalyzeQualifiedElementResult.ImportDescriptors(targets)

                // In other cases it makes no sense to insert import when there is a conflict with function, property etc
                targetsWhenShort.any { it !is ClassifierDescriptorWithTypeParameters && it !is PackageViewDescriptor } ->
                    AnalyzeQualifiedElementResult.Skip


                else ->
                    AnalyzeQualifiedElementResult.ImportDescriptors(targets)
            }
        }

        private fun copyShortenAndAnalyze(
            element: KtDotQualifiedExpression,
            bindingContext: BindingContext
        ): Pair<BindingContext, KtExpression> {
            val selector = element.selectorExpression!!

            //                selector V  V             selector V  V
            // When processing some.fq.Name::callable or some.fq.Name::class
            //                 ^  element ^              ^  element ^
            //
            // Result should be Name::callable or Name::class
            //                  ^  ^              ^  ^
            val doubleColonExpression = element.getParentOfType<KtDoubleColonExpression>(true)
            if (doubleColonExpression != null && doubleColonExpression.receiverExpression == element) {
                val doubleColonExpressionCopy = doubleColonExpression.copied()
                doubleColonExpressionCopy.receiverExpression!!.replace(selector)
                val newBindingContext =
                    doubleColonExpressionCopy.analyzeAsReplacement(doubleColonExpression, bindingContext, resolutionFacade)
                return newBindingContext to doubleColonExpressionCopy.receiverExpression!!
            }

            val qualifiedAbove = element.getQualifiedExpressionForReceiver()
            if (qualifiedAbove != null) {
                val qualifiedAboveCopy = qualifiedAbove.copied()
                qualifiedAboveCopy.receiverExpression.replace(selector)
                val newBindingContext = qualifiedAboveCopy.analyzeAsReplacement(qualifiedAbove, bindingContext, resolutionFacade)
                return newBindingContext to qualifiedAboveCopy.receiverExpression
            }

            val copied = selector.copied()
            val newBindingContext = copied.analyzeAsReplacement(element, bindingContext, resolutionFacade)
            return newBindingContext to copied
        }

        private fun targetsMatch(targets1: Collection<DeclarationDescriptor>, targets2: Collection<DeclarationDescriptor>): Boolean {
            if (targets1.size != targets2.size) return false
            return if (targets1.size == 1) {
                targets1.single().asString() == targets2.single().asString()
            } else {
                targets1.map { it.asString() }.toSet() == targets2.map { it.asString() }.toSet()
            }
        }

        private fun resolvedCallsMatch(rc1: VariableAsFunctionResolvedCall?, rc2: VariableAsFunctionResolvedCall?): Boolean {
            return rc1?.variableCall?.candidateDescriptor?.asString() == rc2?.variableCall?.candidateDescriptor?.asString() &&
                    rc1?.functionCall?.candidateDescriptor?.asString() == rc2?.functionCall?.candidateDescriptor?.asString()
        }

        override fun shortenElement(element: KtDotQualifiedExpression, options: Options): KtElement {
            val parens = element.parent as? KtParenthesizedExpression
            val requiredParens = parens != null && !KtPsiUtil.areParenthesesUseless(parens)
            val commentSaver = CommentSaver(element)
            val shortenedElement = element.replace(element.selectorExpression!!) as KtElement
            commentSaver.restore(shortenedElement)
            val newParent = shortenedElement.parent
            if (requiredParens) return newParent.replaced(shortenedElement)
            if (options.dropBracesInStringTemplates && newParent is KtBlockStringTemplateEntry && newParent.canDropBraces()) {
                newParent.dropBraces()
            }
            return shortenedElement
        }
    }

    private class ShortenThisExpressionsProcessor(
        file: KtFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<KtThisExpression>(file, failedToImportDescriptors) {

        private val simpleThis = KtPsiFactory(file).createExpression("this") as KtThisExpression

        override val collectElementsVisitor: CollectElementsVisitor<KtThisExpression> =
            object : CollectElementsVisitor<KtThisExpression>(elementFilter) {
                override fun visitThisExpression(expression: KtThisExpression) {
                    if (options.removeThisLabels && elementFilter(expression) == FilterResult.PROCESS && expression.getTargetLabel() != null) {
                        addQualifiedElementToAnalyze(expression)
                    }
                }
            }

        override fun analyzeQualifiedElement(element: KtThisExpression, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
            val targetBefore = element.instanceReference.targets(bindingContext).singleOrNull() ?: return AnalyzeQualifiedElementResult.Skip
            val newContext = simpleThis.analyzeAsReplacement(element, bindingContext, resolutionFacade)
            val targetAfter = simpleThis.instanceReference.targets(newContext).singleOrNull()
            return if (targetBefore == targetAfter) AnalyzeQualifiedElementResult.ShortenNow else AnalyzeQualifiedElementResult.Skip
        }

        override fun shortenElement(element: KtThisExpression, options: Options): KtElement {
            return element.replace(simpleThis) as KtElement
        }
    }

    private class RemoveExplicitCompanionObjectReferenceProcessor(
        file: KtFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningProcessor(file, elementFilter, failedToImportDescriptors) {

        private fun KtExpression.singleTarget(context: BindingContext): DeclarationDescriptor? {
            return (getCalleeExpressionIfAny() as? KtReferenceExpression)?.targets(context)?.singleOrNull()
        }

        override fun analyzeQualifiedElement(
            element: KtDotQualifiedExpression,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            val parent = element.parent
            // TODO: Delete this code when KT-13934 is fixed
            if (parent is KtCallableReferenceExpression && parent.receiverExpression == element) return AnalyzeQualifiedElementResult.Skip

            val receiver = element.receiverExpression

            if (PsiTreeUtil.getParentOfType(
                    element,
                    KtImportDirective::class.java, KtPackageDirective::class.java
                ) != null
            ) return AnalyzeQualifiedElementResult.Skip

            val receiverTarget = receiver.singleTarget(bindingContext) as? ClassDescriptor ?: return AnalyzeQualifiedElementResult.Skip

            val selectorExpression = element.selectorExpression ?: return AnalyzeQualifiedElementResult.Skip
            val selectorTarget = selectorExpression.singleTarget(bindingContext) ?: return AnalyzeQualifiedElementResult.Skip

            if (receiverTarget.companionObjectDescriptor != selectorTarget) return AnalyzeQualifiedElementResult.Skip

            val selectorsSelector = (parent as? KtDotQualifiedExpression)?.selectorExpression
                ?: return AnalyzeQualifiedElementResult.ShortenNow

            val selectorsSelectorTarget = selectorsSelector.singleTarget(bindingContext) ?: return AnalyzeQualifiedElementResult.Skip
            if (selectorsSelectorTarget is ClassDescriptor) return AnalyzeQualifiedElementResult.Skip
            // TODO: More generic solution may be possible
            if (selectorsSelectorTarget is PropertyDescriptor) {
                val source = selectorsSelectorTarget.source.getPsi() as? KtProperty
                if (source != null && isEnumCompanionPropertyWithEntryConflict(
                        source,
                        source.name ?: ""
                    )
                ) return AnalyzeQualifiedElementResult.Skip
            }

            return AnalyzeQualifiedElementResult.ShortenNow
        }

        override fun shortenElement(element: KtDotQualifiedExpression, options: Options): KtElement {
            val receiver = element.receiverExpression
            val selector = element.selectorExpression ?: return element

            return when (receiver) {
                is KtSimpleNameExpression -> {
                    val identifier = receiver.getIdentifier() ?: return element
                    (selector.getCalleeExpressionIfAny() as? KtSimpleNameExpression)?.getIdentifier()?.replace(identifier)
                    element.replace(selector) as KtExpression
                }

                is KtQualifiedExpression -> {
                    val identifier = (receiver.selectorExpression as? KtSimpleNameExpression)?.getIdentifier() ?: return element
                    (selector.getCalleeExpressionIfAny() as? KtSimpleNameExpression)?.getIdentifier()?.replace(identifier)
                    receiver.selectorExpression?.replace(selector)
                    element.replace(receiver) as KtExpression
                }

                else -> element
            }
        }
    }
}
