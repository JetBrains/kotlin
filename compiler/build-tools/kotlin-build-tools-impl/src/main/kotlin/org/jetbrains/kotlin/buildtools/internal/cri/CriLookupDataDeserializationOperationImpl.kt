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
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.Options

internal class CriLookupDataDeserializationOperationImpl(
    private val deserializer: CriDataDeserializerImpl,
    private val data: ByteArray,
) : BuildOperationImpl<Iterable<LookupEntry>>(), CriLookupDataDeserializationOperation {
    override val options: Options = Options(CriLookupDataDeserializationOperation::class)

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): Iterable<LookupEntry> {
        return deserializer.deserializeLookupData(data)
    }
}
