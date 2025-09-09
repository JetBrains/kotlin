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
}

fun ArtifactType.toProto(): ArtifactTypeProto {
    return when (this) {
        ArtifactType.SOURCE -> ArtifactTypeProto.SOURCE
        ArtifactType.DEPENDENCY -> ArtifactTypeProto.DEPENDENCY
        ArtifactType.COMPILER_PLUGIN -> ArtifactTypeProto.COMPILER_PLUGIN
        ArtifactType.RESULT -> ArtifactTypeProto.RESULT
    }
}

fun ArtifactTypeProto.toDomain(): ArtifactType {
    return when (this) {
        ArtifactTypeProto.SOURCE -> ArtifactType.SOURCE
        ArtifactTypeProto.DEPENDENCY -> ArtifactType.DEPENDENCY
        ArtifactTypeProto.COMPILER_PLUGIN -> ArtifactType.COMPILER_PLUGIN
        ArtifactTypeProto.RESULT -> ArtifactType.RESULT
        ArtifactTypeProto.UNRECOGNIZED -> ArtifactType.SOURCE // TODO double check
    }
}