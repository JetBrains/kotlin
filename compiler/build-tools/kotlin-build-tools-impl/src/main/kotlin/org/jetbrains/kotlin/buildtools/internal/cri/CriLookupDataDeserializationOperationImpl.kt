/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.cri.CriLookupDataDeserializationOperation
import org.jetbrains.kotlin.buildtools.api.cri.LookupEntry
import org.jetbrains.kotlin.buildtools.cri.internal.CriDataDeserializerImpl
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl

internal class CriLookupDataDeserializationOperationImpl(
    private val data: ByteArray,
) : BuildOperationImpl<Collection<LookupEntry>>(), CriLookupDataDeserializationOperation {

    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): Collection<LookupEntry> {
        return CriDataDeserializerImpl().deserializeLookupData(data)
    }
}
