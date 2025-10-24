/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsCompilationOperation
import java.nio.file.Path

/**
 * Allows creating operations that can be used for performing Kotlin/JVM compilations.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.jvm].
 *
 * An example of the basic usage is:
 *  ```
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.jvm.createJvmCompilationOperation(listOf(Path("/path/foo.kt")), Path("/path/to/outputDirectory"))
 *   toolchain.createBuildSession().use { it.executeOperation(operation) }
 *  ```
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface JsPlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * Creates a build operation for compiling Kotlin sources into class files.
     *
     * Note that [sources] should include .java files from the same module (as defined in https://kotl.in/spec-modules),
     * so that Kotlin compiler can properly resolve references to Java code and track changes in them.
     * However, Kotlin compiler will not compile the .java files.
     *
     * @param sources all sources of the compilation unit. This includes Java source files.
     * @param destinationDirectory where to put the output of the compilation
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    public fun createJsCompilationOperation(sources: List<Path>, destinationDirectory: Path): JsCompilationOperation

    public companion object {
        /**
         * Gets a [JsPlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<JsPlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.js: JsPlatformToolchain get() = getToolchain<JsPlatformToolchain>()
    }
}

