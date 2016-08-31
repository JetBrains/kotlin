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

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.util.Processor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinRequestResultProcessor
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.logPresentation
import org.jetbrains.kotlin.idea.search.usagesSearch.ExpressionsOfTypeProcessor.Companion.testLog
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.dataClassUtils.getComponentIndex
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.util.isValidOperator
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.util.*

private object DestructuringDeclarationSearchesInProgress : ThreadLocal<HashSet<KtDeclaration>>() {
    override fun initialValue() = HashSet<KtDeclaration>()
}

fun findDestructuringDeclarationUsages(
        componentFunction: PsiMethod,
        scope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) {
    if (componentFunction !is KtLightMethod) return //TODO
    val ktDeclarationTarget = componentFunction.kotlinOrigin as? KtDeclaration ?: return //TODO?
    findDestructuringDeclarationUsages(ktDeclarationTarget, scope, consumer, optimizer)
}

fun findDestructuringDeclarationUsages(
        targetDeclaration: KtDeclaration,
        searchScope: SearchScope,
        consumer: Processor<PsiReference>,
        optimizer: SearchRequestCollector
) {
    val inProgress = DestructuringDeclarationSearchesInProgress.get()
    try {
        if (!inProgress.add(targetDeclaration)) return

        val usePlainSearch = when (ExpressionsOfTypeProcessor.mode) {
            ExpressionsOfTypeProcessor.Mode.ALWAYS_SMART -> false
            ExpressionsOfTypeProcessor.Mode.ALWAYS_PLAIN -> true
            ExpressionsOfTypeProcessor.Mode.PLAIN_WHEN_NEEDED -> searchScope is LocalSearchScope // for local scope it's faster to use plain search
        }
        if (usePlainSearch) {
            doPlainSearch(targetDeclaration, searchScope, optimizer)
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

        val componentIndex = when (targetDeclaration) {
            is KtParameter -> targetDeclaration.dataClassComponentFunction()?.name?.asString()?.let { getComponentIndex(it) }
            is KtFunction -> targetDeclaration.name?.let { getComponentIndex(it) }
        //TODO: java component functions (see KT-13605)
            else -> null
        } ?: return

        ExpressionsOfTypeProcessor(
                dataType,
                searchScope,
                suspiciousExpressionHandler = { expression -> processSuspiciousExpression(expression, targetDeclaration, componentIndex, consumer) },
                suspiciousScopeHandler = { searchScope -> doPlainSearch(targetDeclaration, searchScope, optimizer) },
                resolutionFacade = targetDeclaration.getResolutionFacade()
        ).run()
    }
    finally {
        inProgress.remove(targetDeclaration)
    }
}

private fun processSuspiciousExpression(expression: KtExpression, targetDeclaration: KtDeclaration, componentIndex: Int, consumer: Processor<PsiReference>) {
    val parent = expression.parent
    val destructuringDeclaration = when (parent) {
        is KtDestructuringDeclaration -> parent

        is KtContainerNode -> {
            if (parent.node.elementType == KtNodeTypes.LOOP_RANGE) {
                (parent.parent as KtForExpression).destructuringParameter
            }
            else {
                null
            }
        }

        else -> null
    }

    if (destructuringDeclaration != null && componentIndex <= destructuringDeclaration.entries.size) {
        testLog?.add("Checked type of ${logPresentation(destructuringDeclaration)}")

        val declarationReference = destructuringDeclaration.entries[componentIndex - 1].references.firstIsInstance<KtDestructuringDeclarationReference>()
        if (declarationReference.isReferenceTo(targetDeclaration)) {
            consumer.process(declarationReference)
        }
    }
}

private fun doPlainSearch(ktDeclaration: KtDeclaration, scope: SearchScope, optimizer: SearchRequestCollector) {
    val unwrappedElement = ktDeclaration.namedUnwrappedElement ?: return
    val resultProcessor = KotlinRequestResultProcessor(unwrappedElement,
                                                       filter = { ref -> ref is KtDestructuringDeclarationReference })
    optimizer.searchWord("(", scope.restrictToKotlinSources(), UsageSearchContext.IN_CODE, true, unwrappedElement, resultProcessor)
}
