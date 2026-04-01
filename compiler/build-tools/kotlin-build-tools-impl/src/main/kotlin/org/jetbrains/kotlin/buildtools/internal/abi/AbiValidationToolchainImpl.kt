/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi

import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.CompareAbiTextFilesOperation
import org.jetbrains.kotlin.buildtools.internal.abi.operations.DumpJvmAbiToStringOperationImpl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.DumpKlibAbiToStringOperationImpl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.CompareAbiTextFilesOperationImpl
import java.nio.file.Path

internal class AbiValidationToolchainImpl : AbiValidationToolchain {
    private val abiTools = AbiTools.getInstance()

    override fun dumpJvmAbiToStringOperationBuilder(
        appendable: Appendable,
        inputFiles: Iterable<Path>,
    ): DumpJvmAbiToStringOperation.Builder {
        return DumpJvmAbiToStringOperationImpl(appendable, inputFiles, abiTools)
    }

    override fun dumpKlibAbiToStringOperationBuilder(
        appendable: Appendable,
        klibs: Map<KlibTargetId, Path>,
    ): DumpKlibAbiToStringOperation.Builder {
        return DumpKlibAbiToStringOperationImpl(appendable, klibs, abiTools)
    }

    override fun compareAbiTextFilesOperationBuilder(
        diff: Appendable,
        expectedDumpFile: Path,
        actualDumpFile: Path,
    ): CompareAbiTextFilesOperation.Builder {
        return CompareAbiTextFilesOperationImpl(diff, expectedDumpFile, actualDumpFile, abiTools)
    }
}