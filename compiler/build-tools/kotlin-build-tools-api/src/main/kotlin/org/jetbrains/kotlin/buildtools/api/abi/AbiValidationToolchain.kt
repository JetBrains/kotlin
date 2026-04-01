/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.abi

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.abi.operations.CompareAbiTextFilesOperation
import org.jetbrains.kotlin.buildtools.api.getToolchain
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Allows creating tasks for ABI (Application Binary Interface) validation.
 *
 * ABI validation is a part of the Kotlin toolset designed to control which declarations are available to other modules.
 * You can use this tool to extract and compare ABI from your library or other already compiled module.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface AbiValidationToolchain : KotlinToolchains.Toolchain {
    /**
     * Prints an ABI dump for JVM from [inputFiles] into the specified [appendable].
     *
     * @param inputFiles Paths to the files used to extract its ABI. The files can be class-files or jar-files. Directories are not supported.
     *
     * @since 2.4.0
     */
    public fun dumpJvmAbiToStringOperationBuilder(
        appendable: Appendable,
        inputFiles: Iterable<Path>,
    ): DumpJvmAbiToStringOperation.Builder

    /**
     * Prints an ABI dump for klib targets from [klibs] into the specified [appendable].
     * Compressed and unpacked klibs are supported.
     *
     * @since 2.4.0
     */
    public fun dumpKlibAbiToStringOperationBuilder(
        appendable: Appendable,
        klibs: Map<KlibTargetId, Path>,
    ): DumpKlibAbiToStringOperation.Builder

    /**
     * Compares two files line-by-line.
     *
     * If files are equal, nothing is written to [diff].
     *
     * @since 2.4.0
     */
    public fun compareAbiTextFilesOperationBuilder(
        diff: Appendable,
        expectedDumpFile: Path,
        actualDumpFile: Path,
    ): CompareAbiTextFilesOperation.Builder


    public companion object {
        /**
         * Gets a [AbiValidationToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<AbiValidationToolchain>()`
         *
         * @since 2.4.0
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.abiValidation: AbiValidationToolchain get() = getToolchain<AbiValidationToolchain>()
    }
}

/**
 * Prints an ABI dump for JVM from [inputFiles] into the specified [appendable] with options configured by [builderAction].
 * It is possible to pass class-files or jar files in [inputFiles].
 *
 * To control which declarations are passed to the dump, the option [DumpJvmAbiToStringOperation.PATTERN_FILTERS] could be used. By default, no filters will be applied.
 *
 * @since 2.4.0
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun AbiValidationToolchain.dumpJvmAbiToStringOperation(
    appendable: Appendable,
    inputFiles: Iterable<Path>,
    builderAction: DumpJvmAbiToStringOperation.Builder.() -> Unit = {},
): DumpJvmAbiToStringOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return dumpJvmAbiToStringOperationBuilder(appendable, inputFiles).apply(builderAction).build()
}

/**
 * Prints an ABI dump for klib targets from [klibs] into the specified [appendable]  with options configured by [builderAction].
 * Compressed and unpacked klibs are supported.
 *
 * If option [DumpKlibAbiToStringOperation.TARGETS_TO_INFER] is specified and not empty, for the specified targets the ABI will be inferred from the option [DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE].
 * The inference works as follows:
 * - for each target from [DumpKlibAbiToStringOperation.TARGETS_TO_INFER], the ABI is inferred from the [DumpKlibAbiToStringOperation.REFERENCE_DUMP_FILE], if it exists, not empty, and this target is present in it
 * - all the non-inferred targets that belong to the group that this target belongs to are found. Then all declarations are added that are present in all of them.
 *
 * The inference is used in cases where the host compiler cannot compile some targets, but there is a need to build an ABI dump,
 * even if with some inaccuracies.
 *
 * To control which declarations are passed to the dump, the option [DumpKlibAbiToStringOperation.PATTERN_FILTERS] could be used. By default, no filters will be applied.
 *
 * @since 2.4.0
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public fun AbiValidationToolchain.dumpKlibAbiToStringOperation(
    appendable: Appendable,
    klibs: Map<KlibTargetId, Path>,
    builderAction: DumpKlibAbiToStringOperation.Builder.() -> Unit = {},
): DumpKlibAbiToStringOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return dumpKlibAbiToStringOperationBuilder(appendable, klibs).apply(builderAction).build()
}

/**
 * Compares two files line-by-line.
 *
 * If files are equal, nothing is written to [diff].
 *
 * @since 2.4.0
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public fun AbiValidationToolchain.compareAbiTextFilesOperation(
    diff: Appendable,
    expectedDumpFile: Path,
    actualDumpFile: Path,
    builderAction: CompareAbiTextFilesOperation.Builder.() -> Unit = {},
): CompareAbiTextFilesOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return compareAbiTextFilesOperationBuilder(diff, expectedDumpFile, actualDumpFile).apply(builderAction).build()
}
