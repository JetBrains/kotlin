/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsLinkingOperationImpl
import java.io.File
import java.nio.file.Path

internal class JsPlatformToolchainImpl(private val compilerVersion: String, private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>) : JsPlatformToolchain {
    override fun jsLinkingOperationBuilder(klib: Path, destination: Path): JsLinkingOperation.Builder =
        JsLinkingOperationImpl(klib, destination, buildIdToSessionFlagFile = buildIdToSessionFlagFile)

    override fun jsKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): JsKlibCompilationOperation.Builder =
        JsKlibCompilationOperationImpl(sources, destination, buildIdToSessionFlagFile = buildIdToSessionFlagFile, compilerVersion = compilerVersion)
}
