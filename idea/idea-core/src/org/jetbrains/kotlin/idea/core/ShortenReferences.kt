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
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
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
        return elements.groupBy { element -> element.getContainingKtFile() }
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
            val descriptorsToImport = visitors.flatMap { analyzeReferences(elementsToUse, it) }.toSet()
            visitors.forEach { it.shortenElements(elementsToUse) }

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
        val newElements = LinkedHashSet<KtElement>(elementSet.size)
        for (element in elementSet) {
            if (!element.parents.any { it in elementSet }) {
                newElements.add(element)
            }
        }
        return newElements
    }

    private fun analyzeReferences(elements: Iterable<KtElement>, visitor: ShorteningVisitor<*>): Set<DeclarationDescriptor> {
        for (element in elements) {
            visitor.options = options(element)
            element.accept(visitor)
        }
        return visitor.getDescriptorsToImport()
    }

    private abstract class ShorteningVisitor<in T : KtElement>(
            protected val file: KtFile,
            protected val elementFilter: (PsiElement) -> FilterResult,
            protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : KtVisitorVoid() {
        var options: Options = Options.DEFAULT

        private val elementsToShorten = ArrayList<T>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        protected val resolutionFacade = file.getResolutionFacade()

        protected fun analyze(element: KtElement)
                = resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)

        protected fun processQualifiedElement(element: T, targets: Collection<DeclarationDescriptor>, canShortenNow: Boolean) {
            if (canShortenNow) {
                addElementToShorten(element)
            }
            else if (targets.isNotEmpty() && targets.none { it in failedToImportDescriptors } && targets.all { mayImport(it, file) }) {
                descriptorsToImport.addAll(targets)
            }
            else {
                qualifier(element).accept(this)
            }
        }

        protected fun addElementToShorten(element: T) {
            elementsToShorten.add(element)
        }

        protected abstract fun qualifier(element: T): KtElement

        protected abstract fun shortenElement(element: T): KtElement

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
                processType(userType)
            }
            else {
                userType.qualifier?.accept(this)
            }
        }

        private fun processType(type: KtUserType) {
            if (type.qualifier == null) return
            val referenceExpression = type.referenceExpression ?: return

            val bindingContext = analyze(referenceExpression)
            val target = referenceExpression.targets(bindingContext).singleOrNull() ?: return

            val scope = type.getResolutionScope(bindingContext, resolutionFacade)
            val name = target.name
            val targetByName = if (target is ClassifierDescriptor)
                scope.findClassifier(name, NoLookupLocation.FROM_IDE)
            else
                scope.findPackage(name)
            val canShortenNow = targetByName?.asString() == target.asString()

            processQualifiedElement(type, target.singletonOrEmptyList(), canShortenNow)
        }

        override fun qualifier(element: KtUserType) = element.qualifier!!

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
                if (process(expression)) return
            }

            expression.receiverExpression.accept(this)
        }

        abstract fun process(qualifiedExpression: KtDotQualifiedExpression): Boolean

        override fun qualifier(element: KtDotQualifiedExpression) = element.receiverExpression
    }

    private class ShortenQualifiedExpressionsVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningVisitor(file, elementFilter, failedToImportDescriptors) {

        override fun process(qualifiedExpression: KtDotQualifiedExpression): Boolean {
            val bindingContext = analyze(qualifiedExpression)

            val receiver = qualifiedExpression.receiverExpression
            when (receiver) {
                is KtThisExpression -> {
                    if (!options.removeThis) return false
                }
                else -> {
                    if (bindingContext[BindingContext.QUALIFIER, receiver] == null) return false
                }
            }

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    KtImportDirective::class.java, KtPackageDirective::class.java) != null) return true

            val selector = qualifiedExpression.selectorExpression ?: return false
            val callee = selector.getCalleeExpressionIfAny() as? KtReferenceExpression ?: return false
            val targets = callee.targets(bindingContext)
            if (targets.isEmpty()) return false

            val scope = qualifiedExpression.getResolutionScope(bindingContext, resolutionFacade)
            val selectorCopy = selector.copy() as KtReferenceExpression
            val newContext = selectorCopy.analyzeInContext(scope, selector)
            val targetsWhenShort = (selectorCopy.getCalleeExpressionIfAny() as KtReferenceExpression).targets(newContext)
            val targetsMatch = targetsMatch(targets, targetsWhenShort)

            if (receiver is KtThisExpression) {
                if (!targetsMatch) return false
                val originalCall = selector.getResolvedCall(bindingContext) ?: return false
                val newCall = selectorCopy.getResolvedCall(newContext) ?: return false
                val receiverKind = originalCall.explicitReceiverKind
                val newReceiver = when (receiverKind) {
                                      ExplicitReceiverKind.BOTH_RECEIVERS, ExplicitReceiverKind.EXTENSION_RECEIVER -> newCall.extensionReceiver
                                      ExplicitReceiverKind.DISPATCH_RECEIVER -> newCall.dispatchReceiver
                                      else -> return false
                                  } as? ImplicitReceiver ?: return false

                val thisTarget = receiver.instanceReference.targets(bindingContext).singleOrNull()
                if (newReceiver.declarationDescriptor.asString() != thisTarget?.asString()) return false
            }

            if (!targetsMatch && targetsWhenShort.any { it !is ClassDescriptor && it !is PackageViewDescriptor }) {
                // it makes no sense to insert import when there is a conflict with function, property etc
                return false
            }

            processQualifiedElement(qualifiedExpression, targets, targetsMatch)
            return true
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

        private fun process(thisExpression: KtThisExpression) {
            if (!options.removeThisLabels || thisExpression.getTargetLabel() == null) return

            val bindingContext = analyze(thisExpression)

            val targetBefore = thisExpression.instanceReference.targets(bindingContext).singleOrNull() ?: return
            val scope = thisExpression.getResolutionScope(bindingContext, resolutionFacade)
            val newContext = simpleThis.analyzeInContext(scope, thisExpression)
            val targetAfter = simpleThis.instanceReference.targets(newContext).singleOrNull()
            if (targetBefore == targetAfter) {
                addElementToShorten(thisExpression)
            }
        }

        override fun visitThisExpression(expression: KtThisExpression) {
            if (elementFilter(expression) == FilterResult.PROCESS) {
                process(expression)
            }
        }

        override fun qualifier(element: KtThisExpression): KtElement =
                throw AssertionError("Qualifier requested: ${element.getElementTextWithContext()}")

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

        override fun process(qualifiedExpression: KtDotQualifiedExpression): Boolean {
            val bindingContext = analyze(qualifiedExpression)

            val receiver = qualifiedExpression.receiverExpression

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    KtImportDirective::class.java, KtPackageDirective::class.java) != null) return false

            val receiverTarget = receiver.singleTarget(bindingContext) ?: return false
            if (receiverTarget !is ClassDescriptor) return false

            val selectorExpression = qualifiedExpression.selectorExpression ?: return false
            val selectorTarget = selectorExpression.singleTarget(bindingContext) ?: return false

            if (receiverTarget.companionObjectDescriptor != selectorTarget) return false

            val selectorsSelector = (qualifiedExpression.parent as? KtDotQualifiedExpression)?.selectorExpression
            if (selectorsSelector == null) {
                addElementToShorten(qualifiedExpression)
                return true
            }

            val selectorsSelectorTarget = selectorsSelector.singleTarget(bindingContext) ?: return false
            if (selectorsSelectorTarget is ClassDescriptor) return false
            // TODO: More generic solution may be possible
            if (selectorsSelectorTarget is PropertyDescriptor) {
                val source = selectorsSelectorTarget.source.getPsi() as? KtProperty
                if (source != null && isEnumCompanionPropertyWithEntryConflict(source, source.name ?: "")) return false
            }

            addElementToShorten(qualifiedExpression)
            return true
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
