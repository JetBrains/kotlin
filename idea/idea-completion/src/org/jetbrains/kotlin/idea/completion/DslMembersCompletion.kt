/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ReceiverType
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.collectAnnotationEntriesFromStubOrPsi
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils

class DslMembersCompletion(
    private val prefixMatcher: PrefixMatcher,
    private val elementFactory: LookupElementFactory,
    receiverTypes: Collection<ReceiverType>?,
    private val collector: LookupElementsCollector,
    private val indicesHelper: KotlinIndicesHelper,
    private val callTypeAndReceiver: CallTypeAndReceiver<*, *>
) {
    private val nearestReceiver = receiverTypes?.lastOrNull()
    private val nearestReceiverMarkers = nearestReceiver?.takeIf { it.implicit }
        ?.let { DslMarkerUtils.extractDslMarkerFqNames(it.type) }.orEmpty()

    private val factory = object : AbstractLookupElementFactory {
        override fun createLookupElement(
            descriptor: DeclarationDescriptor,
            useReceiverTypes: Boolean,
            qualifyNestedClasses: Boolean,
            includeClassTypeArguments: Boolean,
            parametersAndTypeGrayed: Boolean
        ): LookupElement? {
            error("Should not be called")
        }

        override fun createStandardLookupElementsForDescriptor(
            descriptor: DeclarationDescriptor,
            useReceiverTypes: Boolean
        ): Collection<LookupElement> {
            return elementFactory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes).also {
                it.forEach { element ->
                    element.isDslMember = true
                }
            }
        }
    }


    fun completeDslFunctions() {
        if (nearestReceiver == null || nearestReceiverMarkers.isEmpty()) return

        val receiverMarkersShortNames = nearestReceiverMarkers.map { it.shortName() }.distinct()
        indicesHelper.getCallableTopLevelExtensions(
            nameFilter = { prefixMatcher.prefixMatches(it) },
            declarationFilter = {
                (it as KtModifierListOwner).modifierList?.collectAnnotationEntriesFromStubOrPsi()?.any { it.shortName in receiverMarkersShortNames }
                        ?: false
            },
            callTypeAndReceiver = callTypeAndReceiver,
            receiverTypes = listOf(nearestReceiver.type)
        ).forEach { descriptor: DeclarationDescriptor ->
            collector.addDescriptorElements(descriptor, factory, notImported = true, withReceiverCast = false, prohibitDuplicates = true)
        }

        collector.flushToResultSet()
    }

}
