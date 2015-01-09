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

import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.deserialization.NameResolver

public open class DeserializedPackageMemberScope(
        packageDescriptor: PackageFragmentDescriptor,
        proto: ProtoBuf.Package,
        nameResolver: NameResolver,
        components: DeserializationComponents,
        classNames: () -> Collection<Name>
) : DeserializedMemberScope(components.createContext(packageDescriptor, nameResolver), proto.getMemberList()) {

    private val packageFqName = packageDescriptor.fqName
    private val classNames = c.storageManager.createLazyValue(classNames)

    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = computeDescriptors(kindFilter, nameFilter)

    override fun getClassDescriptor(name: Name) = c.components.deserializeClass(ClassId(packageFqName, name))

    override fun addClassDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
        for (className in classNames()) {
            if (nameFilter(className)) {
                result.addIfNotNull(getClassDescriptor(className))
            }
        }
    }

    override fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>) {
        // Do nothing
    }

    override fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
        // Do nothing
    }

    override fun getImplicitReceiver(): ReceiverParameterDescriptor? = null
}
