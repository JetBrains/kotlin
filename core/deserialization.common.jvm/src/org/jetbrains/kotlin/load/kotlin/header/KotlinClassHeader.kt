/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin.header

import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.MultifileClassKind.DELEGATING
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.MultifileClassKind.INHERITING
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion

class KotlinClassHeader(
    val kind: Kind,
    val metadataVersion: JvmMetadataVersion,
    val bytecodeVersion: JvmBytecodeBinaryVersion,
    val data: Array<String>?,
    val incompatibleData: Array<String>?,
    val strings: Array<String>?,
    private val extraString: String?,
    val extraInt: Int,
    val packageName: String?
) {
    // See kotlin.Metadata
    enum class Kind(val id: Int) {
        UNKNOWN(0),
        CLASS(1),
        FILE_FACADE(2),
        SYNTHETIC_CLASS(3),
        MULTIFILE_CLASS(4),
        MULTIFILE_CLASS_PART(5);

        companion object {
            private val entryById = values().associateBy(Kind::id)

            @JvmStatic
            fun getById(id: Int) = entryById[id] ?: UNKNOWN
        }
    }

    enum class MultifileClassKind {
        DELEGATING,
        INHERITING;
    }

    val multifileClassName: String?
        get() = extraString.takeIf { kind == Kind.MULTIFILE_CLASS_PART }

    val multifilePartNames: List<String>
        get() = data.takeIf { kind == Kind.MULTIFILE_CLASS }?.asList().orEmpty()

    // TODO: use in incremental compilation
    @Suppress("unused")
    val multifileClassKind: MultifileClassKind?
        get() = if (kind == Kind.MULTIFILE_CLASS || kind == Kind.MULTIFILE_CLASS_PART) {
            if ((extraInt and JvmAnnotationNames.METADATA_MULTIFILE_PARTS_INHERIT_FLAG) != 0)
                INHERITING
            else
                DELEGATING
        } else null

    val isUnstableJvmIrBinary: Boolean
        get() = (extraInt and JvmAnnotationNames.METADATA_JVM_IR_FLAG) != 0 &&
                (extraInt and JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG == 0)

    val isPreRelease: Boolean
        get() = (extraInt and JvmAnnotationNames.METADATA_PRE_RELEASE_FLAG) != 0

    val isScript: Boolean
        get() = (extraInt and JvmAnnotationNames.METADATA_SCRIPT_FLAG) != 0

    override fun toString() = "$kind version=$metadataVersion"
}
