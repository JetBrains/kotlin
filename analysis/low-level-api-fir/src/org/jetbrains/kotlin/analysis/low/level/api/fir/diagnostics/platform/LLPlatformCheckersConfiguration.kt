/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.platform

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachComponentPlatform
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * Provides platform-specific checkers for a single platform.
 *
 * The interface should be implemented once per platform kind (JVM, JS, Wasm, Native). Configurations are composable, so checkers from
 * multiple platforms can be registered in the same session (for metadata sessions covering multiple platforms).
 *
 * When the session is a metadata session, [LLPlatformCheckersConfiguration] does NOT have to filter by
 * [platformSpecificCheckerEnabledInMetadataCompilation][org.jetbrains.kotlin.fir.analysis.checkers.FirCheckerWithMppKind.platformSpecificCheckerEnabledInMetadataCompilation].
 * The filtering is later performed in [LLCheckersFactory][org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLCheckersFactory].
 */
internal interface LLPlatformCheckersConfiguration {
    val declarationCheckers: List<DeclarationCheckers>
    val expressionCheckers: List<ExpressionCheckers>
    val typeCheckers: List<TypeCheckers>

    val extraDeclarationCheckers: List<DeclarationCheckers> get() = emptyList()
    val extraExpressionCheckers: List<ExpressionCheckers> get() = emptyList()
    val extraTypeCheckers: List<TypeCheckers> get() = emptyList()

    companion object {
        fun forPlatform(targetPlatform: TargetPlatform): List<LLPlatformCheckersConfiguration> =
            buildList {
                targetPlatform.forEachComponentPlatform(
                    onJvm = { add(LLJvmCheckersConfiguration) },
                    onJs = { add(LLJsCheckersConfiguration) },
                    onWasm = { add(LLWasmCheckersConfiguration(it)) },
                    onNative = { add(LLNativeCheckersConfiguration) },
                )
            }
    }
}
