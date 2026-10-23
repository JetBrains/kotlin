/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathIterableAsListSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyByName
import java.nio.file.Path

@Serializable
internal class DumpJvmAbiToStringOperationImpl(
    @Transient
    override var appendable: Appendable = DoNothingAppendable,
    @Serializable(with = PathIterableAsListSerializer::class)
    override val inputFiles: Iterable<Path>,
    private val abiTools: AbiTools = AbiTools.getInstance(),
) : BaseAbiOperationImpl(), DumpJvmAbiToStringOperation, DumpJvmAbiToStringOperation.Builder,
    DeepCopyable<DumpJvmAbiToStringOperation> {

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext
    ) {
        val filters = patternFilters?.let { AbiValidationUtils.convert(it) } ?: org.jetbrains.kotlin.abi.tools.AbiFilters.EMPTY
        abiTools.printJvmDump(appendable, inputFiles.map { it.toFile() }, filters)
    }


    @UseFromImplModuleRestricted
    override fun <V> get(key: DumpJvmAbiToStringOperation.Option<V>): V = getPropertyByName(propertyAccessMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpJvmAbiToStringOperation.Option<V>, value: V) {
        setPropertyByName(propertyAccessMap, key.id, value)
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpJvmAbiToStringOperationImpl {
        return DumpJvmAbiToStringOperationImpl(appendable, inputFiles, abiTools).also { it.copyFrom(this) }
    }

    private fun copyFrom(other: DumpJvmAbiToStringOperationImpl) {
        super.copyFrom(other)
        patternFilters = other.patternFilters
    }

    override fun build(): DumpJvmAbiToStringOperation {
        return deepCopy()
    }

    /**
     * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
     */
    @SerialName("PATTERN_FILTERS")
    var patternFilters: AbiFilters? = null

    companion object {
        private val propertyAccessMap = mapOf(
            "PATTERN_FILTERS" to DumpJvmAbiToStringOperationImpl::patternFilters
        )
    }
}
