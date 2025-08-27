/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationCompareDumpFormatV2
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import java.io.File

internal class AbiValidationCompareDumpFormatV2Impl(
    private val expectedDumpFile: File,
    private val actualDumpFile: File,
    private val abiTools: AbiToolsInterface,
) : BuildOperationImpl<String?>(), AbiValidationCompareDumpFormatV2 {

    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): String? {
        return abiTools.filesDiff(
            expectedDumpFile,
            actualDumpFile
        )
    }

}