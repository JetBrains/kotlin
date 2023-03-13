/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.api

import java.util.*

interface IncrementalCompilerFacade {
    fun compileWithDaemon(
//        buildId: UUID,
        launcher: KotlinCompilerLauncher,
        configuration: CompilerConfiguration,
        arguments: List<String>,
        options: KotlinCompilationOptions
    )

    fun compileInProcess(
//        buildId: UUID,
        arguments: List<String>,
        options: KotlinCompilationOptions
    )
}