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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy
import org.jetbrains.kotlin.resolve.OverridingStrategy
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.ArrayList

class FunctionClassScope(
        private val storageManager: StorageManager,
        private val functionClass: FunctionClassDescriptor
) : MemberScopeImpl() {
    private val allDescriptors = storageManager.createLazyValue {
        if (functionClass.functionKind == FunctionClassDescriptor.Kind.Function) {
            val invoke = FunctionInvokeDescriptor.create(functionClass)
            (listOf(invoke) + createFakeOverrides(invoke)).toReadOnlyList()
        }
        else {
            createFakeOverrides(null).toReadOnlyList()
        }
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.CALLABLES.kindMask)) return listOf()
        return allDescriptors()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return allDescriptors().filterIsInstance<FunctionDescriptor>().filter { it.name == name }
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return allDescriptors().filterIsInstance<PropertyDescriptor>().filter { it.name == name }
    }

    private fun createFakeOverrides(invoke: FunctionDescriptor?): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>(3)
        val allSuperDescriptors = functionClass.typeConstructor.supertypes
                .flatMap { it.memberScope.getContributedDescriptors() }
                .filterIsInstance<CallableMemberDescriptor>()
        for ((name, group) in allSuperDescriptors.groupBy { it.name }) {
            for ((isFunction, descriptors) in group.groupBy { it is FunctionDescriptor }) {
                OverridingUtil.generateOverridesInFunctionGroup(
                        name,
                        /* membersFromSupertypes = */ descriptors,
                        /* membersFromCurrent = */ if (isFunction && name == invoke?.name) listOf(invoke) else listOf(),
                        functionClass,
                        object : NonReportingOverrideStrategy() {
                            override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                                OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                                result.add(fakeOverride)
                            }

                            override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                                error("Conflict in scope of $functionClass: $fromSuper vs $fromCurrent")
                            }
                        }
                )
            }
        }

        return result
    }

    override fun printScopeStructure(p: Printer) {
        p.println("Scope of function class $functionClass")
    }
}
