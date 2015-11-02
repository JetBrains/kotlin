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
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class StaticMembersCompletion(
        private val collector: LookupElementsCollector,
        private val prefixMatcher: PrefixMatcher,
        private val indicesHelper: KotlinIndicesHelper,
        private val lookupElementFactory: LookupElementFactory
) {
    //TODO: SAM-adapters
    //TODO: different priority&behaviour when static import from the same class exist
    //TODO: filter out those that are visible from bases/imports etc
    //TODO: filter out those that are accessible from SmartCompletion.additionalItems
    fun complete() {
        val descriptorKindFilter = DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions
        val nameFilter: (String) -> Boolean = { prefixMatcher.prefixMatches(it) }

        for (member in indicesHelper.getJavaStaticMembers(descriptorKindFilter, nameFilter)) {
            collector.addElement(createLookupElement(member) ?: continue)
        }

        for (member in indicesHelper.getObjectMembers(descriptorKindFilter, nameFilter)) {
            collector.addElement(createLookupElement(member) ?: continue)
        }
    }

    private fun createLookupElement(descriptor: CallableDescriptor): LookupElement? {
        var classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return null
        if (classDescriptor.isCompanionObject) {
            classDescriptor = classDescriptor.containingDeclaration as? ClassDescriptor ?: return null
        }
        return lookupElementFactory.createLookupElement(descriptor, useReceiverTypes = false)
                .decorateAsStaticMember(descriptor, classDescriptor, classNameAsLookupString = false)
                .assignPriority(ItemPriority.STATIC_MEMBER)
                .suppressAutoInsertion()
    }
}
