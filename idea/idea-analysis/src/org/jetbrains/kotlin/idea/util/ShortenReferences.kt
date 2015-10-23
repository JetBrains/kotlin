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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.getImportableTargets
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstFromMeAndParent
import org.jetbrains.kotlin.resolve.scopes.utils.getClassifier
import java.util.*

public class ShortenReferences(val options: (KtElement) -> Options = { Options.DEFAULT }) {
    public data class Options(
            val removeThisLabels: Boolean = false,
            val removeThis: Boolean = false
    ) {
        companion object {
            val DEFAULT = Options()
        }
    }

    companion object {
        val DEFAULT = ShortenReferences()

        private fun DeclarationDescriptor.asString()
                = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

        private fun KtReferenceExpression.targets(context: BindingContext) = getImportableTargets(context)

        private fun mayImport(descriptor: DeclarationDescriptor, file: KtFile): Boolean {
            return descriptor.canBeReferencedViaImport()
                   && ImportInsertHelper.getInstance(file.getProject()).mayImportOnShortenReferences(descriptor)
        }
    }

    @JvmOverloads
    public fun process(element: KtElement, elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }): KtElement {
        return process(listOf(element), elementFilter).single()
    }

    public fun process(file: KtFile, startOffset: Int, endOffset: Int) {
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
                            val calleeExpression = ((element as? KtDotQualifiedExpression)
                                    ?.getSelectorExpression() as? KtCallExpression)
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

    public enum class FilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    @JvmOverloads
    public fun process(elements: Iterable<KtElement>, elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS }): Collection<KtElement> {
        return elements.groupBy { element -> element.getContainingJetFile() }
                .flatMap { shortenReferencesInFile(it.key, it.value, elementFilter) }
    }

    private fun shortenReferencesInFile(
            file: KtFile,
            elements: List<KtElement>,
            elementFilter: (PsiElement) -> FilterResult
    ): Collection<KtElement> {
        //TODO: that's not correct since we have options!
        val elementsToUse = dropNestedElements(elements)

        val helper = ImportInsertHelper.getInstance(file.getProject())

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
                if (result != ImportInsertHelper.ImportDescriptorResult.ALREADY_IMPORTED) {
                    anyChange = true
                }
                if (result == ImportInsertHelper.ImportDescriptorResult.FAIL) {
                    failedToImportDescriptors.add(descriptor)
                }
            }
            if (!anyChange) break
        }

        return elementsToUse
    }

    private fun dropNestedElements(elements: List<KtElement>): LinkedHashSet<KtElement> {
        val elementSet = elements.toSet()
        val newElements = LinkedHashSet<KtElement>(elementSet.size())
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

    private abstract class ShorteningVisitor<T : KtElement>(
            protected val file: KtFile,
            protected val elementFilter: (PsiElement) -> FilterResult,
            protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : KtVisitorVoid() {
        var options: Options = Options.DEFAULT

        private val elementsToShorten = ArrayList<T>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        private val resolutionFacade = file.getResolutionFacade()

        protected fun analyze(element: KtElement)
                = resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)

        protected fun processQualifiedElement(element: T, target: DeclarationDescriptor, canShortenNow: Boolean) {
            if (canShortenNow) {
                addElementToShorten(element)
            }
            else if (target !in failedToImportDescriptors && mayImport(target, file)) {
                descriptorsToImport.add(target)
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

        public fun shortenElements(elementSetToUpdate: MutableSet<KtElement>) {
            for (element in elementsToShorten) {
                if (!element.isValid()) continue
                val newElement = shortenElement(element)
                if (element in elementSetToUpdate && newElement != element) {
                    elementSetToUpdate.remove(element)
                    elementSetToUpdate.add(newElement)
                }
            }
        }

        public fun getDescriptorsToImport(): Set<DeclarationDescriptor> = descriptorsToImport
    }

    private class ShortenTypesVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<KtUserType>(file, elementFilter, failedToImportDescriptors) {
        override fun visitUserType(userType: KtUserType) {
            val filterResult = elementFilter(userType)
            if (filterResult == FilterResult.SKIP) return

            userType.getTypeArgumentList()?.accept(this)

            if (filterResult == FilterResult.PROCESS) {
                processType(userType)
            }
            else {
                userType.getQualifier()?.accept(this)
            }
        }

        private fun processType(type: KtUserType) {
            if (type.getQualifier() == null) return
            val referenceExpression = type.getReferenceExpression() ?: return

            val bindingContext = analyze(referenceExpression)
            val target = referenceExpression.targets(bindingContext).singleOrNull() ?: return

            val typeReference = type.getStrictParentOfType<KtTypeReference>()!!
            val scope = bindingContext[BindingContext.LEXICAL_SCOPE, typeReference] ?: return
            val name = target.getName()
            val targetByName = if (target is ClassifierDescriptor)
                scope.getClassifier(name, NoLookupLocation.FROM_IDE)
            else
                scope.findFirstFromMeAndParent { (it as? ImportingScope)?.getPackage(name) }
            val canShortenNow = targetByName?.asString() == target.asString()

            processQualifiedElement(type, target, canShortenNow)
        }

        override fun qualifier(element: KtUserType) = element.getQualifier()!!

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

            expression.getSelectorExpression()?.acceptChildren(this)

            if (filterResult == FilterResult.PROCESS) {
                if (process(expression)) return
            }

            expression.getReceiverExpression().accept(this)
        }

        abstract fun process(qualifiedExpression: KtDotQualifiedExpression): Boolean

        override fun qualifier(element: KtDotQualifiedExpression) = element.getReceiverExpression()
    }

    private class ShortenQualifiedExpressionsVisitor(
            file: KtFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningVisitor(file, elementFilter, failedToImportDescriptors) {

        override fun process(qualifiedExpression: KtDotQualifiedExpression): Boolean {
            val bindingContext = analyze(qualifiedExpression)

            val receiver = qualifiedExpression.getReceiverExpression()
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
                    javaClass<KtImportDirective>(), javaClass<KtPackageDirective>()) != null) return true

            val selector = qualifiedExpression.getSelectorExpression() ?: return false
            val callee = selector.getCalleeExpressionIfAny() as? KtReferenceExpression ?: return false
            val target = callee.targets(bindingContext).singleOrNull() ?: return false

            val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, qualifiedExpression] ?: return false
            val selectorCopy = selector.copy() as KtReferenceExpression
            val newContext = selectorCopy.analyzeInContext(scope, selector)
            val targetsWhenShort = (selectorCopy.getCalleeExpressionIfAny() as KtReferenceExpression).targets(newContext)
            val targetsMatch = targetsWhenShort.singleOrNull()?.asString() == target.asString()

            if (receiver is KtThisExpression) {
                if (!targetsMatch) return false
                val originalCall = selector.getResolvedCall(bindingContext) ?: return false
                val newCall = selectorCopy.getResolvedCall(newContext) ?: return false
                val receiverKind = originalCall.getExplicitReceiverKind()
                val newReceiver = when (receiverKind) {
                                      ExplicitReceiverKind.BOTH_RECEIVERS, ExplicitReceiverKind.EXTENSION_RECEIVER -> newCall.getExtensionReceiver()
                                      ExplicitReceiverKind.DISPATCH_RECEIVER -> newCall.getDispatchReceiver()
                                      else -> return false
                                  } as? ThisReceiver ?: return false

                val thisTarget = receiver.getInstanceReference().targets(bindingContext).singleOrNull()
                if (newReceiver.getDeclarationDescriptor().asString() != thisTarget?.asString()) return false
            }

            if (!targetsMatch && targetsWhenShort.any { it !is ClassDescriptor && it !is PackageViewDescriptor }) {
                // it makes no sense to insert import when there is a conflict with function, property etc
                return false
            }

            processQualifiedElement(qualifiedExpression, target, targetsMatch)
            return true
        }

        override fun shortenElement(element: KtDotQualifiedExpression): KtElement {
            return element.replace(element.getSelectorExpression()!!) as KtElement
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

            val targetBefore = thisExpression.getInstanceReference().targets(bindingContext).singleOrNull() ?: return
            val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, thisExpression] ?: return
            val newContext = simpleThis.analyzeInContext(scope, thisExpression)
            val targetAfter = simpleThis.getInstanceReference().targets(newContext).singleOrNull()
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

            val receiver = qualifiedExpression.getReceiverExpression()

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    javaClass<KtImportDirective>(), javaClass<KtPackageDirective>()) != null) return false

            val receiverTarget = receiver.singleTarget(bindingContext) ?: return false
            if (receiverTarget !is ClassDescriptor) return false

            val selectorExpression = qualifiedExpression.getSelectorExpression() ?: return false
            val selectorTarget = selectorExpression.singleTarget(bindingContext) ?: return false

            if (receiverTarget.getCompanionObjectDescriptor() != selectorTarget) return false

            val selectorsSelector = (qualifiedExpression.getParent() as? KtDotQualifiedExpression)?.getSelectorExpression()
            if (selectorsSelector == null) {
                addElementToShorten(qualifiedExpression)
                return true
            }

            val selectorsSelectorTarget = selectorsSelector.singleTarget(bindingContext) ?: return false
            if (selectorsSelectorTarget is ClassDescriptor) return false

            addElementToShorten(qualifiedExpression)
            return true
        }

        override fun shortenElement(element: KtDotQualifiedExpression): KtElement {
            return element.replace(element.getReceiverExpression()) as KtElement
        }
    }
}
