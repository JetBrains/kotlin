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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.Query
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import kotlin.collections.forEach as forEachStdLib

class SimplifyForInspection : IntentionBasedInspection<KtForExpression>(SimplifyForIntention())

class SimplifyForIntention : SelfTargetingRangeIntention<KtForExpression>(
        KtForExpression::class.java,
        "Simplify 'for' using destructing declaration",
        "Simplify 'for'"
) {
    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val (propertiesToRemove, removeSelectorInLoopRange) = collectPropertiesToRemove(element) ?: return

        val loopRange = element.loopRange ?: return
        val loopParameter = element.loopParameter ?: return

        loopParameter.replace(KtPsiFactory(element).createDestructuringDeclarationInFor("(${propertiesToRemove.joinToString { it.name!! }})"))
        propertiesToRemove.forEachStdLib { p -> p.delete() }

        if (removeSelectorInLoopRange && loopRange is KtDotQualifiedExpression) {
            loopRange.replace(loopRange.receiverExpression)
        }
    }

    override fun applicabilityRange(element: KtForExpression): TextRange? {
        if (element.destructuringParameter != null) return null

        if (collectPropertiesToRemove(element) != null) {
            return element.loopParameter!!.textRange
        }
        return null
    }

    // Note: list should contains properties in order to create destructing declaration
    private fun collectPropertiesToRemove(element: KtForExpression): Pair<List<KtProperty>, Boolean>? {
        val loopParameter = element.loopParameter ?: return null

        val context = element.analyzeFullyAndGetResult().bindingContext

        val loopParameterDescriptor = context.get(BindingContext.VALUE_PARAMETER, loopParameter) ?: return null
        val classDescriptor = loopParameterDescriptor.type.constructor.declarationDescriptor as? ClassDescriptor ?: return null

        var otherUsages = false
        var removeSelectorInLoopRange = false
        val propertiesToRemove: Array<KtProperty?>

        if (DescriptorUtils.isSubclass(classDescriptor, classDescriptor.builtIns.mapEntry)) {
            val loopRangeDescriptorName = element.loopRange.getResolvedCall(context)?.resultingDescriptor?.name
            if (loopRangeDescriptorName != null &&
                (loopRangeDescriptorName.asString().equals("entries") || loopRangeDescriptorName.asString().equals("entrySet"))) {
                removeSelectorInLoopRange = true
            }

            propertiesToRemove = arrayOfNulls<KtProperty>(2)

            ReferencesSearch.search(loopParameter).iterateOverMapEntryPropertiesUsages(
                    context,
                    { index, property -> propertiesToRemove[index] = property },
                    { otherUsages = true }
            )

            if (!otherUsages && propertiesToRemove.all { it != null }) {
                return propertiesToRemove.mapNotNull { it } to removeSelectorInLoopRange
            }
        }
        else if (classDescriptor.isData) {
            val valueParameters = classDescriptor.unsubstitutedPrimaryConstructor?.valueParameters ?: return null
            propertiesToRemove = arrayOfNulls<KtProperty>(valueParameters.size)

            ReferencesSearch.search(loopParameter).iterateOverDataClassPropertiesUsagesWithIndex(
                    context,
                    classDescriptor,
                    { index, property -> propertiesToRemove[index] = property },
                    { otherUsages = true }
            )

            if (otherUsages) return null

            val notNullProperties = propertiesToRemove.filterNotNull()
            val droppedLastUnused = propertiesToRemove.dropLastWhile { it == null }
            if (droppedLastUnused.size == notNullProperties.size) {
                return notNullProperties to removeSelectorInLoopRange
            }
        }

        return null
    }

    private fun Query<PsiReference>.iterateOverMapEntryPropertiesUsages(
            context: BindingContext,
            process: (Int, KtProperty) -> Unit,
            cancel: () -> Unit
    ) {
        // TODO: Remove SAM-constructor when KT-11265 will be fixed
        forEach(Processor forEach@{
            val applicableUsage = getDataIfUsageIsApplicable(it, context)
            if (applicableUsage != null) {
                val (property, descriptor) = applicableUsage
                if (descriptor.name.asString().equals("key") || descriptor.name.asString().equals("getKey")) {
                    process(0, property)
                    return@forEach true
                }
                else if (descriptor.name.asString().equals("value") || descriptor.name.asString().equals("getValue")) {
                    process(1, property)
                    return@forEach true
                }
            }

            cancel()
            return@forEach false
        })
    }

    private fun Query<PsiReference>.iterateOverDataClassPropertiesUsagesWithIndex(
            context: BindingContext,
            dataClass: ClassDescriptor,
            process: (Int, KtProperty) -> Unit,
            cancel: () -> Unit
    ) {
        val valueParameters = dataClass.unsubstitutedPrimaryConstructor?.valueParameters ?: return

        forEach(Processor forEach@{
            val applicableUsage = getDataIfUsageIsApplicable(it, context)
            if (applicableUsage != null) {
                val (property, descriptor) = applicableUsage
                for (valueParameter in valueParameters) {
                    if (context.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter) == descriptor) {
                        process(valueParameter.index, property)
                        return@forEach true
                    }
                }
            }

            cancel()
            return@forEach false
        })
    }

    private fun getDataIfUsageIsApplicable(usage: PsiReference, context: BindingContext): UsageData? {
        val parentCall = usage.element.parent as? KtExpression ?: return null
        val property = parentCall.parent as? KtProperty ?: return null
        val resolvedCall = parentCall.getResolvedCall(context) ?: return null

        val descriptor = resolvedCall.resultingDescriptor
        return UsageData(property, descriptor)
    }

    private data class UsageData(val property: KtProperty, val descriptor: CallableDescriptor)
}