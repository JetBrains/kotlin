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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiChecker
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

class FromUnresolvedNamesCompletion(
        private val collector: LookupElementsCollector,
        private val prefixMatcher: PrefixMatcher
) {
    fun addNameSuggestions(scope: KtElement, afterOffset: Int?, sampleDescriptor: DeclarationDescriptor?) {
        val names = HashSet<String>()
        scope.forEachDescendantOfType<KtNameReferenceExpression> { refExpr ->
            ProgressManager.checkCanceled()

            if (KotlinPsiChecker.wasUnresolved(refExpr)) {
                val callTypeAndReceiver = CallTypeAndReceiver.detect(refExpr)
                if (callTypeAndReceiver.receiver != null) return@forEachDescendantOfType
                if (sampleDescriptor != null) {
                    if (!callTypeAndReceiver.callType.descriptorKindFilter.accepts(sampleDescriptor)) return@forEachDescendantOfType

                    if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                        val isCall = refExpr.parent is KtCallExpression
                        val canBeUsage = when (sampleDescriptor) {
                            is FunctionDescriptor -> isCall // cannot use simply function name without arguments
                            is VariableDescriptor -> true // variable can as well be used with arguments when it has invoke()
                            is ClassDescriptor -> if (isCall) sampleDescriptor.kind == ClassKind.CLASS else sampleDescriptor.kind.isSingleton
                            else -> false // what else it can be?
                        }
                        if (!canBeUsage) return@forEachDescendantOfType
                    }
                }

                val name = refExpr.getReferencedName()
                if (!prefixMatcher.prefixMatches(name)) return@forEachDescendantOfType

                if (afterOffset != null && refExpr.startOffset < afterOffset) return@forEachDescendantOfType

                if (refExpr.mainReference.resolveToDescriptors(refExpr.analyze(BodyResolveMode.PARTIAL)).isEmpty()) {
                    names.add(name)
                }
            }
        }

        for (name in names.sorted()) {
            val lookupElement = LookupElementBuilder.create(name)
                    .suppressAutoInsertion()
                    .assignPriority(ItemPriority.FROM_UNRESOLVED_NAME_SUGGESTION)
            lookupElement.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
            collector.addElement(lookupElement)
        }
    }
}