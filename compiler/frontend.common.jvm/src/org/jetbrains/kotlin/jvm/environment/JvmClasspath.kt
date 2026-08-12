/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.environment

import java.nio.file.Path

/**
 * A part of the JVM classpath of one compilation, named by its roots. This is what a lookup over binaries is
 * restricted to: [JvmCompilationEnvironment.getKotlinClassFinder], [JvmCompilationEnvironment.getPackagePartProvider]
 * and the binary Java view all take one.
 *
 * The set of shapes is closed on purpose. A compilation never needs arbitrary set algebra over files: every
 * value it uses is either an explicit list of roots ([Roots]) or its whole classpath, possibly without a few
 * roots ([ProjectLibraries]). There is no complement and no ambient universe of "all files", both of which only
 * mean something inside an IDE project model.
 */
interface JvmClasspath {
    /**
     * The given roots and nothing else. A root is a directory or a `.jar`/`.jmod` file, exactly as it is spelled
     * on the compiler's classpath; a path which is neither is ignored.
     */
    data class Roots(val roots: List<Path>) : JvmClasspath

    /**
     * Every classpath root of this compilation as the environment knows them, except those under [excludedRoots].
     *
     * The exclusion serves incremental compilation, which reads the output directory of the previous build as a
     * separate classpath ([Roots]) and must not see it a second time as a part of the regular one.
     */
    data class ProjectLibraries(val excludedRoots: List<Path> = emptyList()) : JvmClasspath

    companion object {
        val EMPTY: JvmClasspath = Roots(emptyList())
    }
}
