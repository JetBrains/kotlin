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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.sure
import javax.inject.Inject

class DeserializedDescriptorResolver(private val errorReporter: ErrorReporter) {
    lateinit var components: DeserializationComponents

    // component dependency cycle
    @Inject
    fun setComponents(components: DeserializationComponentsForJava) {
        this.components = components.components
    }

    fun resolveClass(kotlinClass: KotlinJvmBinaryClass): ClassDescriptor? {
        val classData = readClassData(kotlinClass) ?: return null
        return components.classDeserializer.deserializeClass(kotlinClass.classId, classData)
    }

    internal fun readClassData(kotlinClass: KotlinJvmBinaryClass): ClassDataWithSource? {
        val data = readData(kotlinClass, KOTLIN_CLASS) ?: return null
        val strings = kotlinClass.classHeader.strings.sure { "String table not found in $kotlinClass" }
        val classData = parseProto(kotlinClass) {
            JvmProtoBufUtil.readClassDataFrom(data, strings)
        }
        val sourceElement = KotlinJvmBinarySourceElement(kotlinClass, !IS_PRE_RELEASE && kotlinClass.classHeader.isPreRelease)
        return ClassDataWithSource(classData, sourceElement)
    }

    fun createKotlinPackagePartScope(descriptor: PackageFragmentDescriptor, kotlinClass: KotlinJvmBinaryClass): MemberScope? {
        val data = readData(kotlinClass, KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART) ?: return null
        val strings = kotlinClass.classHeader.strings.sure { "String table not found in $kotlinClass" }
        val (nameResolver, packageProto) = parseProto(kotlinClass) {
            JvmProtoBufUtil.readPackageDataFrom(data, strings)
        }
        val source = JvmPackagePartSource(kotlinClass)
        return DeserializedPackageMemberScope(descriptor, packageProto, nameResolver, source, components) {
            // All classes are included into Java scope
            emptyList()
        }
    }

    private fun readData(kotlinClass: KotlinJvmBinaryClass, expectedKinds: Set<KotlinClassHeader.Kind>): Array<String>? {
        val header = kotlinClass.classHeader
        if (!header.metadataVersion.isCompatible()) {
            errorReporter.reportIncompatibleMetadataVersion(kotlinClass.classId, kotlinClass.location, header.metadataVersion)
        }
        else if (expectedKinds.contains(header.kind)) {
            return header.data
        }

        return null
    }

    private inline fun <T> parseProto(klass: KotlinJvmBinaryClass, block: () -> T): T {
        try {
            return block()
        }
        catch (e: InvalidProtocolBufferException) {
            throw IllegalStateException("Could not read data from ${klass.location}", e)
        }
    }

    companion object {
        internal val KOTLIN_CLASS = setOf(KotlinClassHeader.Kind.CLASS)

        private val KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART =
                setOf(KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART)

        var IS_PRE_RELEASE = KotlinCompilerVersion.IS_PRE_RELEASE
            @Deprecated("Should only be used in tests") set
    }
}
