/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
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
public interface JvmPlatformToolchain : KotlinToolchains.Toolchain {
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
    @Deprecated("Use jvmCompilationOperationBuilder instead", ReplaceWith("jvmCompilationOperationBuilder(sources, destinationDirectory)"))
    public fun createJvmCompilationOperation(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperation

    /**
     * Creates a builder for an operation for compiling Kotlin sources into class files.
     *
     * Note that [sources] should include .java files from the same module (as defined in https://kotl.in/spec-modules),
     * so that Kotlin compiler can properly resolve references to Java code and track changes in them.
     * However, Kotlin compiler will not compile the .java files.
     *
     * @param sources all sources of the compilation unit. This includes Java source files.
     * @param destinationDirectory where to put the output of the compilation
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    public fun jvmCompilationOperationBuilder(sources: List<Path>, destinationDirectory: Path): JvmCompilationOperation.Builder

    /**
     * Creates an options set for snapshot-based incremental compilation (IC) in JVM projects.
     * May be used to observe the defaults, adjust them, and configure incremental compilation as follows:
     * ```
     * val icOptions = kotlinToolchains.jvm.createSnapshotBasedIcOptions()
     *
     * icOptions[JvmIncrementalCompilationOptions.BACKUP_CLASSES] = true
     *
     * compilationOperation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = JvmIncrementalCompilationConfiguration(
     *     workingDirectory = Paths.get("build/kotlin"),
     *     sourcesChanges = SourcesChanges.ToBeCalculated,
     *     dependenciesSnapshotFiles = snapshots,
     *     shrunkClasspathSnapshot = shrunkSnapshot,
     *     options = icOptions,
     * )
     * ```
     * @see org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
     */
    public fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions

    /**
     * Creates a build operation for calculating classpath snapshots used for detecting changes in incremental compilation.
     *
     * Creating classpath snapshots is only required in multi-module projects.
     * Using classpath snapshots allows skipping unnecessary recompilation when ABI
     * changes in dependent modules have no effect on the current module.
     *
     * @param classpathEntry path to existing classpath entry
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    @Deprecated("Use classpathSnapshottingOperationBuilder instead", ReplaceWith("classpathSnapshottingOperationBuilder(classpathEntry)"))
    public fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation

    /**
     * Creates a build operation for calculating classpath snapshots used for detecting changes in incremental compilation.
     *
     * Creating classpath snapshots is only required in multi-module projects.
     * Using classpath snapshots allows skipping unnecessary recompilation when ABI
     * changes in dependent modules have no effect on the current module.
     *
     * @param classpathEntry path to existing classpath entry
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    public fun classpathSnapshottingOperationBuilder(classpathEntry: Path): JvmClasspathSnapshottingOperation.Builder

    public companion object {
        /**
         * Gets a [JvmPlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<JvmPlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.jvm: JvmPlatformToolchain get() = getToolchain<JvmPlatformToolchain>()
    }
}

