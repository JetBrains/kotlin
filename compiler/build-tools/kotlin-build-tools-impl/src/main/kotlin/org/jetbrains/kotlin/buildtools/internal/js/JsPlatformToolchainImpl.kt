/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import java.io.File
import java.nio.file.Path

internal class JsPlatformToolchainImpl(private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>) : JsPlatformToolchain {
    override fun createJsCompilationOperation(
        sources: List<Path>,
        destinationDirectory: Path,
    ): JsCompilationOperation =
        JsCompilationOperationImpl(sources, destinationDirectory, buildIdToSessionFlagFile = buildIdToSessionFlagFile)

}