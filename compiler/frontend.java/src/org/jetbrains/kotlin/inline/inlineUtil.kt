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

package org.jetbrains.kotlin.inline

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf.JvmMethodSignature
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility

fun inlineFunctionsAndAccessors(header: KotlinClassHeader): List<JvmMemberSignature.Method> {
    val data = header.data ?: return emptyList()
    val strings = header.strings ?: return emptyList()

    return when (header.kind) {
        KotlinClassHeader.Kind.CLASS -> {
            val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)
            inlineFunctions(classProto.functionList, nameResolver, classProto.typeTable) +
                    inlineAccessors(classProto.propertyList, nameResolver)
        }
        KotlinClassHeader.Kind.FILE_FACADE,
        KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)
            inlineFunctions(packageProto.functionList, nameResolver, packageProto.typeTable) +
                    inlineAccessors(packageProto.propertyList, nameResolver)
        }
        else -> emptyList()
    }
}

private fun inlineFunctions(
    functions: List<ProtoBuf.Function>,
    nameResolver: NameResolver,
    protoTypeTable: ProtoBuf.TypeTable
): List<JvmMemberSignature.Method> {
    val typeTable = TypeTable(protoTypeTable)
    return functions.filter { Flags.IS_INLINE.get(it.flags) }.mapNotNull {
        JvmProtoBufUtil.getJvmMethodSignature(it, nameResolver, typeTable)
    }
}

fun inlineAccessors(
    properties: List<ProtoBuf.Property>,
    nameResolver: NameResolver,
    excludePrivateAccessors: Boolean = false
): List<JvmMemberSignature.Method> {
    val inlineAccessors = mutableListOf<JvmMethodSignature>()

    fun isInline(flags: Int) = Flags.IS_INLINE_ACCESSOR.get(flags)
    fun isPrivate(flags: Int) = DescriptorVisibilities.isPrivate(ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(flags)))

    properties.forEach { property ->
        val propertySignature = property.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return@forEach

        if (property.hasGetterFlags() && isInline(property.getterFlags)) {
            if (!(excludePrivateAccessors && isPrivate(property.getterFlags))) {
                inlineAccessors.add(propertySignature.getter)
            }
        }
        if (property.hasSetterFlags() && isInline(property.setterFlags)) {
            if (!(excludePrivateAccessors && isPrivate(property.setterFlags))) {
                inlineAccessors.add(propertySignature.setter)
            }
        }
    }

    return inlineAccessors.map {
        JvmMemberSignature.Method(name = nameResolver.getString(it.name), desc = nameResolver.getString(it.desc))
    }
}
