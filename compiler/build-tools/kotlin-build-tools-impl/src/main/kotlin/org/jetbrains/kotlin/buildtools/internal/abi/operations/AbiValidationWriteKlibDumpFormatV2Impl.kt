/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteJvmDumpFormatV2
import org.jetbrains.kotlin.buildtools.api.abi.operations.AbiValidationWriteKlibDumpFormatV2
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.abi.AbiValidationUtils
import org.jetbrains.kotlin.buildtools.internal.abi.operations.AbiValidationWriteJvmDumpFormatV2Impl.Companion.PATTERN_FILTERS
import java.io.File

internal class AbiValidationWriteKlibDumpFormatV2Impl(
    private val appendable: Appendable,
    private val referenceDumpFile: File,
    private val klibs: Map<KlibTargetId, File>,
    private val unsupported: Set<KlibTargetId>,
    private val abiTools: AbiToolsInterface,
) : BuildOperationImpl<Unit>(), AbiValidationWriteKlibDumpFormatV2 {

    private val options: Options = Options(AbiValidationWriteJvmDumpFormatV2::class)

    @UseFromImplModuleRestricted
    override fun <V> get(key: AbiValidationWriteKlibDumpFormatV2.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: AbiValidationWriteKlibDumpFormatV2.Option<V>, value: V) {
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

        val mergedDump = abiTools.v2.createKlibDump()
        klibs.forEach { (target, klibDir) ->
            val dump = abiTools.v2.extractKlibAbi(
                klibDir,
                KlibTarget(target.targetName, target.configurableName),
                AbiValidationUtils.convert(filters)
            )
            mergedDump.merge(dump)
        }
        if (unsupported.isNotEmpty()) {
            val referenceDump = if (referenceDumpFile.exists() && referenceDumpFile.isFile) {
                abiTools.v2.loadKlibDump(referenceDumpFile)
            } else {
                abiTools.v2.createKlibDump()
            }
            unsupported.forEach { unsupportedTarget ->
                val inferredDump = mergedDump.inferAbiForUnsupportedTarget(referenceDump, AbiValidationUtils.convert(unsupportedTarget))
                mergedDump.merge(inferredDump)
            }
        }
        mergedDump.print(appendable)
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

}