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

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.serialization.AnnotationSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.typeUtil.isUnresolvedType

class BuiltInsSerializerExtension : KotlinSerializerExtensionBase(BuiltInSerializerProtocol) {
    private val shortNameToClassId = mapOf(
        "IntRange" to "kotlin/ranges/IntRange",
        "LongRange" to "kotlin/ranges/LongRange",
        "CharRange" to "kotlin/ranges/CharRange",
        "ExperimentalStdlibApi" to "kotlin/ExperimentalStdlibApi",
    )

    private val ignoredAnnotationShortNames = setOf(
        "JvmStatic", "JvmField", "OptIn",
    )

    override fun createAnnotationSerializer(): AnnotationSerializer = object : AnnotationSerializer(stringTable) {
        override fun getAnnotationClassId(annotation: AnnotationDescriptor): ClassId? {
            val type = annotation.type
            val annotationClass = annotation.annotationClass ?: error("Annotation type is not a class: $type")
            if (ErrorUtils.isError(annotationClass)) {
                if (type.presentableName in ignoredAnnotationShortNames) return null
                return ClassId.fromString(resolveUnresolvedType(type))
            }

            return annotationClass.classId
        }
    }

    override val metadataVersion: BinaryVersion
        get() = BuiltInsBinaryVersion.INSTANCE

    override fun shouldUseTypeTable(): Boolean = true

    override fun serializeErrorType(type: KotlinType, builder: ProtoBuf.Type.Builder) {
        val className = resolveUnresolvedType(type)
        builder.className = stringTable.getQualifiedClassNameIndex(className, false)
    }

    private fun resolveUnresolvedType(type: KotlinType): String =
        shortNameToClassId[type.presentableName]
            ?: throw UnsupportedOperationException(
                "Unsupported unresolved type: ${type.unwrap()}.\n" +
                        "Consider adding it to `BuiltInsSerializerExtension.shortNameToClassId`."
            )

    private val KotlinType.presentableName: String
        get() {
            val unwrapped = unwrap()
            if (!isUnresolvedType(unwrapped)) {
                throw UnsupportedOperationException("Error types which are not unresolved type instances are not supported here: $unwrapped")
            }
            return unwrapped.debugMessage.removePrefix("Unresolved type for ")
        }
}
