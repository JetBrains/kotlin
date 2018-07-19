/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.protobuf.MessageLite

class VersionRequirementTable private constructor(private val infos: List<ProtoBuf.VersionRequirement>) {
    operator fun get(id: Int): ProtoBuf.VersionRequirement? = infos.getOrNull(id)

    companion object {
        val EMPTY = VersionRequirementTable(emptyList())

        fun create(table: ProtoBuf.VersionRequirementTable): VersionRequirementTable =
            if (table.requirementCount == 0) EMPTY else VersionRequirementTable(
                table.requirementList
            )
    }
}

class VersionRequirement(
    val version: Version,
    val kind: ProtoBuf.VersionRequirement.VersionKind,
    val level: DeprecationLevel,
    val errorCode: Int?,
    val message: String?
) {
    data class Version(val major: Int, val minor: Int, val patch: Int = 0) {
        fun asString(): String =
            if (patch == 0) "$major.$minor" else "$major.$minor.$patch"

        fun encode(
            writeVersion: (Int) -> Unit,
            writeVersionFull: (Int) -> Unit
        ) = when {
            this == INFINITY -> {
                // Do nothing: absence of version means INFINITY
            }
            major > MAJOR_MASK || minor > MINOR_MASK || patch > PATCH_MASK -> {
                writeVersionFull(major or (minor shl 8) or (patch shl 16))
            }
            else -> {
                writeVersion(major or (minor shl MAJOR_BITS) or (patch shl (MAJOR_BITS + MINOR_BITS)))
            }
        }

        override fun toString(): String = asString()

        companion object {
            @JvmField
            val INFINITY = Version(256, 256, 256)

            // Number of bits used for major, minor and patch components in "version" field
            private const val MAJOR_BITS = 3
            private const val MINOR_BITS = 4
            private const val PATCH_BITS = 7
            private const val MAJOR_MASK = (1 shl MAJOR_BITS) - 1
            private const val MINOR_MASK = (1 shl MINOR_BITS) - 1
            private const val PATCH_MASK = (1 shl PATCH_BITS) - 1

            fun decode(version: Int?, versionFull: Int?): Version = when {
                versionFull != null -> Version(
                    major = versionFull and 255,
                    minor = (versionFull shr 8) and 255,
                    patch = (versionFull shr 16) and 255
                )
                version != null -> Version(
                    major = version and MAJOR_MASK,
                    minor = (version shr MAJOR_BITS) and MINOR_MASK,
                    patch = (version shr (MAJOR_BITS + MINOR_BITS)) and PATCH_MASK
                )
                else -> INFINITY
            }
        }
    }

    override fun toString(): String =
        "since $version $level" + (if (errorCode != null) " error $errorCode" else "") + (if (message != null) ": $message" else "")

    companion object {
        fun create(proto: MessageLite, nameResolver: NameResolver, table: VersionRequirementTable): List<VersionRequirement> {
            val ids = when (proto) {
                is ProtoBuf.Class -> proto.versionRequirementList
                is ProtoBuf.Constructor -> proto.versionRequirementList
                is ProtoBuf.Function -> proto.versionRequirementList
                is ProtoBuf.Property -> proto.versionRequirementList
                is ProtoBuf.TypeAlias -> proto.versionRequirementList
                else -> throw IllegalStateException("Unexpected declaration: ${proto::class.java}")
            }

            return ids.mapNotNull { id -> create(id, nameResolver, table) }
        }

        fun create(id: Int, nameResolver: NameResolver, table: VersionRequirementTable): VersionRequirement? {
            val info = table[id] ?: return null

            val version = Version.decode(
                if (info.hasVersion()) info.version else null,
                if (info.hasVersionFull()) info.versionFull else null
            )

            val level = when (info.level!!) {
                ProtoBuf.VersionRequirement.Level.WARNING -> DeprecationLevel.WARNING
                ProtoBuf.VersionRequirement.Level.ERROR -> DeprecationLevel.ERROR
                ProtoBuf.VersionRequirement.Level.HIDDEN -> DeprecationLevel.HIDDEN
            }

            val errorCode = if (info.hasErrorCode()) info.errorCode else null

            val message = if (info.hasMessage()) nameResolver.getString(info.message) else null

            return VersionRequirement(version, info.versionKind, level, errorCode, message)
        }
    }
}
