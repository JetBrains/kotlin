/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment


/**
 * Target to be lazily resolved by LL FIR lazy resolver.
 *
 * Specifies the path to the resolve targets and resolve targets themselves.
 * Those targets are going to be resolved by [org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirModuleLazyDeclarationResolver]
 */
sealed class LLFirResolveTarget(
    /**
     * [FirFile] where the targets are located
     */
    val firFile: FirFile,

    /**
     * The list of [FirScript] and [FirRegularClass] which are the required to go from file to target declarations in the top-down order.
     *
     * If resolve target is [FirRegularClass] or [FirScript] itself, it's not included into the [path]
     */
    val path: List<FirDeclaration>,
) {
    /**
     * Executions the [action] for each target that this [LLFirResolveTarget] represents.
     */
    abstract fun forEachTarget(action: (FirElementWithResolveState) -> Unit)

    override fun toString(): String = buildString {
        append(this::class.simpleName)
        append("(")
        buildList {
            add(firFile.name)
            path.mapTo(this) {
                when (it) {
                    is FirRegularClass -> it.name
                    is FirScript -> it.name
                    else -> errorWithFirSpecificEntries("Unsupported path declaration: ${it::class.simpleName}", fir = it)
                }
            }

            add(toStringForTarget())
        }.joinTo(this, separator = " -> ")
        append(")")
    }

    protected abstract fun toStringForTarget(): String
}

/**
 * Resolves the target to the specified [phase].
 * The owning session must be a resolvable one.
 */
fun LLFirResolveTarget.resolve(phase: FirResolvePhase) {
    val session = firFile.llFirResolvableSession
        ?: errorWithAttachment("Resolvable session expected, got '${firFile.llFirSession::class.java}'") {
            withEntry("firSession", firFile.llFirSession) { it.toString() }
        }

    val lazyDeclarationResolver = session.moduleComponents.firModuleLazyDeclarationResolver
    lazyDeclarationResolver.lazyResolveTarget(this, phase, towerDataContextCollector = null)
}