/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import java.io.File
import java.nio.file.Path

internal class JvmPlatformToolchainImpl(private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>) : JvmPlatformToolchain {
    @Deprecated(
        "Use newJvmCompilationOperation instead",
        replaceWith = ReplaceWith("newJvmCompilationOperation(sources, destinationDirectory)")
    )
    override fun createJvmCompilationOperation(
        sources: List<Path>,
        destinationDirectory: Path,
    ): JvmCompilationOperation =
        JvmCompilationOperationImpl(sources, destinationDirectory, buildIdToSessionFlagFile = buildIdToSessionFlagFile)

    override fun jvmCompilationOperationBuilder(
        sources: List<Path>,
        destinationDirectory: Path,
    ): JvmCompilationOperation.Builder =
        JvmCompilationOperationImpl(sources, destinationDirectory, buildIdToSessionFlagFile = buildIdToSessionFlagFile)

    override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
        return JvmClasspathSnapshottingOperationImpl(classpathEntry)
    }


    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsImpl()
    }
}