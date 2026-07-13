/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.analysis.api.compilation.KaCodeCompilationException as KaNewCodeCompilationException
import org.jetbrains.kotlin.analysis.api.compilation.KaCompilationResult as KaNewCompilationResult
import org.jetbrains.kotlin.analysis.api.compilation.compile as compileEndpoint
import org.jetbrains.kotlin.analysis.api.compilation.createCompilationOptions as createCompilationOptionsEndpoint
import org.jetbrains.kotlin.analysis.api.compilation.modify as modifyEndpoint

/**
 * Routes the legacy [KaCompilerFacility] surface through the new public `context(session: KaSession)`
 * `org.jetbrains.kotlin.analysis.api.compilation` endpoints, which in turn reach the
 * [org.jetbrains.kotlin.analysis.api.internals.KaInternalsCompilerFacility] proxy.
 *
 * The new endpoints work with the `compilation`-package types, so this bridge narrows the results back to the legacy `components`-package
 * types: the only [KaCompilationResult]/[KaCompilationOptions]/[KaCompiledFile] implementations also implement the legacy interfaces, so
 * the casts are safe. The new [KaNewCodeCompilationException] is rethrown as the legacy [KaCodeCompilationException].
 */
internal class KaCompilerFacilityBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaCompilerFacility {
    @Throws(KaCodeCompilationException::class)
    override fun compile(file: KtFile, options: KaCompilationOptions): KaCompilationResult =
        context(analysisSession) {
            try {
                compileEndpoint(file, options).toLegacyResult()
            } catch (e: KaNewCodeCompilationException) {
                throw KaCodeCompilationException(e.cause ?: e)
            }
        }

    override fun createCompilationOptions(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions =
        context(analysisSession) {
            createCompilationOptionsEndpoint { (this as KaCompilationOptionsBuilder).init() } as KaCompilationOptions
        }

    override fun KaCompilationOptions.modify(init: KaCompilationOptionsBuilder.() -> Unit): KaCompilationOptions =
        context(analysisSession) {
            this@modify.modifyEndpoint { (this as KaCompilationOptionsBuilder).init() } as KaCompilationOptions
        }
}

private fun KaNewCompilationResult.toLegacyResult(): KaCompilationResult =
    when (this) {
        is KaNewCompilationResult.Success -> KaCompilationResult.Success(
            output = output.map { it as KaCompiledFile },
            capturedValues = capturedValues,
            canBeCached = canBeCached,
            mutedExceptions = mutedExceptions,
        )
        is KaNewCompilationResult.Failure -> KaCompilationResult.Failure(
            errors = errors,
            mutedExceptions = mutedExceptions,
        )
    }
