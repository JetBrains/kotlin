/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.ArtifactTypeProto

enum class ArtifactType {
    SOURCE,
    DEPENDENCY,
    COMPILER_PLUGIN,
    RESULT,
    CLASSPATH_ENTRY_SNAPSHOT,
    SHRUNK_CLASSPATH_SNAPSHOT,
    IC_CACHE
}

fun ArtifactType.toProto(): ArtifactTypeProto {
    return when (this) {
        ArtifactType.SOURCE -> ArtifactTypeProto.SOURCE
        ArtifactType.DEPENDENCY -> ArtifactTypeProto.DEPENDENCY
        ArtifactType.COMPILER_PLUGIN -> ArtifactTypeProto.COMPILER_PLUGIN
        ArtifactType.CLASSPATH_ENTRY_SNAPSHOT -> ArtifactTypeProto.CLASSPATH_ENTRY_SNAPSHOT
        ArtifactType.SHRUNK_CLASSPATH_SNAPSHOT -> ArtifactTypeProto.SHRUNK_CLASSPATH_SNAPSHOT
        ArtifactType.RESULT -> ArtifactTypeProto.RESULT
        ArtifactType.IC_CACHE -> ArtifactTypeProto.IC_CACHE
    }
}

fun ArtifactTypeProto.toDomain(): ArtifactType {
    return when (this) {
        ArtifactTypeProto.SOURCE -> ArtifactType.SOURCE
        ArtifactTypeProto.DEPENDENCY -> ArtifactType.DEPENDENCY
        ArtifactTypeProto.COMPILER_PLUGIN -> ArtifactType.COMPILER_PLUGIN
        ArtifactTypeProto.RESULT -> ArtifactType.RESULT
        ArtifactTypeProto.CLASSPATH_ENTRY_SNAPSHOT -> ArtifactType.CLASSPATH_ENTRY_SNAPSHOT
        ArtifactTypeProto.SHRUNK_CLASSPATH_SNAPSHOT -> ArtifactType.SHRUNK_CLASSPATH_SNAPSHOT
        ArtifactTypeProto.IC_CACHE -> ArtifactType.IC_CACHE
        ArtifactTypeProto.UNRECOGNIZED -> ArtifactType.SOURCE // TODO double check
    }
}