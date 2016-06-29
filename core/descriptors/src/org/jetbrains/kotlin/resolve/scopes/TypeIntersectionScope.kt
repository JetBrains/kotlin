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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.selectMostSpecificInEachOverridableGroup
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class TypeIntersectionScope private constructor(override val workerScope: ChainedMemberScope) : AbstractScopeAdapter() {
    override fun getContributedFunctions(name: Name, location: LookupLocation) =
            super.getContributedFunctions(name, location).selectMostSpecificInEachOverridableGroup { this }

    override fun getContributedVariables(name: Name, location: LookupLocation) =
            super.getContributedVariables(name, location).selectMostSpecificInEachOverridableGroup { this }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val (callables, other) = super.getContributedDescriptors(kindFilter, nameFilter).partition { it is CallableDescriptor }

        @Suppress("UNCHECKED_CAST")
        return (callables as Collection<CallableDescriptor>).selectMostSpecificInEachOverridableGroup { this } + other
    }

    override fun printScopeStructure(p: Printer) {
        p.print("TypeIntersectionScope for: " + workerScope.debugName)
        super.printScopeStructure(p)
    }

    companion object {
        @JvmStatic
        fun create(message: String, types: Collection<KotlinType>): MemberScope {
            val chainedScope = ChainedMemberScope(message, types.map { it.memberScope })
            if (types.size <= 1) return chainedScope

            return TypeIntersectionScope(chainedScope)
        }
    }
}
