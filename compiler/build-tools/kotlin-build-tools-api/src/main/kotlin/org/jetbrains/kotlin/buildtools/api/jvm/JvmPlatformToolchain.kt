/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
 *   val operation = toolchain.jvm.jvmCompilationOperationBuilder(listOf(Path("/path/foo.kt")), Path("/path/to/outputDirectory")).build()
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
     * Creates a build operation for calculating classpath snapshots used for detecting changes in incremental compilation.
     *
     * Creating classpath snapshots is only required in multi-module projects.
     * Using classpath snapshots allows skipping unnecessary recompilation when ABI
     * changes in dependent modules have no effect on the current module.
     *
     * @param classpathEntry path to existing classpath entry
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     */
    @Deprecated("Use `classpathSnapshottingOperationBuilder` instead", ReplaceWith("classpathSnapshottingOperationBuilder(classpathEntry)"))
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

    /**
     * Creates a builder for an operation for discovering Kotlin script extensions.
     *
     * @param classpath classpath to search for custom script extensions
     * @see org.jetbrains.kotlin.buildtools.api.KotlinToolchains.BuildSession.executeOperation
     *
     * @since 2.4.0
     */
    public fun discoverScriptExtensionsOperationBuilder(classpath: List<Path>): DiscoverScriptExtensionsOperation.Builder

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

/**
 * Convenience function for creating a [JvmCompilationOperation] with options configured by [builderAction].
 *
 * @return an immutable `JvmCompilationOperation`.
 * @see JvmPlatformToolchain.jvmCompilationOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JvmPlatformToolchain.jvmCompilationOperation(
    sources: List<Path>,
    destinationDirectory: Path,
    builderAction: JvmCompilationOperation.Builder.() -> Unit,
): JvmCompilationOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return jvmCompilationOperationBuilder(sources, destinationDirectory).apply(builderAction).build()
}

/**
 * Convenience function for creating a [JvmClasspathSnapshottingOperation] with options configured by [builderAction].
 *
 * @return an immutable `JvmClasspathSnapshottingOperation`.
 * @see JvmPlatformToolchain.classpathSnapshottingOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JvmPlatformToolchain.classpathSnapshottingOperation(
    classpathEntry: Path,
    builderAction: JvmClasspathSnapshottingOperation.Builder.() -> Unit,
): JvmClasspathSnapshottingOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return classpathSnapshottingOperationBuilder(classpathEntry).apply(builderAction).build()
}

/**
 * Convenience function for creating a [DiscoverScriptExtensionsOperation] with options configured by [builderAction].
 *
 * @return an immutable `DiscoverScriptExtensionsOperation`.
 * @see JvmPlatformToolchain.discoverScriptExtensionsOperationBuilder
 *
 * @since 2.4.0
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JvmPlatformToolchain.discoverScriptExtensionsOperation(
    classpath: List<Path>,
    builderAction: DiscoverScriptExtensionsOperation.Builder.() -> Unit = {},
): DiscoverScriptExtensionsOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return discoverScriptExtensionsOperationBuilder(classpath).apply(builderAction).build()
}