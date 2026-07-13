/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.compilation.KaCodeCompilationException
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptions
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationOptionsBuilder
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationResult
import org.jetbrains.kotlin.psi.KtFile

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsCompilerFacility {
    @KaExperimentalApi
    @Throws(KaCodeCompilationException::class)
    public fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult

    @KaExperimentalApi
    public fun createCompilationOptions(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions

    @KaExperimentalApi
    public fun modify(options: KaCompilationOptions, init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions
}
