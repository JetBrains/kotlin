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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer
import java.util.*

class CloneableClassScope(storageManager: StorageManager, private val classDescriptor: ClassDescriptor) : MemberScopeImpl() {
    private val allDescriptors by storageManager.createLazyValue {
        val cloneFunction =
                SimpleFunctionDescriptorImpl.create(classDescriptor, Annotations.EMPTY, CLONE_NAME, DECLARATION, SourceElement.NO_SOURCE)
        cloneFunction.initialize(null, classDescriptor.thisAsReceiverParameter, emptyList(), emptyList(), classDescriptor.builtIns.anyType,
                                 Modality.OPEN, Visibilities.PROTECTED)

        listOf(cloneFunction) + createFakeOverrides()
    }

    private val functionNames = setOf(CLONE_NAME) + classDescriptor.builtIns.anyType.memberScope.getFunctionNames()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
            if (name in functionNames) allDescriptors.filter { it.name == name } else emptySet()

    override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = allDescriptors

    override fun getFunctionNames(): Set<Name> = functionNames

    private fun createFakeOverrides(): List<SimpleFunctionDescriptor> {
        val result = ArrayList<SimpleFunctionDescriptor>(3)
        val allSuperDescriptors = classDescriptor.typeConstructor.supertypes
                .flatMap { it.memberScope.getContributedDescriptors() }
                .filterIsInstance<SimpleFunctionDescriptor>()
        for ((name, descriptors) in allSuperDescriptors.groupBy { it.name }) {
            OverridingUtil.generateOverridesInFunctionGroup(
                    name,
                    /* membersFromSupertypes = */ descriptors,
                    /* membersFromCurrent = */ emptyList(),
                    classDescriptor,
                    object : NonReportingOverrideStrategy() {
                        override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                            result.add(fakeOverride as SimpleFunctionDescriptor)
                        }

                        override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                            error("Conflict in scope of $classDescriptor: $fromSuper vs $fromCurrent")
                        }
                    }
            )
        }

        return result
    }

    override fun printScopeStructure(p: Printer) {
        p.println("kotlin.Cloneable member scope")
    }

    companion object {
        internal val CLONE_NAME = Name.identifier("clone")
    }
}
