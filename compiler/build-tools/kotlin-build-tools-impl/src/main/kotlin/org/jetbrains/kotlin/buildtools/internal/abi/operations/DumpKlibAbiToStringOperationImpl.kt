/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class, KlibTargetIdSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.ExecutionContext
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import org.jetbrains.kotlin.buildtools.internal.serializability.KlibTargetIdSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyByName
import java.nio.file.Path

@Serializable
internal class DumpKlibAbiToStringOperationImpl(
    override var appendable: Appendable = DoNothingAppendable,
    override val klibs: Map<KlibTargetId, Path>,
    private val abiTools: AbiTools = AbiTools.getInstance(),
) : BaseAbiOperationImpl(), DumpKlibAbiToStringOperation, DumpKlibAbiToStringOperation.Builder,
    DeepCopyable<DumpKlibAbiToStringOperation> {

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ) {
        val filters = patternFilters?.let { AbiValidationUtils.convert(it) } ?: org.jetbrains.kotlin.abi.tools.AbiFilters.EMPTY

        val mergedDump = abiTools.createKlibDump()
        klibs.forEach { [target, klibDir] ->
            val dump = abiTools.extractKlibAbi(
                klibDir.toFile(),
                AbiValidationUtils.convert(target),
                filters
            )
            mergedDump.merge(dump)
        }

        if (targetsToInfer.isNotEmpty()) {
            val reference = referenceDumpFile?.toFile()
            val referenceDump = if (reference != null && reference.exists() && reference.isFile) {
                abiTools.loadKlibDump(reference)
            } else {
                abiTools.createKlibDump()
            }
            targetsToInfer.forEach { unsupportedTarget ->
                val inferredDump = mergedDump.inferAbiForUnsupportedTarget(referenceDump, AbiValidationUtils.convert(unsupportedTarget))
                mergedDump.merge(inferredDump)
            }
        }
        mergedDump.print(appendable)
    }


    @UseFromImplModuleRestricted
    override fun <V> get(key: DumpKlibAbiToStringOperation.Option<V>): V = getPropertyByName(propertyAccessMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpKlibAbiToStringOperation.Option<V>, value: V) {
        setPropertyByName(propertyAccessMap, key.id, value)
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpKlibAbiToStringOperationImpl {
        return DumpKlibAbiToStringOperationImpl(appendable, klibs.toMap(), abiTools).also { it.copyFrom(this) }
    }

    private fun copyFrom(other: DumpKlibAbiToStringOperationImpl) {
        super.copyFrom(other)
        patternFilters = other.patternFilters
        referenceDumpFile = other.referenceDumpFile
        targetsToInfer = other.targetsToInfer
    }

    override fun build(): DumpKlibAbiToStringOperation {
        return deepCopy()
    }

    /**
     * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
     */
    @SerialName("PATTERN_FILTERS")
    var patternFilters: AbiFilters? = null

    @SerialName("REFERENCE_DUMP_FILE")
    var referenceDumpFile: Path? = null

    @SerialName("TARGETS_TO_INFER")
    var targetsToInfer: Set<KlibTargetId> = emptySet()

    companion object {
        private val propertyAccessMap = mapOf(
            "PATTERN_FILTERS" to DumpKlibAbiToStringOperationImpl::patternFilters,
            "REFERENCE_DUMP_FILE" to DumpKlibAbiToStringOperationImpl::referenceDumpFile,
            "TARGETS_TO_INFER" to DumpKlibAbiToStringOperationImpl::targetsToInfer
        )
    }
}
