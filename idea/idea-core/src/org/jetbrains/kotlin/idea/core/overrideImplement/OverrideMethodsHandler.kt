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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import java.util.ArrayList
import java.util.LinkedHashMap

public class OverrideMethodsHandler : OverrideImplementMethodsHandler() {
    override fun collectMethodsToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject> {
        val result = ArrayList<OverrideMemberChooserObject>()
        for (member in descriptor.unsubstitutedMemberScope.getAllDescriptors()) {
            if (member is CallableMemberDescriptor
                && (member.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE || member.kind == CallableMemberDescriptor.Kind.DELEGATION)) {
                val overridden = member.overriddenDescriptors
                if (overridden.any { it.modality == Modality.FINAL }) continue

                val realSuperToImmediates = LinkedHashMap<CallableMemberDescriptor, MutableCollection<CallableMemberDescriptor>>()
                for (immediateSuper in overridden) {
                    for (realSuper in toRealSupers(immediateSuper)) {
                        realSuperToImmediates.getOrPut(realSuper) { ArrayList(1) }.add(immediateSuper)
                    }
                }

                val realSupers = realSuperToImmediates.keySet()
                val nonAbstractRealSupers = realSupers.filter { it.modality != Modality.ABSTRACT }
                val realSupersToUse = if (nonAbstractRealSupers.isNotEmpty()) {
                    nonAbstractRealSupers
                }
                else {
                    listOf(realSupers.first())
                }

                for (realSuper in realSupersToUse) {
                    val immediateSupers = realSuperToImmediates[realSuper]!!
                    assert(immediateSupers.isNotEmpty())

                    val immediateSuperToUse = if (immediateSupers.size() == 1) {
                        immediateSupers.single()
                    }
                    else {
                        immediateSupers.singleOrNull { (it.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.CLASS } ?: immediateSupers.first()
                    }
                    result.add(OverrideMemberChooserObject.create(project, realSuper, immediateSuperToUse))
                }
            }
        }
        return result
    }

    private fun toRealSupers(immediateSuper: CallableMemberDescriptor): Collection<CallableMemberDescriptor> {
        if (immediateSuper.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return listOf(immediateSuper)
        }
        val overridden = immediateSuper.overriddenDescriptors
        assert(overridden.isNotEmpty())
        return overridden.flatMap { toRealSupers(it) }.toSet()
    }

    override fun getChooserTitle() = "Override Members"

    override fun getNoMethodsFoundHint() = "No methods to override have been found"
}
