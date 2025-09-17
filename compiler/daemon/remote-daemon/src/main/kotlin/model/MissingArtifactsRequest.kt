/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.server.MissingArtifactsRequestProto

data class MissingArtifactsRequest(
    val missingArtifacts: Set<Artifact>
) : CompileResponse

fun MissingArtifactsRequest.toProto(): MissingArtifactsRequestProto {
    return MissingArtifactsRequestProto
        .newBuilder()
        .addAllMissingArtifacts(missingArtifacts.map { it.toProto() })
        .build()
}

fun MissingArtifactsRequestProto.toDomain(): MissingArtifactsRequest {
    return MissingArtifactsRequest(missingArtifactsList.map { it.toDomain() }.toSet())
}