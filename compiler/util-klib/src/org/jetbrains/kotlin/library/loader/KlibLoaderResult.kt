/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning

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

    sealed interface ProblemCase {
        /**
         * The library is not found by the given path:
         * - the path is invalid
         * - or no file system object exists by that path
         */
        object LibraryNotFound : ProblemCase

        /**
         * The path points to some real file system object, but:
         * - either this is not a KLIB library
         * - or a corrupted KLIB library
         */
        object InvalidLibraryFormat : ProblemCase

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
        ) : ProblemCase

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
        ) : ProblemCase {
            init {
                check(minPermittedAbiVersion != null || maxPermittedAbiVersion != null)
            }
        }
    }
}
