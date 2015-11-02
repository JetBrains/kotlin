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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import java.util.*

class StaticMembersCompletion(
        private val collector: LookupElementsCollector,
        private val prefixMatcher: PrefixMatcher,
        private val resolutionFacade: ResolutionFacade,
        private val lookupElementFactory: LookupElementFactory,
        alreadyAdded: Collection<DeclarationDescriptor>
) {
    private val alreadyAdded = alreadyAdded.mapTo(HashSet()) {
        if (it is ImportedFromObjectCallableDescriptor<*>) it.callableFromObject else it
    }

    //TODO: include it in smart completion?
    fun completeFromImports(file: KtFile) {
        val containers = file.importDirectives
                .filter { !it.isAllUnder }
                .map {
                    it.targetDescriptors(resolutionFacade).map { it.containingDeclaration }.distinct().singleOrNull() as? ClassDescriptor
                }
                .filterNotNull()
                .toSet()

        for (container in containers) {
            val memberScope = if (container.kind == ClassKind.OBJECT) container.unsubstitutedMemberScope else container.staticScope
            val members = memberScope.getDescriptorsFiltered(DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions, prefixMatcher.asNameFilter())
            for (member in members) {
                if (member is CallableDescriptor) {
                    addFromDescriptor(member, ItemPriority.STATIC_MEMBER_FROM_IMPORTS)
                }
            }
        }
    }

    //TODO: SAM-adapters
    //TODO: filter out those that are accessible from SmartCompletion.additionalItems
    //TODO: what about enum members?
    //TODO: filter out Kotlin functions from file facades
    //TODO: better presentation for lookup elements from imports too
    //TODO: better sorting
    //TODO: from the same file
    fun completeFromIndices(indicesHelper: KotlinIndicesHelper) {
        val descriptorKindFilter = DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions
        val nameFilter: (String) -> Boolean = { prefixMatcher.prefixMatches(it) }

        indicesHelper.getJavaStaticMembers(descriptorKindFilter, nameFilter).forEach { addFromDescriptor(it, ItemPriority.STATIC_MEMBER) }

        indicesHelper.getObjectMembers(descriptorKindFilter, nameFilter).forEach { addFromDescriptor(it, ItemPriority.STATIC_MEMBER) }
    }

    private fun addFromDescriptor(member: CallableDescriptor, itemPriority: ItemPriority) {
        if (member !in alreadyAdded) {
            collector.addElement(createLookupElement(member, itemPriority) ?: return)
        }
    }

    private fun createLookupElement(descriptor: CallableDescriptor, itemPriority: ItemPriority): LookupElement? {
        return lookupElementFactory.createLookupElement(descriptor, useReceiverTypes = false)
                .decorateAsStaticMember(descriptor, classNameAsLookupString = false)
                ?.assignPriority(itemPriority)
                ?.suppressAutoInsertion()
    }
}
