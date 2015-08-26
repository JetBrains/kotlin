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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.utils.Printer
import java.util.*

public open class DeserializedNewPackageMemberScope(
        val packageDescriptor: PackageFragmentDescriptor,
        val membersList: List<JetScope>
) : JetScopeImpl() {

    override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> = sort(membersList.flatMap { it.getFunctions(name, location) })

    override fun getProperties(name: Name, location: LookupLocation): Collection<VariableDescriptor> = sort(membersList.flatMap { it.getProperties(name, location) })

    override fun getClassifier(name: Name, location: LookupLocation) = membersList.asSequence().map { it.getClassifier(name, location) }.filterNotNull().firstOrNull()

    override fun getContainingDeclaration() = packageDescriptor

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> {
        return listOf()
    }

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("containingDeclaration = " + getContainingDeclaration())

        p.popIndent()
        p.println("}")
    }

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = sort(membersList.flatMap { it.getDescriptors(kindFilter, nameFilter) })


    public fun <T : DeclarationDescriptor> sort(descriptors: Collection<T>): List<T> {
        val result = ArrayList(descriptors)
        //NOTE: the exact comparator does matter here
        Collections.sort(result, MemberComparator.INSTANCE)
        return result
    }
}
