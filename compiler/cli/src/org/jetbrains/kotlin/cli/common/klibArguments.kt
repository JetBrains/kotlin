/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.customKlibAbiVersion
import org.jetbrains.kotlin.config.duplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.klibNormalizeAbsolutePath
import org.jetbrains.kotlin.config.klibRelativePathBases
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.produceKlibSignaturesClashChecks
import org.jetbrains.kotlin.library.KotlinAbiVersion
import kotlin.collections.plus

/**
 * Important: If you add or remove some argument from [setupCommonKlibArguments],
 * please remember to update it in [copyCommonKlibArgumentsFrom] correspondingly.
 */
fun CompilerConfiguration.setupCommonKlibArguments(
    arguments: CommonKlibBasedCompilerArguments,
    canBeMetadataKlibCompilation: Boolean
) {
    // Paths.
    arguments.relativePathBases?.let { klibRelativePathBases += it }
    klibNormalizeAbsolutePath = arguments.normalizeAbsolutePath

    // Diagnostics & checks.
    produceKlibSignaturesClashChecks = arguments.enableSignatureClashChecks
    renderDiagnosticInternalName = arguments.renderInternalDiagnosticNames

    duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.parseOrDefault(
        arguments.duplicatedUniqueNameStrategy,
        default = if (canBeMetadataKlibCompilation && arguments.metadataKlib)
            DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING
        else
            DuplicatedUniqueNameStrategy.DENY
    )

    // Set up the custom ABI version (the one that has no effect on the KLIB serialization, though will be written to manifest).
    customKlibAbiVersion = parseCustomKotlinAbiVersion(arguments.customKlibAbiVersion, messageCollector)
}

/**
 * Important: If you add or remove some argument from [copyCommonKlibArgumentsFrom],
 * please remember to update it in [setupCommonKlibArguments] correspondingly.
 */
fun CompilerConfiguration.copyCommonKlibArgumentsFrom(source: CompilerConfiguration) {
    // Paths.
    klibRelativePathBases = source.klibRelativePathBases
    klibNormalizeAbsolutePath = source.klibNormalizeAbsolutePath

    // Diagnostics & checks.
    produceKlibSignaturesClashChecks = source.produceKlibSignaturesClashChecks
    renderDiagnosticInternalName = source.renderDiagnosticInternalName
    source.duplicatedUniqueNameStrategy?.let { duplicatedUniqueNameStrategy = it }

    // Custom ABI version (the one that has no effect on the KLIB serialization, though will be written to manifest).
    customKlibAbiVersion = source.customKlibAbiVersion
}

private fun parseCustomKotlinAbiVersion(customKlibAbiVersion: String?, collector: MessageCollector): KotlinAbiVersion? {
    val versionParts = customKlibAbiVersion?.split('.') ?: return null
    if (versionParts.size != 3) {
        collector.report(
            CompilerMessageSeverity.ERROR,
            "Invalid ABI version format. Expected format: <major>.<minor>.<patch>"
        )
        return null
    }
    val version = versionParts.mapNotNull { it.toIntOrNull() }
    val validNumberRegex = Regex("(0|[1-9]\\d{0,2})")
    if (versionParts.any { !it.matches(validNumberRegex) } || version.any { it !in 0..255 }) {
        collector.report(
            CompilerMessageSeverity.ERROR,
            "Invalid ABI version numbers. Each part must be in the range 0..255."
        )
        return null
    }
    return KotlinAbiVersion(version[0], version[1], version[2])
}
