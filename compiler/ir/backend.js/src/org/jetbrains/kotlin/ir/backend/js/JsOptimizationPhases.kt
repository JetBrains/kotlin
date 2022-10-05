/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.lower.StringTrimLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.FoldConstantLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

private val computeStringTrimPhase = makeJsModulePhase(
    ::StringTrimLowering,
    name = "StringTrimLowering",
    description = "Compute trimIndent and trimMargin operations on constant strings"
).toModuleLowering()

private val jsPrefixOptimizations = NamedCompilerPhase(
    name = "IrOptimizationPrefix",
    description = "IR lowerings with one-time optimizations before main optimization loop",
    lower = computeStringTrimPhase.modulePhase
)

private val foldConstantLoweringPhase = makeBodyLoweringPhase(
    { FoldConstantLowering(it, true) },
    name = "FoldConstantLowering",
    description = "[Optimization] Constant Folding",
)

private val jsMainOptimizations = NamedCompilerPhase(
    name = "IrOptimizations",
    description = "IR lowerings with one-time optimizations before main optimization loop",
    lower = foldConstantLoweringPhase.modulePhase
)

private val validateIrAfterLowering = makeCustomJsModulePhase(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
).toModuleLowering()

private val jsPostfixOptimizations = NamedCompilerPhase(
    name = "IrOptimizationPostifx",
    description = "IR lowerings with one-time optimizations after main optimization loop",
    lower = validateIrAfterLowering.modulePhase
)

const val MAX_NUMBER_OF_OPTIMIZATION_ITERATIONS = 10

fun runOptimizationsLoop(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext
) {
    // Run prefix optimizations
    jsPrefixOptimizations.invokeToplevel(PhaseConfig(jsPrefixOptimizations), context, modules)

    for (i in 0 until MAX_NUMBER_OF_OPTIMIZATION_ITERATIONS) {
        jsMainOptimizations.invokeToplevel(PhaseConfig(jsMainOptimizations), context, modules)
    }

    // Run postfix optimizations with validations
    jsPostfixOptimizations.invokeToplevel(PhaseConfig(jsPostfixOptimizations), context, modules)
}