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

package org.jetbrains.kotlin.load.java.descriptors

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaStaticClassScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.types.KotlinType

fun copyValueParameters(
        newValueParametersTypes: Collection<KotlinType>,
        oldValueParameters: Collection<ValueParameterDescriptor>,
        newOwner: CallableDescriptor
): List<ValueParameterDescriptor> {
    assert(newValueParametersTypes.size() == oldValueParameters.size()) {
        "Different value parameters sizes: Enhanced = ${newValueParametersTypes.size}, Old = ${oldValueParameters.size}"
    }

    return newValueParametersTypes.zip(oldValueParameters).map {
        pair ->
        val (newType, oldParameter) = pair
        ValueParameterDescriptorImpl(
                newOwner,
                oldParameter,
                oldParameter.getIndex(),
                oldParameter.getAnnotations(),
                oldParameter.getName(),
                newType,
                oldParameter.declaresDefaultValue(),
                oldParameter.isCrossinline,
                oldParameter.isNoinline,
                if (oldParameter.getVarargElementType() != null) newOwner.module.builtIns.getArrayElementType(newType) else null,
                oldParameter.getSource()
        )
    }
}

fun ClassDescriptor.getParentJavaStaticClassScope(): LazyJavaStaticClassScope? {
    val superClassDescriptor = getSuperClassNotAny() ?: return null

    val staticScope = superClassDescriptor.staticScope

    if (staticScope !is LazyJavaStaticClassScope) return superClassDescriptor.getParentJavaStaticClassScope()

    return staticScope
}

fun DeserializedCallableMemberDescriptor.getImplClassNameForDeserialized(): Name? =
        getImplClassNameForProto(this.proto, this.nameResolver)

fun getImplClassNameForProto(proto: MessageLite, nameResolver: NameResolver): Name? =
        when (proto) {
            is ProtoBuf.Constructor ->
                null
            is ProtoBuf.Function ->
                if (proto.hasExtension(JvmProtoBuf.methodImplClassName))
                    proto.getExtension(JvmProtoBuf.methodImplClassName)
                else null
            is ProtoBuf.Property ->
                if (proto.hasExtension(JvmProtoBuf.propertyImplClassName))
                    proto.getExtension(JvmProtoBuf.propertyImplClassName)
                else null
            else ->
                error("Unknown message: $proto")
        }?.let { nameResolver.getName(it) }