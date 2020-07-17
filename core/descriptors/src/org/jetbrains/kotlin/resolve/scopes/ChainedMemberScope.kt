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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.collectionUtils.getFirstClassifierDiscriminateHeaders
import org.jetbrains.kotlin.util.collectionUtils.getFromAllScopes
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.SmartList

class ChainedMemberScope private constructor(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        getFirstClassifierDiscriminateHeaders(scopes) { it.getContributedClassifier(name, location) }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        getFromAllScopes(scopes) { it.getContributedVariables(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> =
        getFromAllScopes(scopes) { it.getContributedFunctions(name, location) }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
        getFromAllScopes(scopes) { it.getContributedDescriptors(kindFilter, nameFilter) }

    override fun getFunctionNames() = scopes.flatMapTo(mutableSetOf()) { it.getFunctionNames() }
    override fun getVariableNames() = scopes.flatMapTo(mutableSetOf()) { it.getVariableNames() }
    override fun getClassifierNames(): Set<Name>? = scopes.asIterable().flatMapClassifierNamesOrNull()

    override fun recordLookup(name: Name, location: LookupLocation) {
        scopes.forEach { it.recordLookup(name, location) }
    }

    override fun toString() = debugName

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopes) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }

    companion object {
        fun create(debugName: String, vararg scopes: MemberScope): MemberScope = create(debugName, scopes.asIterable())

        fun create(debugName: String, scopes: Iterable<MemberScope>): MemberScope {
            val flattenedNonEmptyScopes = SmartList<MemberScope>()
            for (scope in scopes) {
                when {
                    scope === MemberScope.Empty -> {}
                    scope is ChainedMemberScope -> flattenedNonEmptyScopes.addAll(scope.scopes)
                    else -> flattenedNonEmptyScopes.add(scope)
                }
            }
            return createOrSingle(debugName, flattenedNonEmptyScopes)
        }

        internal fun createOrSingle(debugName: String, scopes: List<MemberScope>): MemberScope =
            when (scopes.size) {
                0 -> MemberScope.Empty
                1 -> scopes[0]
                else -> ChainedMemberScope(debugName, scopes.toTypedArray())
            }
    }
}
