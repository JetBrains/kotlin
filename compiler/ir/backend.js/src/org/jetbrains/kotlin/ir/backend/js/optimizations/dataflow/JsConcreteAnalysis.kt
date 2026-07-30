/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Sparse concrete type/value analysis over [JsControlFlowGraph] with IPA-lite parameter
 * refinement from known call sites in the linked program.
 *
 * Unknown / unsupported IR evaluates to [JsFact.Top]. Exported / external / open functions
 * keep parameter seeds at Top (or declared upper bound only — never caller-refined).
 */

private const val MAX_IPA_ROUNDS = 16

/**
 * Per-function analysis facts.
 */
class JsFunctionFacts(
    val function: IrFunction,
    val cfg: JsControlFlowGraph,
    private val summaries: Map<IrValueDeclaration, JsFact>,
) {
    /**
     * Whole-function summary: join of facts across block exits (and entry params).
     * Used for IPA-lite call-site argument evaluation and dump queries.
     */
    fun summary(value: IrValueDeclaration): JsFact =
        summaries[value] ?: JsFact.Top

    fun allSummaries(): Map<IrValueDeclaration, JsFact> = summaries
}

/**
 * Analysis result for the linked program.
 */
class JsConcreteAnalysisResult(
    val index: JsProgramIndex,
    val functionFacts: Map<IrFunction, JsFunctionFacts>,
) {
    fun summary(function: IrFunction, value: IrValueDeclaration): JsFact =
        functionFacts[function]?.summary(value) ?: JsFact.Top

    fun value(function: IrFunction, value: IrValueDeclaration): JsValueLattice =
        summary(function, value).value

    fun type(function: IrFunction, value: IrValueDeclaration): JsTypeLattice =
        summary(function, value).type
}

private fun JsProgramIndex.analyze(): JsConcreteAnalysisResult {
    var paramSeeds = emptyMap<IrFunction, Map<IrValueDeclaration, JsFact>>()
    var functionFacts = emptyMap<IrFunction, JsFunctionFacts>()

    repeat(MAX_IPA_ROUNDS) {
        functionFacts = functions.mapValues { entry ->
            entry.value.analyze(paramSeeds[entry.value.ir].orEmpty())
        }
        val nextSeeds = computeParamSeeds(functionFacts)
        if (nextSeeds == paramSeeds) {
            return JsConcreteAnalysisResult(this, functionFacts)
        }
        paramSeeds = nextSeeds
    }

    return JsConcreteAnalysisResult(this, functionFacts)
}

private fun JsProgramIndex.computeParamSeeds(
    functionFacts: Map<IrFunction, JsFunctionFacts>,
): Map<IrFunction, Map<IrValueDeclaration, JsFact>> {
    val seeds = mutableMapOf<IrFunction, MutableMap<IrValueDeclaration, JsFact>>()
    val transfer = JsConcreteTransfer()

    for (function in functions.values) {
        if (function.hasUnknownCallers) {
            continue
        }
        val calls = callsTo(function.ir)
        if (calls.isEmpty()) {
            continue
        }
        val params = function.ir.parameters
        val joined = MutableList(params.size) { JsFact.Bottom }
        for (site in calls) {
            val env = site.enclosingFunction?.let { functionFacts[it]?.toSummaryEnv() } ?: FactEnv()
            for (index in params.indices) {
                val arg = site.call.getArgumentOrNull(index) ?: continue
                val argFact = with(transfer) { arg.evaluateWith(env) }
                joined[index] = joined[index].join(argFact)
            }
        }
        val funFacts = mutableMapOf<IrValueDeclaration, JsFact>()
        for (indexedParam in params.withIndex()) {
            val fact = joined[indexedParam.index]
            if (fact != JsFact.Bottom) funFacts[indexedParam.value] = fact
        }
        if (funFacts.isNotEmpty()) {
            seeds[function.ir] = funFacts
        }
    }
    return seeds
}

private fun JsFunctionFacts.toSummaryEnv(): FactEnv {
    val env = FactEnv()
    val keys = linkedSetOf<IrValueDeclaration>()
    keys.addAll(function.parameters)
    for (block in cfg.blocks) {
        for (statement in block.statements) {
            collectValueDecls(statement, keys)
        }
    }
    for (key in keys) {
        env[key] = summary(key)
    }
    // Also include any keys present only in summaries.
    for (entry in allSummaries().entries) {
        env[entry.key] = entry.value
    }
    return env
}

private fun collectValueDecls(element: IrElement, into: MutableSet<IrValueDeclaration>) {
    element.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitVariable(declaration: IrVariable) {
            into += declaration
            declaration.acceptChildrenVoid(this)
        }
    })
}

private fun JsIndexedFunction.analyze(
    paramSeeds: Map<IrValueDeclaration, JsFact>,
): JsFunctionFacts {
    val transfer = JsConcreteTransfer()
    val entryEnv = FactEnv()
    for (param in ir.parameters) {
        entryEnv[param] = paramSeeds[param] ?: param.type.toFact()
    }

    if (cfg.unsupportedConstruct != null) {
        val summaries = mutableMapOf<IrValueDeclaration, JsFact>()
        for (param in ir.parameters) {
            summaries[param] = JsFact.Top
        }
        return JsFunctionFacts(ir, cfg, summaries)
    }

    val inStates = ForwardDataflowSolver().solve(
        cfg = cfg,
        entryState = entryEnv,
        lattice = FactEnv.lattice,
        transfer = transfer,
    )

    val summary = FactEnv()
    for (entry in inStates.entries) {
        val block = entry.key
        val inn = entry.value
        val outs = transfer.transfer(block, inn)
        if (outs.isEmpty()) {
            summary.joinInPlace(transfer.transferUntil(block, inn, stopAt = null))
        } else {
            for (out in outs.values) summary.joinInPlace(out)
        }
    }
    for (entry in entryEnv.map) {
        if (entry.key !in summary.map) summary[entry.key] = entry.value
    }

    return JsFunctionFacts(function = ir, cfg, summaries = summary.map.toMap())
}

private fun IrFunctionAccessExpression.getArgumentOrNull(index: Int): IrExpression? =
    arguments.getOrNull(index)

/**
 * Entry point: build program index and run concrete analysis over the linked program.
 */
context(context: JsIrBackendContext)
fun analyzeJsProgram(modules: Iterable<IrModuleFragment>): JsConcreteAnalysisResult =
    buildJsProgramIndex(modules).analyze()
