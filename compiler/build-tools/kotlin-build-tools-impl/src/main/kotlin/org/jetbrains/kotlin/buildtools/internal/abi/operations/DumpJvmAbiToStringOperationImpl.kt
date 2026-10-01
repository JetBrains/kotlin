/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpJvmAbiToStringOperation
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.ExecutionContext
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import java.nio.file.Path

@Serializable
internal class DumpJvmAbiToStringOperationImpl(
    private val appendable: Appendable,
    override val inputFiles: Iterable<Path>,
    private val abiTools: AbiTools,
    @SerialName("PATTERN_FILTERS")
    internal var patternFilters: AbiFilters? = null,
) : BuildOperationImpl<Unit>(), DumpJvmAbiToStringOperation, DumpJvmAbiToStringOperation.Builder,
    DeepCopyable<DumpJvmAbiToStringOperation> {

    override val usesApplicationEnvironment: Boolean
        get() = false

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ) {
        val filters = patternFilters?.let { AbiValidationUtils.convert(it) } ?: org.jetbrains.kotlin.abi.tools.AbiFilters.EMPTY
        abiTools.printJvmDump(appendable, inputFiles.map { it.toFile() }, filters)
    }


    @UseFromImplModuleRestricted
    override fun <V> get(key: DumpJvmAbiToStringOperation.Option<V>): V =
        DumpJvmAbiToStringOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpJvmAbiToStringOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        DumpJvmAbiToStringOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    operator fun <V> get(key: Option<V>): V =
        DumpJvmAbiToStringOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        DumpJvmAbiToStringOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpJvmAbiToStringOperation {
        return DumpJvmAbiToStringOperationImpl(appendable, inputFiles, abiTools, patternFilters).also {
            it.copyFrom(this)
        }
    }

    override fun build(): DumpJvmAbiToStringOperation {
        return deepCopy()
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        /**
         * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
         */
        val PATTERN_FILTERS: Option<AbiFilters?> = Option("PATTERN_FILTERS")
    }
}
