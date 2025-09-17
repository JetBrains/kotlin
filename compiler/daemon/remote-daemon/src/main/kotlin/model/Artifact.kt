/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.ArtifactProto
import java.io.File

data class Artifact(
    val file: File,
    val types: Set<ArtifactType>
)

fun Artifact.toProto(): ArtifactProto {
    return ArtifactProto.newBuilder()
        .addAllArtifactTypes(types.map { it.toProto() })
        .setFilePath(file.path)
        .build()
}

fun ArtifactProto.toDomain(): Artifact {
    return Artifact(
        File(filePath),
        artifactTypesList.map { it.toDomain() }.toSet()
    )
}