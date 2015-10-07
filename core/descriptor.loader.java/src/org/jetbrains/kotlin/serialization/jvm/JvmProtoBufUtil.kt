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

package org.jetbrains.kotlin.serialization.jvm

import com.google.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.load.kotlin.JvmNameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import java.io.ByteArrayInputStream

public object JvmProtoBufUtil {
    public val EXTENSION_REGISTRY: ExtensionRegistryLite = run {
        val registry = ExtensionRegistryLite.newInstance()
        JvmProtoBuf.registerAllExtensions(registry)
        registry
    }

    @JvmStatic
    public fun readClassDataFrom(data: Array<String>, strings: Array<String>): ClassData =
            readClassDataFrom(BitEncoding.decodeBytes(data), strings)

    @JvmStatic
    public fun readClassDataFrom(bytes: ByteArray, strings: Array<String>): ClassData {
        val input = ByteArrayInputStream(bytes)
        val nameResolver = JvmNameResolver(JvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, EXTENSION_REGISTRY), strings)
        val classProto = ProtoBuf.Class.parseFrom(input, EXTENSION_REGISTRY)
        return ClassData(nameResolver, classProto)
    }

    @JvmStatic
    public fun readPackageDataFrom(data: Array<String>, strings: Array<String>): PackageData =
            readPackageDataFrom(BitEncoding.decodeBytes(data), strings)

    @JvmStatic
    public fun readPackageDataFrom(bytes: ByteArray, strings: Array<String>): PackageData {
        val input = ByteArrayInputStream(bytes)
        val nameResolver = JvmNameResolver(JvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, EXTENSION_REGISTRY), strings)
        val packageProto = ProtoBuf.Package.parseFrom(input, EXTENSION_REGISTRY)
        return PackageData(nameResolver, packageProto)
    }

    // returns JVM signature in the format: "equals(Ljava/lang/Object;)Z"
    fun getJvmMethodSignature(proto: ProtoBuf.FunctionOrBuilder, nameResolver: NameResolver): String? {
        val signature =
                if (proto.hasExtension(JvmProtoBuf.methodSignature)) proto.getExtension(JvmProtoBuf.methodSignature) else null
        val name = if (signature != null && signature.hasName()) signature.name else proto.name
        val desc = if (signature != null && signature.hasDesc()) {
            nameResolver.getString(signature.desc)
        }
        else {
            val parameterTypes =
                    (if (proto.hasReceiverType()) listOf(proto.receiverType) else listOf()) + proto.valueParameterList.map { it.type }

            val parametersDesc = parameterTypes.map { mapTypeDefault(it, nameResolver) ?: return null }
            val returnTypeDesc = mapTypeDefault(proto.returnType, nameResolver) ?: return null

            parametersDesc.joinToString(separator = "", prefix = "(", postfix = ")") + returnTypeDesc
        }
        return nameResolver.getString(name) + desc
    }

    fun getJvmConstructorSignature(proto: ProtoBuf.ConstructorOrBuilder, nameResolver: NameResolver): String? {
        val signature =
                if (proto.hasExtension(JvmProtoBuf.constructorSignature)) proto.getExtension(JvmProtoBuf.constructorSignature) else null
        val desc = if (signature != null && signature.hasDesc()) {
            nameResolver.getString(signature.desc)
        }
        else {
            proto.valueParameterList.map {
                mapTypeDefault(it.type, nameResolver) ?: return null
            }.joinToString(separator = "", prefix = "(", postfix = ")V")
        }
        return "<init>" + desc
    }

    private fun mapTypeDefault(type: ProtoBuf.Type, nameResolver: NameResolver): String? {
        return if (type.hasClassName()) mapClassIdDefault(nameResolver.getClassId(type.className)) else null
    }

    @JvmStatic
    fun mapClassIdDefault(classId: ClassId): String {
        val internalName = classId.asString().replace('.', '$')
        val simpleName = internalName.removePrefix("kotlin/")
        if (simpleName != internalName) {
            for (jvmPrimitive in JvmPrimitiveType.values()) {
                val primitiveType = jvmPrimitive.primitiveType
                if (simpleName == primitiveType.typeName.asString()) return jvmPrimitive.desc
                if (simpleName == primitiveType.arrayTypeName.asString()) return "[" + jvmPrimitive.desc
            }

            if (simpleName == KotlinBuiltIns.FQ_NAMES.unit.shortName().asString()) return "V"
        }

        val javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(classId.asSingleFqName().toUnsafe())
        if (javaClassId != null) {
            return "L" + javaClassId.asString().replace('.', '$') + ";"
        }

        return "L$internalName;"
    }
}
