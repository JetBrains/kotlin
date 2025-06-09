/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm

import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.operations.JvmCompilationOperationImpl
import java.nio.file.Path

class JvmPlatformToolchainImpl : JvmPlatformToolchain {
    override fun createJvmCompilationOperation(
        kotlinSources: List<Path>,
        destinationDirectory: Path,
    ): JvmCompilationOperation = JvmCompilationOperationImpl(kotlinSources, destinationDirectory)

    override fun createClasspathSnapshottingOperation(classpathEntry: Path): JvmClasspathSnapshottingOperation {
        return JvmClasspathSnapshottingOperationImpl(classpathEntry)
    }
}