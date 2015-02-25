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

import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import java.util.LinkedHashSet
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import java.util.ArrayList
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options
import org.jetbrains.kotlin.idea.imports.*
import org.jetbrains.kotlin.resolve.*
import com.intellij.psi.*

public class ShortenReferences(val options: (JetElement) -> Options = { Options.DEFAULT }) {
    public data class Options(
            val removeThisLabels: Boolean = false,
            val removeThis: Boolean = false
    ) {
        class object {
            val DEFAULT = Options()
        }
    }

    class object {
        val DEFAULT = ShortenReferences()

        private fun DeclarationDescriptor.asString()
                = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

        private fun JetReferenceExpression.targets(context: BindingContext) = getImportableTargets(context)

        private fun mayImport(descriptor: DeclarationDescriptor, file: JetFile): Boolean {
            return descriptor.canBeReferencedViaImport()
                   && ImportInsertHelper.getInstance(file.getProject()).mayImportByCodeStyle(descriptor)
        }
    }

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
        //TODO: that's not correct since we have options!
        val elementsToUse = dropNestedElements(elements)

        val importInserter = ImportInserter(file)

        val failedToImportDescriptors = LinkedHashSet<DeclarationDescriptor>()

        while (true) {
            // Visitor order is important here so that enclosing elements are not shortened before their children are, e.g.
            // test.foo(this@A) -> foo(this)
            val visitors: List<ShorteningVisitor<*>> = listOf(
                    ShortenTypesVisitor(file, elementFilter, failedToImportDescriptors),
                    ShortenThisExpressionsVisitor(file, elementFilter, failedToImportDescriptors),
                    ShortenQualifiedExpressionsVisitor(file, elementFilter, failedToImportDescriptors),
                    RemoveExplicitDefaultObjectReferenceVisitor(file, elementFilter, failedToImportDescriptors)
            )
            val descriptorsToImport = visitors.flatMap { analyzeReferences(elementsToUse, it) }.toSet()
            visitors.forEach { it.shortenElements(elementsToUse) }

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

    private fun dropNestedElements(elements: List<JetElement>): LinkedHashSet<JetElement> {
        val elementSet = elements.toSet()
        val newElements = LinkedHashSet<JetElement>(elementSet.size())
        for (element in elementSet) {
            if (!element.parents(withItself = false).any { it in elementSet }) {
                newElements.add(element)
            }
        }
        return newElements
    }

    private fun analyzeReferences(elements: Iterable<JetElement>, visitor: ShorteningVisitor<*>): Set<DeclarationDescriptor> {
        for (element in elements) {
            visitor.options = options(element)
            element.accept(visitor)
        }
        return visitor.getDescriptorsToImport()
    }

    private abstract class ShorteningVisitor<T : JetElement>(
            protected val file: JetFile,
            protected val elementFilter: (PsiElement) -> FilterResult,
            protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : JetVisitorVoid() {
        var options: Options = Options.DEFAULT

        private val elementsToShorten = ArrayList<T>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        protected val resolutionFacade: ResolutionFacade = file.getResolutionFacade()

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

        protected abstract fun qualifier(element: T): JetElement

        protected abstract fun shortenElement(element: T): JetElement

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        public fun shortenElements(elementSetToUpdate: MutableSet<JetElement>) {
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
            else {
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

        override fun shortenElement(element: JetUserType): JetElement {
            element.deleteQualifier()
            return element
        }
    }

    private abstract class QualifiedExpressionShorteningVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<JetDotQualifiedExpression>(file, elementFilter, failedToImportDescriptors) {
        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            val filterResult = elementFilter(expression)
            if (filterResult == FilterResult.SKIP) return

            expression.getSelectorExpression()?.acceptChildren(this)

            if (filterResult == FilterResult.PROCESS) {
                if (process(expression)) return
            }

            expression.getReceiverExpression().accept(this)
        }

        abstract fun process(qualifiedExpression: JetDotQualifiedExpression): Boolean

        override fun qualifier(element: JetDotQualifiedExpression) = element.getReceiverExpression()
    }

    private class ShortenQualifiedExpressionsVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningVisitor(file, elementFilter, failedToImportDescriptors) {

        override fun process(qualifiedExpression: JetDotQualifiedExpression): Boolean {
            val bindingContext = resolutionFacade.analyze(qualifiedExpression)

            val receiver = qualifiedExpression.getReceiverExpression()
            when {
                receiver is JetThisExpression -> {
                    if (!options.removeThis) return false
                }
                else -> {
                    if (bindingContext[BindingContext.QUALIFIER, receiver] == null) return false
                }
            }

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
            val targetsMatch = targetsWhenShort.singleOrNull()?.asString() == target.asString()

            if (receiver is JetThisExpression) {
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

        override fun shortenElement(element: JetDotQualifiedExpression): JetElement {
            return element.replace(element.getSelectorExpression()!!) as JetElement
        }
    }

    private class ShortenThisExpressionsVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningVisitor<JetThisExpression>(file, elementFilter, failedToImportDescriptors) {
        private val simpleThis = JetPsiFactory(file).createExpression("this") as JetThisExpression

        private fun process(thisExpression: JetThisExpression) {
            if (!options.removeThisLabels || thisExpression.getTargetLabel() == null) return

            val bindingContext = resolutionFacade.analyze(thisExpression)

            val targetBefore = thisExpression.getInstanceReference().targets(bindingContext).singleOrNull() ?: return
            val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, thisExpression] ?: return
            val newContext = simpleThis.analyzeInContext(scope)
            val targetAfter = simpleThis.getInstanceReference().targets(newContext).singleOrNull()
            if (targetBefore == targetAfter) {
                addElementToShorten(thisExpression)
            }
        }

        override fun visitThisExpression(expression: JetThisExpression) {
            if (elementFilter(expression) == FilterResult.PROCESS) {
                process(expression)
            }
        }

        override fun qualifier(element: JetThisExpression): JetElement =
                throw AssertionError("Qualifier requested: ${JetPsiUtil.getElementTextWithContext(element)}")

        override fun shortenElement(element: JetThisExpression): JetElement {
            return element.replace(simpleThis) as JetElement
        }
    }

    private class RemoveExplicitDefaultObjectReferenceVisitor(
            file: JetFile,
            elementFilter: (PsiElement) -> FilterResult,
            failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningVisitor(file, elementFilter, failedToImportDescriptors) {

        private fun JetExpression.singleTarget(context: BindingContext): DeclarationDescriptor? {
            return (getCalleeExpressionIfAny() as? JetReferenceExpression)?.targets(context)?.singleOrNull()
        }

        override fun process(qualifiedExpression: JetDotQualifiedExpression): Boolean {
            val bindingContext = resolutionFacade.analyze(qualifiedExpression)

            val receiver = qualifiedExpression.getReceiverExpression()

            if (PsiTreeUtil.getParentOfType(
                    qualifiedExpression,
                    javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) != null) return false

            val receiverTarget = receiver.singleTarget(bindingContext) ?: return false
            if (receiverTarget !is ClassDescriptor) return false

            val selectorExpression = qualifiedExpression.getSelectorExpression() ?: return false
            val selectorTarget = selectorExpression.singleTarget(bindingContext) ?: return false

            if (receiverTarget.getDefaultObjectDescriptor() != selectorTarget) return false

            val selectorsSelector = (qualifiedExpression.getParent() as? JetDotQualifiedExpression)?.getSelectorExpression()
            if (selectorsSelector == null) {
                addElementToShorten(qualifiedExpression)
                return true
            }

            val selectorsSelectorTarget = selectorsSelector.singleTarget(bindingContext) ?: return false
            if (selectorsSelectorTarget is ClassDescriptor) return false

            addElementToShorten(qualifiedExpression)
            return true
        }

        override fun shortenElement(element: JetDotQualifiedExpression): JetElement {
            return element.replace(element.getReceiverExpression()) as JetElement
        }
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
