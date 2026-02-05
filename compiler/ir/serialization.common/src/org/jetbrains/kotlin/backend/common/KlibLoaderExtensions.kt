/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.duplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibLoaderResult
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.InvalidPathException
import java.nio.file.Paths

/**
 * Checks for existence of duplicated [uniqueName]s among [KlibLoaderResult.librariesStdlibFirst].
 * Processes the duplicates based on the [DuplicatedUniqueNameStrategy] which is stored in [CompilerConfiguration].
 *
 * TODO (KT-76785): In fact, handling of duplicated names is a workaround that needs to be removed in the future.
 */
fun KlibLoaderResult.eliminateLibrariesWithDuplicatedUniqueNames(configuration: CompilerConfiguration): KlibLoaderResult {
    if (librariesStdlibFirst.isEmpty()) return this

    // Note: Use LinkedHashMap to preserve the order of libraries.
    val librariesByUniqueName: Map<String, List<KotlinLibrary>> = librariesStdlibFirst.groupByTo(LinkedHashMap()) { it.uniqueName }

    val librariesWithDuplicatedUniqueNames: Map<String, List<KotlinLibrary>> = librariesByUniqueName.filterValues { it.size > 1 }
    if (librariesWithDuplicatedUniqueNames.isEmpty()) {
        return this
    }

    val messageCollector = configuration.messageCollector
    val duplicatedUniqueNameStrategy = configuration.duplicatedUniqueNameStrategy ?: DuplicatedUniqueNameStrategy.DENY

    for ((uniqueName, libraries) in librariesWithDuplicatedUniqueNames) {
        val message =
            "KLIB loader: The same 'unique_name=$uniqueName' found in more than one library: ${libraries.joinToString { it.libraryFile.path }}"

        if (duplicatedUniqueNameStrategy == DuplicatedUniqueNameStrategy.DENY) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                message +
                        "\nPlease file an issue to https://kotl.in/issue and meanwhile use CLI parameter -Xklib-duplicated-unique-name-strategy with one of the following values:\n" +
                        "${DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING}: Use all KLIB dependencies, even when they have same 'unique_name' property.\n" +
                        "${DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING}: Use the first KLIB dependency with clashing 'unique_name' property. No order guarantees are given though.\n" +
                        "${DuplicatedUniqueNameStrategy.DENY}: Fail a compilation with the error."
            )
        } else {
            messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, message)
        }
    }

    return if (duplicatedUniqueNameStrategy == DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING) {
        KlibLoaderResult(
            librariesStdlibFirst = librariesByUniqueName.map { it.value.first() },
            problematicLibraries = problematicLibraries
        )
    } else {
        this
    }
}

/**
 * Report any problems with loading KLIBs stored in [KlibLoaderResult] to compiler's [MessageCollector].
 */
fun KlibLoaderResult.reportLoadingProblemsIfAny(
    configuration: CompilerConfiguration,
    allAsErrors: Boolean = false
) {
    val messageCollector = configuration.messageCollector
    reportLoadingProblemsIfAny { defaultSeverity, message ->
        val compilerMessageSeverity = if (allAsErrors) CompilerMessageSeverity.ERROR else when (defaultSeverity) {
            KlibLoaderResult.ProblemSeverity.INFO -> CompilerMessageSeverity.INFO
            KlibLoaderResult.ProblemSeverity.WARNING -> CompilerMessageSeverity.STRONG_WARNING
            KlibLoaderResult.ProblemSeverity.ERROR -> CompilerMessageSeverity.ERROR
        }

        messageCollector.report(compilerMessageSeverity, message)
    }
}

/**
 * A helper to load the list of "friend" libraries.
 *
 * Note: It is assumed that paths of all "friend" libraries have already been passed to [KlibLoader],
 * so the loaded libraries should be in [KlibLoaderResult]. All we need is to "look up" them from the result.
 */
fun KlibLoaderResult.loadFriendLibraries(friendLibraryPaths: List<String>): List<KotlinLibrary> {
    if (friendLibraryPaths.isEmpty() || librariesStdlibFirst.isEmpty()) return emptyList()

    val canonicalFriendLibraryPaths: Set<String> = friendLibraryPaths.mapNotNullTo(linkedSetOf()) { rawPath ->
        if (rawPath.isEmpty()) return@mapNotNullTo null

        try {
            Paths.get(rawPath).toRealPath().toString()
        } catch (_: InvalidPathException) {
            return@mapNotNullTo null
        }
    }

    if (canonicalFriendLibraryPaths.isEmpty()) return emptyList()

    val canonicalLibraryPathsToLibraries: Map<String, KotlinLibrary> = librariesStdlibFirst.associateBy { it.libraryFile.canonicalPath }

    return canonicalFriendLibraryPaths.mapNotNull { canonicalLibraryPathsToLibraries[it] }
}
