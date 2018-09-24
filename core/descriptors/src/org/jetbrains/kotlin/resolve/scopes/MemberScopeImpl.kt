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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.alwaysTrue

abstract class MemberScopeImpl : MemberScope {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = emptyList()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> = emptyList()

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = emptyList()

    override fun getFunctionNames(): Set<Name> =
            getContributedDescriptors(
                    DescriptorKindFilter.FUNCTIONS, alwaysTrue()
            ).filterIsInstance<SimpleFunctionDescriptor>().mapTo(mutableSetOf()) { it.name }

    override fun getVariableNames(): Set<Name> =
            getContributedDescriptors(
                    DescriptorKindFilter.VARIABLES, alwaysTrue()
            ).filterIsInstance<VariableDescriptor>().mapTo(mutableSetOf()) { it.name }

    override fun getClassifierNames(): Set<Name>? = null

    // This method should not be implemented here by default: every scope class has its unique structure pattern
    abstract override fun printScopeStructure(p: Printer)
}
