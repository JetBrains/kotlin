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
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

internal class DumpKlibAbiToStringOperationImpl private constructor(
    private val appendable: Appendable,
    override val klibs: Map<KlibTargetId, Path>,
    private val abiTools: AbiTools,
    override val options: Options,
) : BuildOperationImpl<Unit>(), DumpKlibAbiToStringOperation, DumpKlibAbiToStringOperation.Builder,
    DeepCopyable<DumpKlibAbiToStringOperation> {

    constructor(appendable: Appendable, klibs: Map<KlibTargetId, Path>, abiTools: AbiTools) : this(
        appendable,
        klibs,
        abiTools,
        Options(DumpKlibAbiToStringOperation::class)
    ) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?) {
        val filters = options[PATTERN_FILTERS]?.let { AbiValidationUtils.convert(it) } ?: org.jetbrains.kotlin.abi.tools.AbiFilters.EMPTY

        val mergedDump = abiTools.createKlibDump()
        klibs.forEach { (target, klibDir) ->
            val dump = abiTools.extractKlibAbi(
                klibDir.toFile(),
                AbiValidationUtils.convert(target),
                filters
            )
            mergedDump.merge(dump)
        }

        val targetsToInfer = options[TARGETS_TO_INFER]

        if (targetsToInfer.isNotEmpty()) {
            val referenceDumpFile = options[REFERENCE_DUMP_FILE]

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
    override fun <V> get(key: DumpKlibAbiToStringOperation.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: DumpKlibAbiToStringOperation.Option<V>, value: V) {
        options[key] = value
    }

    override fun filtersBuilder(): AbiFilters.Builder {
        return AbiFiltersImpl()
    }

    override fun deepCopy(): DumpKlibAbiToStringOperation {
        return DumpKlibAbiToStringOperationImpl(appendable, klibs.toMap(), abiTools, options.deepCopy())
    }

    override fun build(): DumpKlibAbiToStringOperation {
        return deepCopy()
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        /**
         * Filters with declarations of patterns containing `**`, `*` and `?` wildcards.
         */
        val PATTERN_FILTERS: Option<AbiFilters?> = Option("PATTERN_FILTERS", null)

        val REFERENCE_DUMP_FILE: Option<Path?> = Option("REFERENCE_DUMP_FILE", null)

        val TARGETS_TO_INFER: Option<Set<KlibTargetId>> = Option("TARGETS_TO_INFER", emptySet())
    }
}
