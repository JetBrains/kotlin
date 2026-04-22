/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

internal class DumpJvmAbiToStringOperationImpl private constructor(
    private val appendable: Appendable,
    override val inputFiles: Iterable<Path>,
    private val abiTools: AbiTools,
    override val options: Options,
) : BuildOperationImpl<Unit>(), DumpJvmAbiToStringOperation, DumpJvmAbiToStringOperation.Builder,
    DeepCopyable<DumpJvmAbiToStringOperation> {

    constructor(appendable: Appendable, inputFiles: Iterable<Path>, abiTools: AbiTools) : this(
        appendable,
        inputFiles,
        abiTools,
        Options(DumpJvmAbiToStringOperation::class)
    ) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?) {
        val filters = options[PATTERN_FILTERS]?.let { AbiValidationUtils.convert(it) } ?: org.jetbrains.kotlin.abi.tools.AbiFilters.EMPTY
        abiTools.printJvmDump(appendable, inputFiles.map { it.toFile() }, filters)
    }


    @UseFromImplModuleRestricted
    override fun <V> get(key: DumpJvmAbiToStringOperation.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpJvmAbiToStringOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpJvmAbiToStringOperation {
        return DumpJvmAbiToStringOperationImpl(appendable, inputFiles, abiTools, options.deepCopy())
    }

    override fun build(): DumpJvmAbiToStringOperation {
        return deepCopy()
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        /**
         * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
         */
        val PATTERN_FILTERS: Option<AbiFilters?> = Option("PATTERN_FILTERS", null)
    }
}
