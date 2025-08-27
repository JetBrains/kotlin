/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteJvmDumpFormatV2
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import java.io.File

internal class AbiValidationWriteJvmDumpFormatV2Impl(
    private val appendable: Appendable,
    private val inputFiles: Iterable<File>,
    private val abiTools: AbiToolsInterface,
) : BuildOperationImpl<Unit>(), AbiValidationWriteJvmDumpFormatV2 {

    private val options: Options = Options(AbiValidationWriteJvmDumpFormatV2::class)

    @UseFromImplModuleRestricted
    override fun <V> get(key: AbiValidationWriteJvmDumpFormatV2.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: AbiValidationWriteJvmDumpFormatV2.Option<V>, value: V) {
        options[key] = value
    }

    private operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    override fun execute(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ) {
        val filters: AbiFilters = options[PATTERN_FILTERS]
        abiTools.v2.printJvmDump(appendable, inputFiles, AbiValidationUtils.convert(filters))
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        /**
         * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
         */
        @JvmField
        public val PATTERN_FILTERS: Option<AbiFilters> = Option("PATTERN_FILTERS", AbiFilters.EMPTY)
    }

}