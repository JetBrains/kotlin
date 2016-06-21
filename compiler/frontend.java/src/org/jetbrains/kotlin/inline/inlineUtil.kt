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

import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.JvmMethodSignature
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

fun inlineFunctionsJvmNames(header: KotlinClassHeader): Set<String> {
    val annotationData = header.data ?: return emptySet()
    val strings = header.strings ?: return emptySet()

    return when (header.kind) {
        KotlinClassHeader.Kind.CLASS -> {
            val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(BitEncoding.decodeBytes(annotationData), strings)
            inlineFunctionsJvmNames(classProto.functionList, nameResolver, classProto.typeTable) +
            inlineAccessorsJvmNames(classProto.propertyList, nameResolver)
        }
        KotlinClassHeader.Kind.FILE_FACADE,
        KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(BitEncoding.decodeBytes(annotationData), strings)
            inlineFunctionsJvmNames(packageProto.functionList, nameResolver, packageProto.typeTable) +
            inlineAccessorsJvmNames(packageProto.propertyList, nameResolver)
        }
        else -> emptySet()
    }
}

private fun inlineFunctionsJvmNames(functions: List<ProtoBuf.Function>, nameResolver: NameResolver, protoTypeTable: ProtoBuf.TypeTable): Set<String> {
    val typeTable = TypeTable(protoTypeTable)
    val inlineFunctions = functions.filter { Flags.IS_INLINE.get(it.flags) }
    val jvmNames = inlineFunctions.mapNotNull {
        JvmProtoBufUtil.getJvmMethodSignature(it, nameResolver, typeTable)
    }
    return jvmNames.toSet()
}

private fun inlineAccessorsJvmNames(properties: List<ProtoBuf.Property>, nameResolver: NameResolver): Set<String> {
    val propertiesWithInlineAccessors = properties.filter { proto ->
        proto.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(proto.getterFlags) ||
        proto.hasSetterFlags() && Flags.IS_INLINE_ACCESSOR.get(proto.setterFlags)
    }
    val inlineAccessors = arrayListOf<JvmMethodSignature>()
    propertiesWithInlineAccessors.forEach { proto ->
        if (proto.hasExtension(JvmProtoBuf.propertySignature)) {
            val signature = proto.getExtension(JvmProtoBuf.propertySignature)
            if (proto.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(proto.getterFlags)) {
                inlineAccessors.add(signature.getter)
            }

            if (proto.hasSetterFlags() && Flags.IS_INLINE_ACCESSOR.get(proto.setterFlags)) {
                inlineAccessors.add(signature.setter)
            }
        }
    }

    return inlineAccessors.mapNotNull {
        nameResolver.getString(it.name) + nameResolver.getString(it.desc)
    }.toSet()
}
