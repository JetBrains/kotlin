/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable

object VersionRequirementUtils {
    fun writeVersionRequirement(
        major: Int,
        minor: Int,
        patch: Int,
        versionKind: ProtoBuf.VersionRequirement.VersionKind,
        versionRequirementTable: MutableVersionRequirementTable
    ): Int {
        val requirement = ProtoBuf.VersionRequirement.newBuilder().apply {
            VersionRequirement.Version(major, minor, patch).encode(
                writeVersion = { version = it },
                writeVersionFull = { versionFull = it }
            )
            if (versionKind != defaultInstanceForType.versionKind) {
                this.versionKind = versionKind
            }
        }
        return versionRequirementTable[requirement]
    }

}
