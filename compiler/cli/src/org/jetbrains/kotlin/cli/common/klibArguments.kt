/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.library.KotlinAbiVersion
import java.util.EnumMap
import kotlin.collections.plus
import kotlin.reflect.KProperty1

/**
 * Important: If you add or remove some argument from [setupCommonKlibArguments],
 * please remember to update it in [copyCommonKlibArgumentsFrom] correspondingly.
 */
fun CompilerConfiguration.setupCommonKlibArguments(
    arguments: CommonKlibBasedCompilerArguments,
    canBeMetadataKlibCompilation: Boolean,
    rootDisposable: Disposable,
) {
    val isKlibMetadataCompilation = canBeMetadataKlibCompilation && arguments.metadataKlib

    // Paths.
    arguments.relativePathBases?.let { klibRelativePathBases += it }
    klibNormalizeAbsolutePath = arguments.normalizeAbsolutePath

    // Diagnostics & checks.
    produceKlibSignaturesClashChecks = arguments.enableSignatureClashChecks
    renderDiagnosticInternalName = arguments.renderInternalDiagnosticNames
    skipLibrarySpecialCompatibilityChecks = arguments.skipLibrarySpecialCompatibilityChecks

    duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.parseOrDefault(
        arguments.duplicatedUniqueNameStrategy,
        default = if (isKlibMetadataCompilation) DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING else DuplicatedUniqueNameStrategy.DENY
    )

    // Set up the custom ABI version (the one that has no effect on the KLIB serialization, though will be written to manifest).
    customKlibAbiVersion = parseCustomKotlinAbiVersion(arguments.customKlibAbiVersion, messageCollector)

    // Set up the ABI compatibility level (the one that actually affects the KLIB serialization).
    if (!isKlibMetadataCompilation) {
        setupKlibAbiCompatibilityLevel()
    }

    zipFileSystemAccessor = arguments.getZipFileSystemAccessor(
        zipFileAccessorCacheLimitArgument = CommonKlibBasedCompilerArguments::klibZipFileAccessorCacheLimit,
        collector = messageCollector,
        rootDisposable = rootDisposable
    )
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
    skipLibrarySpecialCompatibilityChecks = source.skipLibrarySpecialCompatibilityChecks
    source.duplicatedUniqueNameStrategy?.let { duplicatedUniqueNameStrategy = it }

    // Custom ABI version (the one that has no effect on the KLIB serialization, though will be written to manifest).
    customKlibAbiVersion = source.customKlibAbiVersion

    // ABI compatibility level (the one that actually affects the KLIB serialization).
    klibAbiCompatibilityLevel = source.klibAbiCompatibilityLevel

    zipFileSystemAccessor = source.zipFileSystemAccessor
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

fun <A : CommonCompilerArguments> A.getZipFileSystemAccessor(
    zipFileAccessorCacheLimitArgument: KProperty1<A, String>,
    collector: MessageCollector,
    rootDisposable: Disposable,
): ZipFileSystemAccessor? {
    val cacheLimitRawValue: String = zipFileAccessorCacheLimitArgument.get(this)
    val cacheLimit: Int? = cacheLimitRawValue.toIntOrNull()

    if (cacheLimit == null || cacheLimit < 0) {
        collector.report(
            CompilerMessageSeverity.ERROR,
            buildString {
                append("Cannot parse ${zipFileAccessorCacheLimitArgument.cliArgument} value: \"$cacheLimitRawValue\". ")
                append("It must be an integer >= 0.")
            }
        )
        return null
    }
    return if (cacheLimit > 0) {
        DisposableZipFileSystemAccessor(cacheLimit).also { Disposer.register(rootDisposable, it) }
    } else {
        ZipFileSystemInPlaceAccessor
    }
}

private class DisposableZipFileSystemAccessor(
    private val zipAccessor: ZipFileSystemCacheableAccessor
) : Disposable, ZipFileSystemAccessor by zipAccessor {
    constructor(cacheLimit: Int) : this(ZipFileSystemCacheableAccessor(cacheLimit))

    override fun dispose() {
        zipAccessor.reset()
    }
}

fun CompilerConfiguration.setupKlibAbiCompatibilityLevel() {
    val languageVersionSettings = this[LANGUAGE_VERSION_SETTINGS]
        ?: error("Language version settings should be already set up")

    klibAbiCompatibilityLevel = if (languageVersionSettings.supportsFeature(LanguageFeature.ExportKlibToOlderAbiVersion)) {
        val languageVersion = languageVersionSettings.languageVersion

        val abiCompatibilityLevel = LANGUAGE_VERSION_TO_ABI_COMPATIBILITY_LEVEL[languageVersion]
        if (abiCompatibilityLevel == null) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                buildString {
                    append("Exporting KLIBs in older ABI format is only supported for the following language versions: ")
                    // Show all LVs that are less than the current LV. Because otherwise it could lead to confusion.
                    LANGUAGE_VERSION_TO_ABI_COMPATIBILITY_LEVEL.keys.takeWhile { it < LanguageVersion.LATEST_STABLE }.joinTo(this)
                    append(". The current language version is ")
                    append(languageVersion)
                }
            )
            return
        }

        abiCompatibilityLevel
    } else
        KlibAbiCompatibilityLevel.LATEST_STABLE
}

private val LANGUAGE_VERSION_TO_ABI_COMPATIBILITY_LEVEL =
    EnumMap<LanguageVersion, KlibAbiCompatibilityLevel>(LanguageVersion::class.java).apply {
        KlibAbiCompatibilityLevel.entries.associateByTo(this) { abiCompatibilityLevel ->
            when (abiCompatibilityLevel) {
                KlibAbiCompatibilityLevel.ABI_LEVEL_2_3 -> LanguageVersion.KOTLIN_2_3
                KlibAbiCompatibilityLevel.ABI_LEVEL_2_4 -> LanguageVersion.KOTLIN_2_4
                // add new entries here as necessary
            }
        }

        check(size == KlibAbiCompatibilityLevel.entries.size)

        LanguageVersion.entries.forEach { languageVersion ->
            when {
                languageVersion < LanguageVersion.KOTLIN_2_1 -> Unit // Old unsupported language version. Skip it.
                languageVersion in this -> Unit // It's already added to the mapping.
                else -> {
                    // A new language version, for which we don't have the matching ABI compatibility level yet.
                    // So, use the latest stable ABI compatibility level.
                    this[languageVersion] = KlibAbiCompatibilityLevel.LATEST_STABLE
                }
            }
        }
    }

fun CompilerConfiguration.checkForUnexpectedKlibLibraries(
    librariesToCheck: List<String>,
    librariesToCheckArgument: String,
    allLibraries: List<String>,
    allLibrariesArgument: String,
) {
    if (librariesToCheck.isEmpty()) return

    val unexpectedLibraries = librariesToCheck subtract allLibraries.toSet()
    if (unexpectedLibraries.isNotEmpty()) {
        messageCollector.report(
            WARNING,
            "There are libraries in $librariesToCheckArgument CLI argument " +
                    "that are not included in $allLibrariesArgument CLI argument: " +
                    unexpectedLibraries.joinToString()
        )
    }
}
