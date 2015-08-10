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
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.ArrayList

class FunctionClassScope(
        private val storageManager: StorageManager,
        private val functionClass: FunctionClassDescriptor
) : JetScopeImpl() {
    private val allDescriptors = storageManager.createLazyValue {
        if (functionClass.functionKind == FunctionClassDescriptor.Kind.Function) {
            val invoke = FunctionInvokeDescriptor.create(functionClass)
            (listOf(invoke) + createFakeOverrides(invoke)).toReadOnlyList()
        }
        else {
            createFakeOverrides(null).toReadOnlyList()
        }
    }

    override fun getContainingDeclaration() = functionClass

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.CALLABLES.kindMask)) return listOf()
        return allDescriptors()
    }

    override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return allDescriptors().filterIsInstance<FunctionDescriptor>().filter { it.getName() == name }
    }

    override fun getProperties(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return allDescriptors().filterIsInstance<PropertyDescriptor>().filter { it.getName() == name }
    }

    private fun createFakeOverrides(invoke: FunctionDescriptor?): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>(3)
        val allSuperDescriptors = functionClass.getTypeConstructor().getSupertypes()
                .flatMap { it.getMemberScope().getAllDescriptors() }
                .filterIsInstance<CallableMemberDescriptor>()
        for ((name, group) in allSuperDescriptors.groupBy { it.getName() }) {
            for ((isFunction, descriptors) in group.groupBy { it is FunctionDescriptor }) {
                OverridingUtil.generateOverridesInFunctionGroup(
                        name,
                        /* membersFromSupertypes = */ descriptors,
                        /* membersFromCurrent = */ if (isFunction && name == invoke?.getName()) listOf(invoke) else listOf(),
                        functionClass,
                        object : OverridingUtil.DescriptorSink {
                            override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                                OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                                result.add(fakeOverride)
                            }

                            override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                                error("Conflict in scope of ${getContainingDeclaration()}: $fromSuper vs $fromCurrent")
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
