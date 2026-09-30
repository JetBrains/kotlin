/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.SerialName
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
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

internal class DumpKlibAbiToStringOperationImpl(
    private val appendable: Appendable,
    override val klibs: Map<KlibTargetId, Path>,
    private val abiTools: AbiTools,
) : BuildOperationImpl<Unit>(), DumpKlibAbiToStringOperation, DumpKlibAbiToStringOperation.Builder,
    DeepCopyable<DumpKlibAbiToStringOperation> {

    @SerialName("PATTERN_FILTERS")
    private var patternFilters: AbiFilters? = null

    @SerialName("REFERENCE_DUMP_FILE")
    private var referenceDumpFile: Path? = null

    @SerialName("TARGETS_TO_INFER")
    private var targetsToInfer: Set<KlibTargetId> = emptySet()

    override val usesApplicationEnvironment: Boolean
        get() = false

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext
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
    override fun <V> get(key: DumpKlibAbiToStringOperation.Option<V>): V =
        DumpKlibAbiToStringOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpKlibAbiToStringOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        DumpKlibAbiToStringOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    operator fun <V> get(key: Option<V>): V =
        DumpKlibAbiToStringOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        DumpKlibAbiToStringOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpKlibAbiToStringOperation {
        return DumpKlibAbiToStringOperationImpl(appendable, klibs.toMap(), abiTools)
    }

    override fun build(): DumpKlibAbiToStringOperation {
        return deepCopy()
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        /**
         * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
         */
        val PATTERN_FILTERS: Option<AbiFilters?> = Option("PATTERN_FILTERS")

        val REFERENCE_DUMP_FILE: Option<Path?> = Option("REFERENCE_DUMP_FILE")

        val TARGETS_TO_INFER: Option<Set<KlibTargetId>> = Option("TARGETS_TO_INFER")
    }
}
