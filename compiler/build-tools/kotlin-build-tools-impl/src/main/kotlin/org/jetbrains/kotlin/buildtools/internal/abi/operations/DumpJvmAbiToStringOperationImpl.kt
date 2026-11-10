/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.internal.ExecutionContext
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import org.jetbrains.kotlin.buildtools.internal.daemon.PathIterableAsListSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.cast
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound
import org.jetbrains.kotlin.daemon.common.serialization.PathAsStringSerializer
import java.nio.file.Path

@Serializable
internal class DumpJvmAbiToStringOperationImpl : BaseAbiOperationImpl, DumpJvmAbiToStringOperation, DumpJvmAbiToStringOperation.Builder,
    DeepCopyable<DumpJvmAbiToStringOperation> {

    constructor(
        appendable: Appendable = PlaceholderAppendable,
        inputFiles: Iterable<Path>,
        abiTools: AbiTools = AbiTools.getInstance()
    ) : super(appendable, abiTools) {
        this.inputFiles = inputFiles
        this.appendable = appendable
    }

    private constructor(other: DumpJvmAbiToStringOperationImpl) : super(other) {
        inputFiles = other.inputFiles
        patternFilters = other.patternFilters?.deepCopy()
    }

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext
    ) {
        val filters = patternFilters?.let { AbiValidationUtils.convert(it) } ?: org.jetbrains.kotlin.abi.tools.AbiFilters.EMPTY
        abiTools.printJvmDump(appendable, inputFiles.map { it.toFile() }, filters)
    }

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: DumpJvmAbiToStringOperation.Option<V>): V = when (key.id) {
        "PATTERN_FILTERS" -> patternFilters
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpJvmAbiToStringOperation.Option<V>, value: V) {
        when (key.id) {
            "PATTERN_FILTERS" -> patternFilters = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpJvmAbiToStringOperationImpl = DumpJvmAbiToStringOperationImpl(this)

    override fun build(): DumpJvmAbiToStringOperation {
        return deepCopy()
    }


    @Serializable(with = PathIterableAsListSerializer::class)
    override val inputFiles: Iterable<Path>

    /**
     * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
     */
    var patternFilters: AbiFiltersImpl? = null
}
