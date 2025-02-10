/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.impl.ExtraClassInfoGenerator
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.inline.InlineFunctionOrAccessor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * Minimal information about a Kotlin class to compute recompilation-triggering changes during an incremental run of the `KotlinCompile`
 * task (see [IncrementalJvmCache.saveClassToCache]).
 *
 * It's important that this class contain only the minimal required information, as it will be part of the classpath snapshot of the
 * `KotlinCompile` task and the task needs to support compile avoidance. For example, this class should contain public method signatures,
 * and should not contain private method signatures, or method implementations.
 */
class KotlinClassInfo(
    val classId: ClassId,
    val classKind: KotlinClassHeader.Kind,
    val classHeaderData: Array<String>, // Can be empty
    val classHeaderStrings: Array<String>, // Can be empty
    val multifileClassName: String?, // Not null iff classKind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
    val extraInfo: ExtraInfo
) {

    /** Extra information about a Kotlin class that is not captured in the Kotlin class metadata. */
    class ExtraInfo(

        /**
         * Snapshot of the class excluding its fields and methods and Kotlin metadata. It is not null iff
         * [classKind] == [KotlinClassHeader.Kind.CLASS].
         *
         * Note: Kotlin metadata is excluded because [ExtraInfo] is meant to contain information that supplements Kotlin metadata. (We have
         * a separate logic for comparing protos constructed from Kotlin metadata. That logic considers only changes in protos/Kotlin
         * metadata that are important for incremental compilation. If we don't exclude Kotlin metadata here, we might report a change in
         * Kotlin metadata even when the change is not important for incremental compilation.)
         *
         * TODO(KT-59292): Consider removing this info once class annotations are included in Kotlin metadata.
         */
        val classSnapshotExcludingMembers: Long?,

        /**
         * Snapshots of the class's non-private constants.
         *
         * Each entry maps a constant's name to the hash of its value.
         */
        val constantSnapshots: Map<String, Long>,

        /**
         * Snapshots of the class's non-private inline functions and property accessors.
         *
         * Each entry maps an inline function or property accessor to the hash of its corresponding method in the bytecode (including the
         * method's body).
         */
        val inlineFunctionOrAccessorSnapshots: Map<InlineFunctionOrAccessor, Long>,
    )

    val className: JvmClassName by lazy { JvmClassName.byClassId(classId) }

    val protoMapValue: ProtoMapValue by lazy {
        ProtoMapValue(
            isPackageFacade = classKind != KotlinClassHeader.Kind.CLASS,
            BitEncoding.decodeBytes(classHeaderData),
            classHeaderStrings
        )
    }

    /**
     * The [ProtoData] of this class.
     *
     * NOTE: The caller needs to ensure `classKind != KotlinClassHeader.Kind.MULTIFILE_CLASS` first, as the compiler doesn't write proto
     * data to [KotlinClassHeader.Kind.MULTIFILE_CLASS] classes.
     */
    val protoData: ProtoData by lazy {
        check(classKind != KotlinClassHeader.Kind.MULTIFILE_CLASS) {
            "Proto data is not available for KotlinClassHeader.Kind.MULTIFILE_CLASS: $classId"
        }
        protoMapValue.toProtoData(classId.packageFqName)
    }

    /** Name of the companion object of this class (default is "Companion") iff this class HAS a companion object, or null otherwise. */
    val companionObject: ClassId? by lazy {
        if (classKind == KotlinClassHeader.Kind.CLASS) {
            (protoData as ClassProtoData).getCompanionObjectName()?.let {
                classId.createNestedClassId(Name.identifier(it))
            }
        } else null
    }

    /** List of constants defined in this class iff this class IS a companion object, or null otherwise. The list could be empty. */
    val constantsInCompanionObject: List<String>? by lazy {
        if (classKind == KotlinClassHeader.Kind.CLASS) {
            val classProtoData = protoData as ClassProtoData
            if (classProtoData.proto.isCompanionObject) {
                classProtoData.getConstants()
            } else null
        } else null
    }

    companion object {

        fun createFrom(kotlinClass: LocalFileKotlinClass): KotlinClassInfo {
            return createFrom(kotlinClass.classId, kotlinClass.classHeader, kotlinClass.fileContents)
        }

        fun createFrom(classId: ClassId, classHeader: KotlinClassHeader, classContents: ByteArray): KotlinClassInfo {
            return KotlinClassInfo(
                classId,
                classHeader.kind,
                classHeader.data ?: classHeader.incompatibleData ?: emptyArray(),
                classHeader.strings ?: emptyArray(),
                classHeader.multifileClassName,
                extraInfo = ExtraClassInfoGenerator.getExtraInfo(classHeader, classContents)
            )
        }
    }
}
