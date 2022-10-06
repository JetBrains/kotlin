/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.common.lower.StringTrimLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.FoldConstantLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.CopyInlineFunctionBodyLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.CollectSingleCallInlinableFunctions
import org.jetbrains.kotlin.ir.backend.js.lower.inline.CollectPotentiallyInlinableFunctions
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.lower.inline.*
import org.jetbrains.kotlin.ir.backend.js.dce.eliminateDeadDeclarations

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

private val jsSpecificConstantFoldingLoweringPhase = makeBodyLoweringPhase(
    ::JsSpecificConstantFoldingLowering,
    name = "JsSpecificConstantFoldingLowering",
    description = "[Optimization] Constant Folding for JS",
)

private val speculationLoweringPhase = makeBodyLoweringPhase(
    ::SpeculationLowering,
    name = "SpeculationLowering",
    description = "[Optimization] Speculate on some values and types",
)

private val collectFunctionUsagesLoweringPhase = makeBodyLoweringPhase(
    ::CollectFunctionUsagesLowering,
    name = "CollectFunctionUsagesLowering",
    description = "[Analysis] Collect information about functions call",
)

private val collectSingleCallInlinableFunctionsLoweringPhase = makeDeclarationTransformerPhase(
    ::CollectSingleCallInlinableFunctions,
    name = "CollectSingleCallInlinableFunctions",
    description = "[Optimization] Collect single call inlinable functions",
    prerequisite = setOf(collectFunctionUsagesLoweringPhase)
)

private val collectPotentiallyInlinableFunctionsLoweringPhase = makeBodyLoweringPhase(
    ::CollectPotentiallyInlinableFunctions,
    name = "CollectPotentiallyInlinableFunctions",
    description = "[Optimization] Collect potentially inlinable functions",
    prerequisite = setOf(collectSingleCallInlinableFunctionsLoweringPhase)
)

private val functionSpecializationLoweringPhase = makeDeclarationTransformerPhase(
    ::FunctionSpecializationLowering,
    name = "FunctionSpecializationLowering",
    description = "[Optimization] Speculate with time of parameters",
    prerequisite = setOf(collectFunctionUsagesLoweringPhase, speculationLoweringPhase)
)


private val functionInliningPhase = makeBodyLoweringPhase(
    { FunctionInlining(it, it.innerClassesSupport) },
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(collectPotentiallyInlinableFunctionsLoweringPhase)
)

private val copyInlineFunctionBodyLoweringPhase = makeDeclarationTransformerPhase(
    ::CopyInlineFunctionBodyLowering,
    name = "CopyInlineFunctionBody",
    description = "Copy inline function body",
    prerequisite = setOf(functionInliningPhase)
)

private val returnableBlockLoweringPhase = makeBodyLoweringPhase(
    ::JsReturnableBlockLowering,
    name = "JsReturnableBlockLowering",
    description = "Introduce temporary variable for result and change returnable block's type to Unit",
    prerequisite = setOf(functionInliningPhase)
)

private val blockDecomposerLoweringPhase = makeBodyLoweringPhase(
    ::JsBlockDecomposerLowering,
    name = "BlockDecomposerLowering",
    description = "Transform statement-like-expression nodes into pure-statement to make it easily transform into JS",
    prerequisite = setOf(returnableBlockLoweringPhase)
)

private val compositeToBlockLoweringPhase = makeBodyLoweringPhase(
    { CompositeToBlockLowering() },
    name = "CompositeToBlockLowering",
    description = "Transform all composites into blocks",
    prerequisite = setOf(blockDecomposerLoweringPhase)
)

private val objectUsageLoweringPhase = makeBodyLoweringPhase(
    ::SimplifiedObjectUsageLowering,
    name = "ObjectUsageLowering",
    description = "Transform IrGetObjectValue into instance generator call"
)

private val collectSimpleVariablesLoweringPhase = makeBodyLoweringPhase(
    ::CollectSimpleVariables,
    name = "CollectSimpleVariables",
    description = "[Optimization] Collect simple variables to collapse",
)

private val variablesCollapsingLoweringPhase = makeBodyLoweringPhase(
    ::VariablesCollapsingLowering,
    name = "VariablesCollapsingLowering",
    description = "[Optimization] Collapse simple variables",
)

private val removeUnreachableStatementsLowering = makeBodyLoweringPhase(
    ::RemoveUnreachableStatementsLowering,
    name = "RemoveUnreachableStatementsLowering",
    description = "[Optimization] Remove unreachable statements",
)

private val unfoldBlocksLowering = makeBodyLoweringPhase(
    ::UnfoldBlocksLowering,
    name = "UnfoldBlocksLowering",
    description = "[Optimization] Remove nested blocks",
)

private val cleanOptimizationContextLoweringPhase = makeCustomJsModulePhase(
    { context, _ -> context.optimizations.reset() },
    name = "CleanOptimizationContextLowerinPhase",
    description = "[Analysis] Remove collected analytic",
)

private val jsMainOptimizations = NamedCompilerPhase(
    name = "IrOptimizations",
    description = "IR lowerings with one-time optimizations before main optimization loop",
    lower = speculationLoweringPhase then
            collectFunctionUsagesLoweringPhase then
            functionSpecializationLoweringPhase then
            collectSingleCallInlinableFunctionsLoweringPhase then
            collectPotentiallyInlinableFunctionsLoweringPhase then
            functionInliningPhase then
            copyInlineFunctionBodyLoweringPhase then
            returnableBlockLoweringPhase then
            blockDecomposerLoweringPhase then
//            compositeToBlockLoweringPhase then
            collectSimpleVariablesLoweringPhase then
            variablesCollapsingLoweringPhase then
            jsSpecificConstantFoldingLoweringPhase then
            foldConstantLoweringPhase then
            removeUnreachableStatementsLowering then
            objectUsageLoweringPhase then
            unfoldBlocksLowering then
            cleanOptimizationContextLoweringPhase
)

const val MAX_NUMBER_OF_OPTIMIZATION_ITERATIONS = 1

fun runOptimizationsLoop(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext
) {
    // Run prefix optimizations
    jsPrefixOptimizations.invokeToplevel(PhaseConfig(jsPrefixOptimizations), context, modules)

    for (i in 0 until MAX_NUMBER_OF_OPTIMIZATION_ITERATIONS) {
        jsMainOptimizations.invokeToplevel(PhaseConfig(jsMainOptimizations), context, modules)
        eliminateDeadDeclarations(modules, context)
    }
}

infix fun Lowering.then(other: Lowering) = modulePhase then other.modulePhase
infix fun CompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>, Iterable<IrModuleFragment>>.then(other: Lowering) =
    this then other.modulePhase