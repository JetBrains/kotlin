/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.environment

sealed interface JvmClasspath {
    /**
     * The given roots. A root which this compilation has not indexed is ignored.
     */
    data class Roots(val roots: List<JvmClasspathRootId>) : JvmClasspath

    /**
     * Every classpath root of this compilation as the environment knows them, except those under [excludedRoots].
     */
    data class ProjectLibraries(val excludedRoots: List<JvmClasspathRootId> = emptyList()) : JvmClasspath

    companion object {
        val EMPTY: JvmClasspath = Roots(emptyList())
    }
}
