/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.PreReleaseInfo
import javax.inject.Inject

class DeserializedDescriptorResolver {
    lateinit var components: DeserializationComponents

    private val ownMetadataVersion: MetadataVersion get() = components.configuration.metadataVersion

    // component dependency cycle
    @Inject
    fun setComponents(components: DeserializationComponentsForJava) {
        this.components = components.components
    }

    private val skipMetadataVersionCheck: Boolean
        get() = components.configuration.skipMetadataVersionCheck

    fun resolveClass(kotlinClass: KotlinJvmBinaryClass): ClassDescriptor? {
        val classData = readClassData(kotlinClass) ?: return null
        return components.classDeserializer.deserializeClass(kotlinClass.classId, classData)
    }

    internal fun readClassData(kotlinClass: KotlinJvmBinaryClass): ClassData? {
        val data = readData(kotlinClass, KOTLIN_CLASS) ?: return null
        val strings = kotlinClass.classHeader.strings ?: return null
        val (nameResolver, classProto) = parseProto(kotlinClass) {
            JvmProtoBufUtil.readClassDataFrom(data, strings)
        } ?: return null
        val source = KotlinJvmBinarySourceElement(
            binaryClass = kotlinClass,
            incompatibility = kotlinClass.incompatibility,
            preReleaseInfo = PreReleaseInfo(isInvisible = kotlinClass.isPreReleaseInvisible),
            abiStability = kotlinClass.abiStability
        )
        return ClassData(nameResolver, classProto, kotlinClass.classHeader.metadataVersion, source)
    }

    fun createKotlinPackagePartScope(descriptor: PackageFragmentDescriptor, kotlinClass: KotlinJvmBinaryClass): MemberScope? {
        val data = readData(kotlinClass, KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART) ?: return null
        val strings = kotlinClass.classHeader.strings ?: return null
        val (nameResolver, packageProto) = parseProto(kotlinClass) {
            JvmProtoBufUtil.readPackageDataFrom(data, strings)
        } ?: return null
        val source = JvmPackagePartSource(
            kotlinClass, packageProto, nameResolver, kotlinClass.incompatibility, kotlinClass.isPreReleaseInvisible,
            kotlinClass.abiStability
        )
        return DeserializedPackageMemberScope(
            descriptor, packageProto, nameResolver, kotlinClass.classHeader.metadataVersion, source, components,
            "scope for $source in $descriptor"
        ) {
            // All classes are included into Java scope
            emptyList()
        }
    }

    private val KotlinJvmBinaryClass.incompatibility: IncompatibleVersionErrorData<MetadataVersion>?
        get() {
            if (skipMetadataVersionCheck || classHeader.metadataVersion.isCompatible(ownMetadataVersion)) return null
            return IncompatibleVersionErrorData(
                actualVersion = classHeader.metadataVersion,
                compilerVersion = MetadataVersion.INSTANCE,
                languageVersion = ownMetadataVersion,
                expectedVersion = ownMetadataVersion.lastSupportedVersionWithThisLanguageVersion(classHeader.metadataVersion.isStrictSemantics),
                filePath = location,
            )
        }

    /**
     * @return true if the class is invisible because it's compiled by a pre-release compiler, and this compiler is either released
     * or is run with a released language version.
     */
    private val KotlinJvmBinaryClass.isPreReleaseInvisible: Boolean
        get() = (components.configuration.reportErrorsOnPreReleaseDependencies &&
                (classHeader.isPreRelease || classHeader.metadataVersion == KOTLIN_1_1_EAP_METADATA_VERSION)) ||
                isCompiledWith13M1

    // We report pre-release errors on .class files produced by 1.3-M1 even if this compiler is pre-release. This is needed because
    // 1.3-M1 did not mangle names of functions mentioning inline classes yet, and we don't want to support this case in the codegen
    private val KotlinJvmBinaryClass.isCompiledWith13M1: Boolean
        get() = !components.configuration.skipPrereleaseCheck &&
                classHeader.isPreRelease && classHeader.metadataVersion == KOTLIN_1_3_M1_METADATA_VERSION

    private val KotlinJvmBinaryClass.abiStability: DeserializedContainerAbiStability
        get() = when {
            components.configuration.allowUnstableDependencies -> DeserializedContainerAbiStability.STABLE
            classHeader.isUnstableJvmIrBinary -> DeserializedContainerAbiStability.UNSTABLE
            else -> DeserializedContainerAbiStability.STABLE
        }

    private fun readData(kotlinClass: KotlinJvmBinaryClass, expectedKinds: Set<KotlinClassHeader.Kind>): Array<String>? {
        val header = kotlinClass.classHeader
        return (header.data ?: header.incompatibleData)?.takeIf { header.kind in expectedKinds }
    }

    private inline fun <T : Any> parseProto(klass: KotlinJvmBinaryClass, block: () -> T): T? =
        try {
            try {
                block()
            } catch (e: InvalidProtocolBufferException) {
                throw IllegalStateException("Could not read data from ${klass.location}", e)
            }
        } catch (e: Throwable) {
            if (skipMetadataVersionCheck || klass.classHeader.metadataVersion.isCompatible(ownMetadataVersion)) {
                throw e
            }

            // TODO: log.warn
            null
        }

    companion object {
        internal val KOTLIN_CLASS = setOf(KotlinClassHeader.Kind.CLASS)

        private val KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART =
            setOf(KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART)

        private val KOTLIN_1_1_EAP_METADATA_VERSION = MetadataVersion(1, 1, 2)

        private val KOTLIN_1_3_M1_METADATA_VERSION = MetadataVersion(1, 1, 11)

        internal val KOTLIN_1_3_RC_METADATA_VERSION = MetadataVersion(1, 1, 13)
    }
}
