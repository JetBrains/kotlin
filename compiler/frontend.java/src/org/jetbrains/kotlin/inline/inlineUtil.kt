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
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility

sealed interface InlineFunctionOrAccessor {
    val jvmMethodSignature: JvmMemberSignature.Method
}

data class InlineFunction(
    override val jvmMethodSignature: JvmMemberSignature.Method,

    /** The Kotlin name of the function. It may be different from the JVM name of the function if [JvmName] is used. */
    val kotlinFunctionName: String
) : InlineFunctionOrAccessor

data class InlinePropertyAccessor(
    override val jvmMethodSignature: JvmMemberSignature.Method,

    /** The name of the property that this property accessor belongs to. */
    val propertyName: String
) : InlineFunctionOrAccessor

fun inlineFunctionsAndAccessors(header: KotlinClassHeader, excludePrivateMembers: Boolean = false): List<InlineFunctionOrAccessor> {
    val data = header.data ?: return emptyList()
    val strings = header.strings ?: return emptyList()

    return when (header.kind) {
        KotlinClassHeader.Kind.CLASS -> {
            val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)
            inlineFunctions(classProto.functionList, nameResolver, classProto.typeTable, excludePrivateMembers) +
                    inlinePropertyAccessors(classProto.propertyList, nameResolver, excludePrivateMembers)
        }
        KotlinClassHeader.Kind.FILE_FACADE,
        KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
            val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)
            inlineFunctions(packageProto.functionList, nameResolver, packageProto.typeTable, excludePrivateMembers) +
                    inlinePropertyAccessors(packageProto.propertyList, nameResolver, excludePrivateMembers)
        }
        else -> emptyList()
    }
}

private fun inlineFunctions(
    functions: List<ProtoBuf.Function>,
    nameResolver: NameResolver,
    protoTypeTable: ProtoBuf.TypeTable,
    excludePrivateFunctions: Boolean = false
): List<InlineFunction> {
    val typeTable = TypeTable(protoTypeTable)
    return functions
        .filter { Flags.IS_INLINE.get(it.flags) && (!excludePrivateFunctions || !isPrivate(it.flags)) }
        .mapNotNull { inlineFunction ->
            JvmProtoBufUtil.getJvmMethodSignature(inlineFunction, nameResolver, typeTable)?.let {
                InlineFunction(jvmMethodSignature = it, kotlinFunctionName = nameResolver.getString(inlineFunction.name))
            }
        }
}

private fun inlinePropertyAccessors(
    properties: List<ProtoBuf.Property>,
    nameResolver: NameResolver,
    excludePrivateAccessors: Boolean = false
): List<InlinePropertyAccessor> {
    val inlineAccessors = mutableListOf<InlinePropertyAccessor>()
    properties.forEach { property ->
        val propertySignature = property.getExtensionOrNull(JvmProtoBuf.propertySignature) ?: return@forEach
        if (property.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(property.getterFlags)
            && (!excludePrivateAccessors || !isPrivate(property.getterFlags))
        ) {
            val getter = propertySignature.getter
            inlineAccessors.add(
                InlinePropertyAccessor(
                    JvmMemberSignature.Method(name = nameResolver.getString(getter.name), desc = nameResolver.getString(getter.desc)),
                    propertyName = nameResolver.getString(property.name)
                )
            )
        }
        if (property.hasSetterFlags() && Flags.IS_INLINE_ACCESSOR.get(property.setterFlags)
            && (!excludePrivateAccessors || !isPrivate(property.setterFlags))
        ) {
            val setter = propertySignature.setter
            inlineAccessors.add(
                InlinePropertyAccessor(
                    JvmMemberSignature.Method(name = nameResolver.getString(setter.name), desc = nameResolver.getString(setter.desc)),
                    propertyName = nameResolver.getString(property.name)
                )
            )
        }
    }
    return inlineAccessors
}

private fun isPrivate(flags: Int) = DescriptorVisibilities.isPrivate(ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(flags)))
