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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.util.*
import javax.inject.Inject

class DeserializedDescriptorResolver(private val errorReporter: ErrorReporter) {
    private var components: DeserializationComponents? = null

    // component dependency cycle
    @Inject
    fun setComponents(context: DeserializationComponentsForJava) {
        this.components = context.components
    }

    fun resolveClass(kotlinClass: KotlinJvmBinaryClass): ClassDescriptor? {
        val data = readData(kotlinClass, KOTLIN_CLASS)
        if (data != null) {
            val strings = kotlinClass.classHeader.strings
            assert(strings != null) { "String table not found in " + kotlinClass }
            val classData = JvmProtoBufUtil.readClassDataFrom(data, strings)
            val sourceElement = KotlinJvmBinarySourceElement(kotlinClass)
            return components!!.classDeserializer.deserializeClass(
                    kotlinClass.classId,
                    ClassDataWithSource(classData, sourceElement))
        }
        return null
    }

    fun createKotlinPackagePartScope(descriptor: PackageFragmentDescriptor, kotlinClass: KotlinJvmBinaryClass): MemberScope? {
        val data = readData(kotlinClass, KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART)
        if (data != null) {
            val strings = kotlinClass.classHeader.strings
            assert(strings != null) { "String table not found in " + kotlinClass }
            val packageData = JvmProtoBufUtil.readPackageDataFrom(data, strings)
            val source = JvmPackagePartSource(kotlinClass.classId)
            return DeserializedPackageMemberScope(
                    descriptor, packageData.packageProto, packageData.nameResolver, source, components
            ) {
                // All classes are included into Java scope
                emptyList()
            }
        }
        return null
    }

    fun createKotlinPackageScope(
            descriptor: PackageFragmentDescriptor,
            packageParts: List<KotlinJvmBinaryClass>): MemberScope {
        val list = ArrayList<MemberScope>(packageParts.size)
        for (callable in packageParts) {
            val scope = createKotlinPackagePartScope(descriptor, callable)
            if (scope != null) {
                list.add(scope)
            }
        }
        if (list.isEmpty()) {
            return MemberScope.Empty
        }
        return ChainedMemberScope("Member scope for union of package parts data", list)
    }

    fun readData(kotlinClass: KotlinJvmBinaryClass, expectedKinds: Set<KotlinClassHeader.Kind>): Array<String>? {
        val header = kotlinClass.classHeader
        if (!header.metadataVersion.isCompatible()) {
            errorReporter.reportIncompatibleMetadataVersion(kotlinClass.classId, kotlinClass.location, header.metadataVersion)
        }
        else if (expectedKinds.contains(header.kind)) {
            return header.data
        }

        return null
    }

    companion object {

        val KOTLIN_CLASS: Set<KotlinClassHeader.Kind> = setOf<Kind>(CLASS)
        val KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART: Set<KotlinClassHeader.Kind> = setOf<Kind>(FILE_FACADE, MULTIFILE_CLASS_PART)
    }
}
