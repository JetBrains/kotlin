/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.js.operations

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.internal.BaseCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonJsAndWasmArgumentsImpl
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import java.io.File
import java.nio.file.Path

internal abstract class CommonJsAndWasmKlibCompilationOperationImpl<BtaCompilerArgs : CommonJsAndWasmArgumentsImpl, CompilerArgs : CommonJsAndWasmCompilerArguments>(
    compilerArguments: BtaCompilerArgs,
    buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    val sources: List<Path>,
    val destination: Path,
) :
    BaseCompilationOperationImpl<BtaCompilerArgs, CompilerArgs>(compilerArguments, buildIdToSessionFlagFile) {
}
