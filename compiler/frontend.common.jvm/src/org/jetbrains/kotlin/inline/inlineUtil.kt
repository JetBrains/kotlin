/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.inline

import org.jetbrains.kotlin.descriptors.Visibilities
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
            val [nameResolver, classProto] = JvmProtoBufUtil.readClassDataFrom(data, strings)
            inlineFunctions(classProto.functionList, nameResolver, classProto.typeTable, excludePrivateMembers) +
                    inlinePropertyAccessors(classProto.propertyList, nameResolver, excludePrivateMembers)
        }
        KotlinClassHeader.Kind.FILE_FACADE,
        KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
            val [nameResolver, packageProto] = JvmProtoBufUtil.readPackageDataFrom(data, strings)
            inlineFunctions(packageProto.functionList, nameResolver, packageProto.typeTable, excludePrivateMembers) +
                    inlinePropertyAccessors(packageProto.propertyList, nameResolver, excludePrivateMembers)
        }
        else -> emptyList()
    }
}

fun inlineFunctions(
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

fun inlinePropertyAccessors(
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

private fun isPrivate(flags: Int) = Visibilities.isPrivate(ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)))
