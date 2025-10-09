/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.cri.CriSubtypeDataDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.cri.SubtypeEntry
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl

internal class CriSubtypeDataDeserializationOperationImpl(
    private val deserializer: CriDataDeserializerImpl,
    private val data: ByteArray,
) : BuildOperationImpl<Collection<SubtypeEntry>>(), CriSubtypeDataDeserializationOperation {

    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): Collection<SubtypeEntry> {
        return deserializer.deserializeSubtypeData(data)
    }
}
