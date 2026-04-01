/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import java.security.MessageDigest

internal typealias RebuildReason = BuildAttribute

internal class HashedConfigurationInputs(
    val inputs: Map<RebuildReason, ByteArray>,
)

internal fun ConfigurationInputs.computeHashedConfigurationInputs(): HashedConfigurationInputs {
    return HashedConfigurationInputs(
        mapOf(
            RebuildReason.INCREMENTAL_COMPILATION_CONFIGURATION_CHANGED to computeConfigurationInputsHash(icConfigurationInputsSnapshot),
            RebuildReason.COMPILER_ARGS_CHANGED to computeConfigurationInputsHash(compilerArgumentsInputsSnapshot),
        )
    )
}

private fun computeConfigurationInputsHash(
    configurationInputsSnapshot: Map<String, String?>,
): ByteArray {
    val argStrings = configurationInputsSnapshot.toSortedMap()
    return MessageDigest.getInstance("SHA-256").apply {
        argStrings.forEach { (key, value) ->
            update(key.toByteArray())
            update((value ?: "").toByteArray())
        }
    }.digest()
}

private fun computeConfigurationInputsHash(
    configurationInputsSnapshot: List<String>,
): ByteArray {
    return MessageDigest.getInstance("SHA-256").apply {
        configurationInputsSnapshot.forEach { s ->
            update(s.toByteArray())
        }
    }.digest()
}