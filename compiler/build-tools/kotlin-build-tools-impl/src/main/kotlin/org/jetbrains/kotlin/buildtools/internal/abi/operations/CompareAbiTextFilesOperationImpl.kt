/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.operations.CompareAbiTextFilesOperation
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.ExecutionContext
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import java.nio.file.Path

@Serializable
internal class CompareAbiTextFilesOperationImpl : BaseAbiOperationImpl, CompareAbiTextFilesOperation, CompareAbiTextFilesOperation.Builder,
    DeepCopyable<CompareAbiTextFilesOperation> {

    override val expectedDumpFile: Path
    override val actualDumpFile: Path

    constructor(appendable: Appendable, expectedDumpFile: Path, actualDumpFile: Path, abiTools: AbiTools = AbiTools.getInstance()) : super(
        appendable,
        abiTools,
    ) {
        this.expectedDumpFile = expectedDumpFile
        this.actualDumpFile = actualDumpFile
    }

    private constructor(other: CompareAbiTextFilesOperationImpl) : super(other) {
        expectedDumpFile = other.expectedDumpFile
        actualDumpFile = other.actualDumpFile
    }

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext
    ) {
        val diff = abiTools.filesDiff(
            expectedDumpFile.toFile(),
            actualDumpFile.toFile(),
        )
        if (diff != null) {
            appendable.append(diff)
        }
    }

    override fun deepCopy(): CompareAbiTextFilesOperationImpl {
        return CompareAbiTextFilesOperationImpl(this)
    }

    override fun build(): CompareAbiTextFilesOperation {
        return deepCopy()
    }
}
