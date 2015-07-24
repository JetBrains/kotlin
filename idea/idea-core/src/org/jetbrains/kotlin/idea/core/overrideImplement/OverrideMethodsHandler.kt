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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.google.common.collect.LinkedHashMultimap
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized

public class OverrideMethodsHandler : OverrideImplementMethodsHandler() {
    override fun collectMethodsToGenerate(descriptor: ClassDescriptor): Set<CallableMemberDescriptor> {
        val superMethods = collectSuperMethods(descriptor)
        for (member in descriptor.defaultType.memberScope.getAllDescriptors()) {
            if (member is CallableMemberDescriptor) {
                if (member.kind == CallableMemberDescriptor.Kind.DECLARATION) {
                    superMethods.removeAll(member.overriddenDescriptors)
                }
            }
        }

        return superMethods
                .filter { it.modality.isOverridable && !isOrOverridesSynthesized(it) }
                .toSet()
    }

    private fun collectSuperMethods(classDescriptor: ClassDescriptor): MutableSet<CallableMemberDescriptor> {
        val inheritedFunctionsSet = classDescriptor.typeConstructor.supertypes
                .flatMap { it.memberScope.getAllDescriptors() }
                .filterIsInstance<CallableMemberDescriptor>()
                .toSet()

        // Only those actually inherited
        val filteredMembers = OverrideResolver.filterOutOverridden(inheritedFunctionsSet)

        // Group members with "the same" signature
        val factoredMembers = LinkedHashMultimap.create<CallableMemberDescriptor, CallableMemberDescriptor>()
        for (one in filteredMembers) {
            if (factoredMembers.values().contains(one)) continue
            for (another in filteredMembers) {
                //                if (one == another) continue;
                factoredMembers.put(one, one)
                if (OverridingUtil.DEFAULT.isOverridableBy(one, another).result == OVERRIDABLE || OverridingUtil.DEFAULT.isOverridableBy(another, one).result == OVERRIDABLE) {
                    factoredMembers.put(one, another)
                }
            }
        }

        return factoredMembers.keySet()
    }

    override fun getChooserTitle() = "Override Members"

    override fun getNoMethodsFoundHint() = "No methods to override have been found"
}
