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

interface ResolutionScope {
    /**
     * Returns only non-deprecated classifiers.
     *
     * See [getContributedClassifierIncludeDeprecated] to get all classifiers.
     */
    fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?

    /**
     * Returns contributed classifier, but discriminates deprecated
     *
     * This method can return some classifier where [getContributedClassifier] haven't returned any,
     * but it should never return different one, even if it is deprecated.
     * Note that implementors are encouraged to provide non-deprecated classifier if it doesn't contradict
     * contract above.
     */
    fun getContributedClassifierIncludeDeprecated(name: Name, location: LookupLocation): DescriptorWithDeprecation<ClassifierDescriptor>? =
        getContributedClassifier(name, location)?.let { DescriptorWithDeprecation.createNonDeprecated(it) }

    fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor>

    fun getContributedFunctions(name: Name, location: LookupLocation): Collection<@JvmWildcard FunctionDescriptor>

    /**
     * All visible descriptors from current scope possibly filtered by the given name and kind filters
     * (that means that the implementation is not obliged to use the filters but may do so when it gives any performance advantage).
     */
    fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
            nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER
    ): Collection<DeclarationDescriptor>

    fun definitelyDoesNotContainName(name: Name): Boolean = false

    fun recordLookup(name: Name, location: LookupLocation) {
        getContributedFunctions(name, location)
    }
}
