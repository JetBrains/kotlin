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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.ShortenReferences.Options
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.getImportableTargets
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findPackage
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.lang.IllegalStateException
import java.util.*

class ShortenReferences(val options: (KtElement) -> Options = { Options.DEFAULT }) {
    data class Options(
            val removeThisLabels: Boolean = false,
            val removeThis: Boolean = false
    ) {
        companion object {
            val DEFAULT = Options()
            val ALL_ENABLED = Options(true, true)
        }
    }

    companion object {
        @JvmField
        val DEFAULT = ShortenReferences()

        private fun DeclarationDescriptor.asString()
                = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

        private fun KtReferenceExpression.targets(context: BindingContext) = getImportableTargets(context)

        private fun mayImport(descriptor: DeclarationDescriptor, file: KtFile): Boolean {
            return descriptor.canBeReferencedViaImport()
                   && ImportInsertHelper.getInstance(file.project).mayImportOnShortenReferences(descriptor)
        }
    }

    @JvmOverloads fun process(element: KtElement, elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }): KtElement {
        return process(listOf(element), elementFilter).single()
    }

    fun process(file: KtFile, startOffset: Int, endOffset: Int) {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = file.viewProvider.document!!
        if (!documentManager.isCommitted(document)) {
            throw IllegalStateException("Document should be committed to shorten references in range")
        }

        val rangeMarker = document.createRangeMarker(startOffset, endOffset)
        rangeMarker.isGreedyToLeft = true
        rangeMarker.isGreedyToRight = true
        try {
            process(listOf(file), { element ->
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

    enum class FilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    @JvmOverloads fun process(elements: Iterable<KtElement>, elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }): Collection<KtElement> {
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

        while (true) {
            // Visitor order is important here so that enclosing elements are not shortened before their children are, e.g.
            // test.foo(this@A) -> foo(this)
            val visitors: List<ShorteningVisitor<*>> = listOf(
                    ShortenTypesVisitor(file, elementFilter, failedToImportDescriptors),
                    ShortenThisExpressionsVisitor(file, elementFilter, failedToImportDescriptors),
                    ShortenQualifiedExpressionsVisitor(file, elementFilter, failedToImportDescriptors),
                    RemoveExplicitCompanionObjectReferenceVisitor(file, elementFilter, failedToImportDescriptors)
            )

            // step 1: collect qualified elements to analyze (no resolve at this step)
            for (visitor in visitors) {
                for (element in elementsToUse) {
                    visitor.options = options(element)
                    element.accept(visitor)
                }
            }
            val elementsToAnalyze = visitors.flatMap { it.getElementsToAnalyze() }

            // step 2: analyze collected elements with resolve and decide which can be shortened now and which need descriptors to be imported before shortening
            val bindingContext = file.getResolutionFacade().analyze(elementsToAnalyze, BodyResolveMode.PARTIAL)
            visitors.forEach { it.analyzeCollectedElements(bindingContext) }

            // step 3: shorten elements that can be shortened right now
            visitors.forEach { it.shortenElements(elementsToUse) }

            // step 4: try to import descriptors needed to shorten other elements
            val descriptorsToImport = visitors.flatMap { it.getDescriptorsToImport() }.toSet()
            var anyChange = false
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
            if (!anyChange) break
        }

        return elementsToUse
    }

    private fun dropNestedElements(elements: List<KtElement>): LinkedHashSet<KtElement> {
        val elementSet = elements.toSet()
        return elementSet.filterTo(LinkedHashSet<KtElement>(elementSet.size)) { element ->
            element.parents.none { it in elementSet }
        }
    }

    private abstract class ShorteningVisitor<TElement : KtElement>(
            protected val file: KtFile,
            protected val elementFilter: (PsiElement) -> FilterResult,
            protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : KtVisitorVoid() {
        var options: Options = Options.DEFAULT

        private data class ElementToAnalyze<TElement>(val element: TElement, val level: Int)

        private val elementsToAnalyze = ArrayList<ElementToAnalyze<TElement>>()
        private val elementsToShorten = ArrayList<TElement>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        protected val resolutionFacade = file.getResolutionFacade()

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

        fun analyzeCollectedElements(bindingContext: BindingContext) {
            var index = 0
            while (index < elementsToAnalyze.size) {
                val (element, level) = elementsToAnalyze[index++]

                val result = analyzeQualifiedElement(element, bindingContext)

                val toBeShortened: Boolean
                when (result) {
                    is ShortenNow -> {
                        elementsToShorten.add(element)
                        toBeShortened = true
                    }

                    is ImportDescriptors -> {
                        val tryImport = result.descriptors.isNotEmpty()
                                        && result.descriptors.none { it in failedToImportDescriptors }
                                        && result.descriptors.all { mayImport(it, file) }
                        if (tryImport) {
                            descriptorsToImport.addAll(result.descriptors)
                            toBeShortened = true
                        }
                        else {
                            toBeShortened = false
                        }
                    }

                    is Skip -> {
                        toBeShortened = false
                    }
                }

                if (toBeShortened) {
                    // we are going to shorten qualified element - we must skip all elements inside its qualifier
                    while (index < elementsToAnalyze.size && elementsToAnalyze[index].level > level) {
                        index++
                    }
                }
            }
        }

        /**
         * This method is invoked for all qualified elements added by [addQualifiedElementToAnalyze]
         */
        protected abstract fun analyzeQualifiedElement(element: TElement, bindingContext: BindingContext): AnalyzeQualifiedElementResult

        protected sealed class AnalyzeQualifiedElementResult {
            object Skip : AnalyzeQualifiedElementResult()

            object ShortenNow : AnalyzeQualifiedElementResult()

            class ImportDescriptors(val descriptors: Collection<DeclarationDescriptor>) : AnalyzeQualifiedElementResult()
        }

        typealias Skip = AnalyzeQualifiedElementResult.Skip
        typealias ShortenNow = AnalyzeQualifiedElementResult.ShortenNow
        typealias ImportDescriptors = AnalyzeQualifiedElementResult.ImportDescriptors

        protected abstract fun shortenElement(element: TElement): KtElement

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        fun shortenElements(elementSetToUpdate: MutableSet<KtElement>) {
            for (element in elementsToShorten) {
                if (!element.isValid) continue

                var newElement: KtElement? = null
                // we never want any reformatting to happen because sometimes it causes strange effects (see KT-11633)
                PostprocessReformattingAspect.getInstance(element.project).disablePostprocessFormattingInside {
                    newElement = shortenElement(element)
                }

                if (element in elementSetToUpdate && newElement != element) {
                    elementSetToUpdate.remove(element)
                    elementSetToUpdate.add(newElement!!)
                }
            }
        }

        fun getElementsToAnalyze(): Collection<TElement> = elementsToAnalyze.map { it.element }
        fun getDescriptorsToImport(): Set<DeclarationDescriptor> = descriptorsToImport
    }

    private class ShortenTypesVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<KtUserType>(file, elementFilter, failedToImportDescriptors) {
        override fun visitUserType(userType: KtUserType) {
            val filterResult = elementFilter(userType)
            if (filterResult == FilterResult.SKIP) return

            userType.typeArgumentList?.accept(this)

            if (filterResult == FilterResult.PROCESS) {
                addQualifiedElementToAnalyze(userType)
            }

            // elements in qualifier must be under
            nextLevel()
            userType.qualifier?.accept(this)
            prevLevel()
        }

        override fun analyzeQualifiedElement(element: KtUserType, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
            if (element.qualifier == null) return Skip
            val referenceExpression = element.referenceExpression ?: return Skip

            val target = referenceExpression.targets(bindingContext).singleOrNull() ?: return Skip

            val scope = element.getResolutionScope(bindingContext, resolutionFacade)
            val name = target.name
            val targetByName = if (target is ClassifierDescriptor)
                scope.findClassifier(name, NoLookupLocation.FROM_IDE)
            else
                scope.findPackage(name)

            val canShortenNow = targetByName?.asString() == target.asString()
            return if (canShortenNow) ShortenNow else ImportDescriptors(target.singletonOrEmptyList())
        }

        override fun shortenElement(element: KtUserType): KtElement {
            element.deleteQualifier()
            return element
        }
    }

    private abstract class QualifiedExpressionShorteningVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<KtDotQualifiedExpression>(file, elementFilter, failedToImportDescriptors) {

        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
            val filterResult = elementFilter(expression)
            if (filterResult == FilterResult.SKIP) return

            expression.selectorExpression?.acceptChildren(this)

            if (filterResult == FilterResult.PROCESS) {
                addQualifiedElementToAnalyze(expression)
            }

            // elements in receiver must be under
            nextLevel()
            expression.receiverExpression.accept(this)
            prevLevel()
        }
    }

    private class ShortenQualifiedExpressionsVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningVisitor(file, elementFilter, failedToImportDescriptors) {

        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
            if (expression.receiverExpression is KtThisExpression && !options.removeThis) return
            super.visitDotQualifiedExpression(expression)
        }

        override fun analyzeQualifiedElement(element: KtDotQualifiedExpression, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
            val receiver = element.receiverExpression
            if (receiver !is KtThisExpression && bindingContext[BindingContext.QUALIFIER, receiver] == null) return Skip

            if (PsiTreeUtil.getParentOfType(
                    element,
                    KtImportDirective::class.java, KtPackageDirective::class.java) != null) return Skip

            val selector = element.selectorExpression ?: return Skip
            val callee = selector.getCalleeExpressionIfAny() as? KtReferenceExpression ?: return Skip
            val targets = callee.targets(bindingContext)
            if (targets.isEmpty()) return Skip

            val scope = element.getResolutionScope(bindingContext, resolutionFacade)
            val selectorCopy = selector.copy() as KtReferenceExpression
            val newContext = selectorCopy.analyzeInContext(scope, selector)
            val targetsWhenShort = (selectorCopy.getCalleeExpressionIfAny() as KtReferenceExpression).targets(newContext)
            val targetsMatch = targetsMatch(targets, targetsWhenShort)

            if (receiver is KtThisExpression) {
                if (!targetsMatch) return Skip
                val originalCall = selector.getResolvedCall(bindingContext) ?: return Skip
                val newCall = selectorCopy.getResolvedCall(newContext) ?: return Skip
                val receiverKind = originalCall.explicitReceiverKind
                val newReceiver = when (receiverKind) {
                                      ExplicitReceiverKind.BOTH_RECEIVERS, ExplicitReceiverKind.EXTENSION_RECEIVER -> newCall.extensionReceiver
                                      ExplicitReceiverKind.DISPATCH_RECEIVER -> newCall.dispatchReceiver
                                      else -> return Skip
                                  } as? ImplicitReceiver ?: return Skip

                val thisTarget = receiver.instanceReference.targets(bindingContext).singleOrNull()
                if (newReceiver.declarationDescriptor.asString() != thisTarget?.asString()) return Skip
            }

            return when {
                targetsMatch -> ShortenNow

                // it makes no sense to insert import when there is a conflict with function, property etc
                targetsWhenShort.any { it !is ClassDescriptor && it !is PackageViewDescriptor } -> Skip

                else -> ImportDescriptors(targets)
            }
        }

        private fun targetsMatch(targets1: Collection<DeclarationDescriptor>, targets2: Collection<DeclarationDescriptor>): Boolean {
            if (targets1.size != targets2.size) return false
            if (targets1.size == 1) {
                return targets1.single().asString() == targets2.single().asString()
            }
            else {
                return targets1.map { it.asString() }.toSet() == targets2.map { it.asString() }.toSet()
            }
        }

        override fun shortenElement(element: KtDotQualifiedExpression): KtElement {
            return element.replace(element.selectorExpression!!) as KtElement
        }
    }

    private class ShortenThisExpressionsVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<KtThisExpression>(file, elementFilter, failedToImportDescriptors) {
        private val simpleThis = KtPsiFactory(file).createExpression("this") as KtThisExpression

        override fun analyzeQualifiedElement(element: KtThisExpression, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
            val targetBefore = element.instanceReference.targets(bindingContext).singleOrNull() ?: return Skip
            val scope = element.getResolutionScope(bindingContext, resolutionFacade)
            val newContext = simpleThis.analyzeInContext(scope, element)
            val targetAfter = simpleThis.instanceReference.targets(newContext).singleOrNull()
            return if (targetBefore == targetAfter) ShortenNow else Skip
        }

        override fun visitThisExpression(expression: KtThisExpression) {
            if (options.removeThisLabels && elementFilter(expression) == FilterResult.PROCESS && expression.getTargetLabel() != null) {
                addQualifiedElementToAnalyze(expression)
            }
        }

        override fun shortenElement(element: KtThisExpression): KtElement {
            return element.replace(simpleThis) as KtElement
        }
    }

    private class RemoveExplicitCompanionObjectReferenceVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningVisitor(file, elementFilter, failedToImportDescriptors) {

        private fun KtExpression.singleTarget(context: BindingContext): DeclarationDescriptor? {
            return (getCalleeExpressionIfAny() as? KtReferenceExpression)?.targets(context)?.singleOrNull()
        }

        override fun analyzeQualifiedElement(element: KtDotQualifiedExpression, bindingContext: BindingContext): AnalyzeQualifiedElementResult {
            val receiver = element.receiverExpression

            if (PsiTreeUtil.getParentOfType(
                    element,
                    KtImportDirective::class.java, KtPackageDirective::class.java) != null) return Skip

            val receiverTarget = receiver.singleTarget(bindingContext) ?: return Skip
            if (receiverTarget !is ClassDescriptor) return Skip

            val selectorExpression = element.selectorExpression ?: return Skip
            val selectorTarget = selectorExpression.singleTarget(bindingContext) ?: return Skip

            if (receiverTarget.companionObjectDescriptor != selectorTarget) return Skip

            val selectorsSelector = (element.parent as? KtDotQualifiedExpression)?.selectorExpression
                                    ?: return ShortenNow

            val selectorsSelectorTarget = selectorsSelector.singleTarget(bindingContext) ?: return Skip
            if (selectorsSelectorTarget is ClassDescriptor) return Skip
            // TODO: More generic solution may be possible
            if (selectorsSelectorTarget is PropertyDescriptor) {
                val source = selectorsSelectorTarget.source.getPsi() as? KtProperty
                if (source != null && isEnumCompanionPropertyWithEntryConflict(source, source.name ?: "")) return Skip
            }

            return ShortenNow
        }

        override fun shortenElement(element: KtDotQualifiedExpression): KtElement {
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
