/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.java.JavaClassProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

// It uses BuiltInSerializerProtocol for annotations serialization
class JavaClassesSerializerExtension : KotlinSerializerExtensionBase(BuiltInSerializerProtocol) {
    override val metadataVersion: BinaryVersion
        get() = JvmMetadataVersion.INVALID_VERSION

    override fun serializeClass(
            descriptor: ClassDescriptor,
            proto: ProtoBuf.Class.Builder,
            versionRequirementTable: MutableVersionRequirementTable,
            childSerializer: DescriptorSerializer
    ) {
        super.serializeClass(descriptor, proto, versionRequirementTable, childSerializer)
        if (descriptor.visibility == JavaVisibilities.PACKAGE_VISIBILITY) {
            proto.setExtension(JavaClassProtoBuf.isPackagePrivateClass, true)
        }
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor,
                                      proto: ProtoBuf.Constructor.Builder,
                                      childSerializer: DescriptorSerializer) {
        super.serializeConstructor(descriptor, proto, childSerializer)
        if (descriptor.visibility == JavaVisibilities.PACKAGE_VISIBILITY) {
            proto.setExtension(JavaClassProtoBuf.isPackagePrivateConstructor, true)
        }
    }

    override fun serializeFunction(descriptor: FunctionDescriptor,
                                   proto: ProtoBuf.Function.Builder,
                                   childSerializer: DescriptorSerializer) {
        super.serializeFunction(descriptor, proto, childSerializer)
        if (descriptor.visibility == JavaVisibilities.PACKAGE_VISIBILITY) {
            proto.setExtension(JavaClassProtoBuf.isPackagePrivateMethod, true)
        }

        if (descriptor.dispatchReceiverParameter == null) {
            proto.setExtension(JavaClassProtoBuf.isStaticMethod, true)
        }
    }

    override fun serializeProperty(
            descriptor: PropertyDescriptor,
            proto: ProtoBuf.Property.Builder,
            versionRequirementTable: MutableVersionRequirementTable?,
            childSerializer: DescriptorSerializer
    ) {
        super.serializeProperty(descriptor, proto, versionRequirementTable, childSerializer)
        if (descriptor.visibility == JavaVisibilities.PACKAGE_VISIBILITY) {
            proto.setExtension(JavaClassProtoBuf.isPackagePrivateField, true)
        }

        if (descriptor.dispatchReceiverParameter == null) {
            proto.setExtension(JavaClassProtoBuf.isStaticField, true)
        }
    }

    override fun shouldUseNormalizedVisibility() = true

    override val customClassMembersProducer =
            object : ClassMembersProducer {
                override fun getCallableMembers(classDescriptor: ClassDescriptor) =
                        arrayListOf<CallableMemberDescriptor>().apply {
                            addAll(classDescriptor.unsubstitutedMemberScope.getSortedCallableDescriptors())
                            addAll(classDescriptor.staticScope.getSortedCallableDescriptors())
                        }
            }

    private fun MemberScope.getSortedCallableDescriptors(): Collection<CallableMemberDescriptor> =
            DescriptorUtils.getAllDescriptors(this).filterIsInstance<CallableMemberDescriptor>()
                    .let { DescriptorSerializer.sort(it) }
}
