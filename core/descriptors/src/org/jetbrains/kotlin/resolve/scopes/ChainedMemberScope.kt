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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.collectionUtils.getFirstMatch
import org.jetbrains.kotlin.util.collectionUtils.getFromAllScopes
import org.jetbrains.kotlin.utils.Printer

class ChainedMemberScope(
        internal val debugName: String,
        private val scopes: List<MemberScope>
) : MemberScope {
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
            = getFirstMatch(scopes) { it.getContributedClassifier(name, location) }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor>
            = getFromAllScopes(scopes) { it.getContributedVariables(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor>
            = getFromAllScopes(scopes) { it.getContributedFunctions(name, location) }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = getFromAllScopes(scopes) { it.getContributedDescriptors(kindFilter, nameFilter) }

    override fun toString() = debugName

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopes) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }
}
