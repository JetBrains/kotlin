/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi

import org.jetbrains.kotlin.abi.tools.AbiToolsFactory
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationCompareDumpFormatV2
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteJvmDumpFormatV2
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteKlibDumpFormatV2
import org.jetbrains.kotlin.buildtools.internal.abi.operations.AbiValidationCompareDumpFormatV2Impl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.AbiValidationWriteJvmDumpFormatV2Impl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.AbiValidationWriteKlibDumpFormatV2Impl
import java.io.File

internal class AbiValidationToolchainImpl : AbiValidationToolchain {
    private val abiTools = AbiToolsFactory().get()

    override fun writeJvmDumpFormatV2(
        appendable: Appendable,
        inputFiles: Iterable<File>,
    ): AbiValidationWriteJvmDumpFormatV2 {
        return AbiValidationWriteJvmDumpFormatV2Impl(appendable, inputFiles, abiTools)
    }

    override fun writeKlibDumpFormatV2(
        appendable: Appendable,
        referenceDumpFile: File,
        klibs: Map<KlibTargetId, File>,
        unsupported: Set<KlibTargetId>,
    ): AbiValidationWriteKlibDumpFormatV2 {
        return AbiValidationWriteKlibDumpFormatV2Impl(appendable, referenceDumpFile, klibs, unsupported, abiTools)
    }

    override fun findDiffFormatV2(
        expectedDumpFile: File,
        actualDumpFile: File,
    ): AbiValidationCompareDumpFormatV2 {
        return AbiValidationCompareDumpFormatV2Impl(expectedDumpFile, actualDumpFile, abiTools)
    }
}