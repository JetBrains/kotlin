/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.ArtifactTypeGrpc

enum class ArtifactType {
    SOURCE,
    DEPENDENCY,
    COMPILER_PLUGIN,
    RESULT,
}

fun ArtifactType.toGrpc(): ArtifactTypeGrpc {
    return when (this) {
        ArtifactType.SOURCE -> ArtifactTypeGrpc.SOURCE
        ArtifactType.DEPENDENCY -> ArtifactTypeGrpc.DEPENDENCY
        ArtifactType.COMPILER_PLUGIN -> ArtifactTypeGrpc.COMPILER_PLUGIN
        ArtifactType.RESULT -> ArtifactTypeGrpc.RESULT
    }
}

fun ArtifactTypeGrpc.toDomain(): ArtifactType {
    return when (this) {
        ArtifactTypeGrpc.SOURCE -> ArtifactType.SOURCE
        ArtifactTypeGrpc.DEPENDENCY -> ArtifactType.DEPENDENCY
        ArtifactTypeGrpc.COMPILER_PLUGIN -> ArtifactType.COMPILER_PLUGIN
        ArtifactTypeGrpc.RESULT -> ArtifactType.RESULT
        ArtifactTypeGrpc.UNRECOGNIZED -> ArtifactType.SOURCE // TODO double check
    }
}