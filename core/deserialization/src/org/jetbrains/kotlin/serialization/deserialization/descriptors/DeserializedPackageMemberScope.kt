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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable

open class DeserializedPackageMemberScope(
        packageDescriptor: PackageFragmentDescriptor,
        proto: ProtoBuf.Package,
        nameResolver: NameResolver,
        containerSource: SourceElement?,
        components: DeserializationComponents,
        classNames: () -> Collection<Name>
) : DeserializedMemberScope(
        components.createContext(packageDescriptor, nameResolver, TypeTable(proto.typeTable), containerSource),
        proto.functionList, proto.propertyList, proto.typeAliasList, classNames
) {
    private val packageFqName = packageDescriptor.fqName

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = computeDescriptors(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)

    override fun hasClass(name: Name) =
            super.hasClass(name) || c.components.fictitiousClassDescriptorFactory.shouldCreateClass(packageFqName, name)

    override fun createClassId(name: Name) = ClassId(packageFqName, name)

    override fun getNonDeclaredFunctionNames(): Set<Name> = emptySet()
    override fun getNonDeclaredVariableNames(): Set<Name> = emptySet()

    override fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
        // Do nothing
    }
}
