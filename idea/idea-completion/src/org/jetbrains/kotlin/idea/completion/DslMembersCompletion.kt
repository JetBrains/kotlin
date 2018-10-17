/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.PrefixMatcher
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
    private val nearestReceiver = receiverTypes?.firstOrNull()
    private val nearestReceiverMarkers = nearestReceiver?.takeIf { it.implicit }?.extractDslMarkers().orEmpty()

    fun completeDslFunctions() {
        if (nearestReceiver == null || nearestReceiverMarkers.isEmpty()) return

        val receiverMarkersShortNames = nearestReceiverMarkers.map { it.shortName() }.distinct()
        val extensionDescriptors = indicesHelper.getCallableTopLevelExtensions(
            nameFilter = { prefixMatcher.prefixMatches(it) },
            declarationFilter = {
                (it as KtModifierListOwner).modifierList?.collectAnnotationEntriesFromStubOrPsi()?.any { it.shortName in receiverMarkersShortNames }
                        ?: false
            },
            callTypeAndReceiver = callTypeAndReceiver,
            receiverTypes = listOf(nearestReceiver.type)
        )
        extensionDescriptors.forEach {
            collector.addDescriptorElements(it, elementFactory, notImported = true, withReceiverCast = false, prohibitDuplicates = true)
        }

        collector.flushToResultSet()
    }

}
