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
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import kotlin.collections.forEach

class SimplifyForInspection : IntentionBasedInspection<KtForExpression>(SimplifyForIntention())

class SimplifyForIntention : SelfTargetingRangeIntention<KtForExpression>(
        KtForExpression::class.java,
        "Simplify 'for' using destructing declaration",
        "Simplify 'for'"
) {
    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val (usagesToRemove, removeSelectorInLoopRange) = collectUsagesToRemove(element) ?: return

        val loopRange = element.loopRange ?: return
        val loopParameter = element.loopParameter ?: return

        val factory = KtPsiFactory(element)
        loopParameter.replace(factory.createDestructuringDeclarationInFor("(${usagesToRemove.joinToString { it.name!! }})"))
        usagesToRemove.forEach { p ->
            p.properties.firstOrNull()?.delete()
            p.usersToReplace.forEach {
                it.replace(factory.createExpression(p.name!!))
            }
        }

        if (removeSelectorInLoopRange && loopRange is KtDotQualifiedExpression) {
            loopRange.replace(loopRange.receiverExpression)
        }
    }

    override fun applicabilityRange(element: KtForExpression): TextRange? {
        if (element.destructuringParameter != null) return null

        val usagesToRemove = collectUsagesToRemove(element)
        if (usagesToRemove != null && usagesToRemove.first.isNotEmpty()) {
            return element.loopParameter!!.textRange
        }
        return null
    }

    // Note: list should contains properties in order to create destructing declaration
    private fun collectUsagesToRemove(element: KtForExpression): Pair<List<UsageData>, Boolean>? {
        val loopParameter = element.loopParameter ?: return null

        val context = element.analyzeFullyAndGetResult().bindingContext

        val loopParameterDescriptor = context.get(BindingContext.VALUE_PARAMETER, loopParameter) ?: return null
        val loopParameterType = loopParameterDescriptor.type
        if (loopParameterType.isMarkedNullable) return null
        val classDescriptor = loopParameterType.constructor.declarationDescriptor as? ClassDescriptor ?: return null

        var otherUsages = false
        val usagesToRemove : Array<UsageData>

        val mapEntry = classDescriptor.builtIns.mapEntry
        val removeSelectorInLoopRange: Boolean
        if (DescriptorUtils.isSubclass(classDescriptor, mapEntry)) {
            val loopRangeDescriptorName = element.loopRange.getResolvedCall(context)?.resultingDescriptor?.name
            removeSelectorInLoopRange = loopRangeDescriptorName?.asString().let { it == "entries" || it == "entrySet" }

            usagesToRemove = Array(2, {
                UsageData(mapEntry.unsubstitutedMemberScope.getContributedVariables(Name.identifier(if (it == 0) "key" else "value"),
                                                                                    NoLookupLocation.FROM_BUILTINS).first())
            })

            ReferencesSearch.search(loopParameter).iterateOverMapEntryPropertiesUsages(
                    context,
                    { index, usageData -> usagesToRemove[index] += usageData },
                    { otherUsages = true }
            )
        }
        else if (classDescriptor.isData) {
            removeSelectorInLoopRange = false

            val valueParameters = classDescriptor.unsubstitutedPrimaryConstructor?.valueParameters ?: return null
            usagesToRemove = Array(valueParameters.size, { UsageData(valueParameters[it] )})

            ReferencesSearch.search(loopParameter).iterateOverDataClassPropertiesUsagesWithIndex(
                    context,
                    classDescriptor,
                    { index, usageData -> usagesToRemove[index] += usageData },
                    { otherUsages = true }
            )
        }
        else {
            return null
        }
        if (otherUsages) return null

        val droppedLastUnused = usagesToRemove.dropLastWhile { it.usersToReplace.isEmpty() && it.properties.isEmpty() }
        if (droppedLastUnused.all { it.properties.size <= 1 } ) {
            return droppedLastUnused to removeSelectorInLoopRange
        }

        return null
    }

    private fun Query<PsiReference>.iterateOverMapEntryPropertiesUsages(
            context: BindingContext,
            process: (Int, UsageData) -> Unit,
            cancel: () -> Unit
    ) {
        // TODO: Remove SAM-constructor when KT-11265 will be fixed
        forEach(Processor forEach@{
            val applicableUsage = getDataIfUsageIsApplicable(it, context)
            if (applicableUsage != null) {
                val descriptorName = applicableUsage.descriptor.name.asString()
                if (descriptorName.equals("key") || descriptorName.equals("getKey")) {
                    process(0, applicableUsage)
                    return@forEach true
                }
                else if (descriptorName.equals("value") || descriptorName.equals("getValue")) {
                    process(1, applicableUsage)
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
            process: (Int, UsageData) -> Unit,
            cancel: () -> Unit
    ) {
        val valueParameters = dataClass.unsubstitutedPrimaryConstructor?.valueParameters ?: return

        forEach(Processor forEach@{
            val applicableUsage = getDataIfUsageIsApplicable(it, context)
            if (applicableUsage != null) {
                for (valueParameter in valueParameters) {
                    if (context.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter) == applicableUsage.descriptor) {
                        process(valueParameter.index, applicableUsage)
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
        val userParent = parentCall.parent
        if (userParent is KtBinaryExpression) {
            if (userParent.operationToken in KtTokens.ALL_ASSIGNMENTS &&
                userParent.left === parentCall) return null
        }
        if (userParent is KtUnaryExpression) {
            if (userParent.operationToken === KtTokens.PLUSPLUS || userParent.operationToken === KtTokens.MINUSMINUS) return null
        }

        val property = parentCall.parent as? KtProperty
        if (property != null && property.isVar) return null
        val resolvedCall = parentCall.getResolvedCall(context) ?: return null

        val descriptor = resolvedCall.resultingDescriptor
        return UsageData(property, parentCall, descriptor)
    }

    private data class UsageData(val properties: List<KtProperty>,
                                 val usersToReplace: List<KtExpression>,
                                 val descriptor: CallableDescriptor,
                                 val name: String? = properties.firstOrNull()?.name
    ) {
        constructor(descriptor: CallableDescriptor):
                this(emptyList(), emptyList(), descriptor, descriptor.name.asString())

        constructor(property: KtProperty?, user: KtExpression, descriptor: CallableDescriptor):
                this(listOfNotNull(property), if (property != null) emptyList() else listOf(user), descriptor)
    }

    private operator fun UsageData?.plus(newData: UsageData): UsageData {
        if (this == null) return newData
        val allUsersToReplace = usersToReplace + newData.usersToReplace
        val allProperties = properties + newData.properties
        val name = if (properties.isNotEmpty()) name else newData.name ?: name
        return UsageData(allProperties, allUsersToReplace, descriptor, name)
    }
}