/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.IncompatibleAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.InvalidLibraryFormat
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.LibraryNotFound
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.OtherCheckMismatch
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.PlatformCheckMismatch
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblematicLibrary

/**
 * All libraries in [librariesStdlibFirst] are preserved in their original order (CLI-order),
 * except for stdlib, which, if it is present, must be at the first position.
 */
class KlibLoaderResult(
    val librariesStdlibFirst: List<KotlinLibrary>,
    val problematicLibraries: List<ProblematicLibrary>
) {
    val hasProblems: Boolean get() = problematicLibraries.isNotEmpty()

    class ProblematicLibrary(val libraryPath: String, val problemCase: ProblemCase)

    enum class ProblemSeverity {
        INFO, WARNING, ERROR
    }

    sealed interface ProblemCase {
        /**
         * The default severity for this problem case when it is reported to the user.
         */
        val defaultSeverity: ProblemSeverity

        /**
         * The library is not found by the given path:
         * - the path is invalid
         * - or no file system object exists by that path
         */
        object LibraryNotFound : ProblemCase {
            /**
             * The default severity is [ProblemSeverity.INFO], because this is not necessarily an error
             * if no library is found by the given path in compiler's production pipeline.
             * This behavior is similar to the javac CLI behavior.
             */
            override val defaultSeverity: ProblemSeverity get() = ProblemSeverity.INFO
        }

        /**
         * The path points to some real file system object, but:
         * - either this is not a KLIB library
         * - or a corrupted KLIB library
         */
        object InvalidLibraryFormat : ProblemCase {
            /**
             * The default severity is [ProblemSeverity.INFO], because this is not necessarily an error
             * if no library is found by the given path in compiler's production pipeline.
             * This behavior is similar to the javac CLI behavior.
             */
            override val defaultSeverity: ProblemSeverity get() = ProblemSeverity.INFO
        }


        /**
         * A valid library is found, but it can't be used due to some specific reason. User shall be notified about this
         * to avoid confusion.
         */
        sealed class ExistingKlibProblem : ProblemCase {
            /** Such problem cases are reported with [ProblemSeverity.WARNING] level. */
            final override val defaultSeverity: ProblemSeverity get() = ProblemSeverity.WARNING
        }

        /**
         * The library has not passed the platform and target check by [KlibPlatformChecker] that was set
         * in [KlibLoaderSpec.platformChecker].
         *
         * Note: [property], [expected] and [actual] have [String] type, and they contain some information
         * that can be used to compute a user-visible error message text. The exact meaning of these fields
         * is determined by a specific [KlibPlatformChecker] implementation.
         *
         * @property expected Some expectation set by [KlibPlatformChecker] that was not met.
         * @property actual The actual value discovered by [KlibPlatformChecker].
         */
        class PlatformCheckMismatch(
            val property: String,
            val expected: String,
            val actual: String,
        ) : ExistingKlibProblem()

        /**
         * The library does not match the ABI version requirements that were set in [KlibLoaderSpec.maxPermittedAbiVersion].
         *
         * @property libraryVersions The actual versions (including the ABI version) in a KLIB.
         * @property maxPermittedAbiVersion The max permitted ABI version set in [KlibLoader] via
         *  [KlibLoaderSpec.maxPermittedAbiVersion] call.
         * @property minPermittedAbiVersion The min permitted ABI version (reserved for the future).
         */
        class IncompatibleAbiVersion(
            val libraryVersions: KotlinLibraryVersioning,
            val minPermittedAbiVersion: KotlinAbiVersion?,
            val maxPermittedAbiVersion: KotlinAbiVersion?,
        ) : ExistingKlibProblem()

        /**
         * The library does not pass some other (custom) check that was not set in [KlibLoaderSpec].
         *
         * Note: This class is intentionally made abstract to allow having multiple implementations
         * for different purposes.
         *
         * @property caption The brief description of the problem that is going to be
         *  shown in the first line of the error message: "<caption>: <libraryPath>".
         * @property details The detailed description of the problem. May contain multiple lines.
         */
        abstract class OtherCheckMismatch : ExistingKlibProblem() {
            abstract val caption: String
            abstract val details: String
        }
    }
}

/**
 * Report any problems with loading KLIBs stored in [KlibLoaderResult] to the supplied [reporter] lambda.
 * Returns `true` if there were any problems reported.
 */
fun KlibLoaderResult.reportLoadingProblemsIfAny(
    reporter: (defaultSeverity: KlibLoaderResult.ProblemSeverity, message: String) -> Unit
): Boolean {
    if (problematicLibraries.isEmpty()) return false

    problematicLibraries.forEach { problematicLibrary ->
        reporter(
            problematicLibrary.problemCase.defaultSeverity,
            problematicLibrary.computeMessageText()
        )
    }

    return true
}

private fun ProblematicLibrary.computeMessageText(): String {
    val messageText = when (val problemCase = problemCase) {
        LibraryNotFound -> "Library not found: $libraryPath"
        InvalidLibraryFormat -> "Not a Kotlin library, or a library with invalid format: $libraryPath"

        is PlatformCheckMismatch -> with(problemCase) {
            "Library failed platform-specific check: $libraryPath\n" +
                    "Expected $property is $expected while found $actual."
        }

        is IncompatibleAbiVersion -> with(problemCase) {
            val libraryCompilerLine: String? = libraryVersions.compilerVersion?.let { "The library was produced by $it compiler." }

            val abiVersionCheckExplanation: String = when {
                minPermittedAbiVersion != null && maxPermittedAbiVersion != null ->
                    "ABI version in the range [$minPermittedAbiVersion, $maxPermittedAbiVersion]"

                maxPermittedAbiVersion != null ->
                    "ABI version <= $maxPermittedAbiVersion"

                else /*if (minPermittedAbiVersion != null)*/ ->
                    "ABI version >= $minPermittedAbiVersion"
            }

            val libraryAbiVersion: KotlinAbiVersion? = libraryVersions.abiVersion
            val lines: List<String> = if (libraryAbiVersion != null) {
                listOfNotNull(
                    "Incompatible ABI version $libraryAbiVersion in library: $libraryPath",
                    libraryCompilerLine,
                    "The current Kotlin compiler can consume libraries having $abiVersionCheckExplanation",
                    "Please upgrade your Kotlin compiler version to consume this library."
                )
            } else {
                // This message is not very actionable. However, we shouldn't have to worry about it because
                // there are no modern libraries without the ABI version in the wild.
                listOfNotNull(
                    "Library with unknown ABI version: $libraryPath",
                    libraryCompilerLine,
                    "The current Kotlin compiler can consume libraries having $abiVersionCheckExplanation, but it's not possible to determine the exact ABI version."
                )
            }

            lines.joinToString("\n")
        }

        is OtherCheckMismatch -> buildString {
            append(problemCase.caption).append(": ").appendLine(libraryPath)
            append(problemCase.details)
        }
    }

    return "KLIB loader: $messageText"
}
