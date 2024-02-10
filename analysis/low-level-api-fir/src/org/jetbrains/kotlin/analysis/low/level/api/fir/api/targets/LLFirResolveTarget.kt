/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript

/**
 * [target] element and optionally its subgraph to be lazily resolved by LL FIR lazy resolver.
 *
 * Specifies the path to resolve targets and resolve targets themselves.
 * Those targets are going to be resolved by [LLFirModuleLazyDeclarationResolver][org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirModuleLazyDeclarationResolver].
 *
 * @see FirDesignation
 */
internal sealed class LLFirResolveTarget(val designation: FirDesignation) {
    /**
     * [FirFile] where the targets are located.
     * Can be null if [target] does not belong to any file.
     * E.g., fake overrides.
     * @see org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
     */
    val firFile: FirFile? get() = designation.fileOrNull

    /**
     * The list of [FirFile], [FirScript] and [FirRegularClass] which are
     * the required to go from file to target declarations in the top-down order.
     *
     * If resolve target is [FirFile], [FirScript] or [FirRegularClass] itself, it's not included into the [path].
     */
    val path: List<FirDeclaration> get() = designation.path

    /**
     * A dedicated main element.
     */
    val target: FirElementWithResolveState get() = designation.target

    /**
     * Visit [path], [target] and optionally its subgraph.
     * Each nested declaration will be wrapped with corresponding [LLFirResolveTargetVisitor.withFile],
     * [LLFirResolveTargetVisitor.withRegularClass] and [LLFirResolveTargetVisitor.withScript] recursively.
     */
    fun visit(visitor: LLFirResolveTargetVisitor) {
        if (target is FirFile) {
            visitor.performAction(target)
        }

        goToTarget(visitor)
    }

    private fun goToTarget(visitor: LLFirResolveTargetVisitor) {
        val pathIterator = path.iterator()
        goToTarget(pathIterator, visitor)
    }

    private fun goToTarget(
        pathIterator: Iterator<FirDeclaration>,
        visitor: LLFirResolveTargetVisitor,
    ) {
        if (pathIterator.hasNext()) {
            when (val declaration = pathIterator.next()) {
                is FirRegularClass -> visitor.withRegularClass(declaration) { goToTarget(pathIterator, visitor) }
                is FirScript -> visitor.withScript(declaration) { goToTarget(pathIterator, visitor) }
                is FirFile -> visitor.withFile(declaration) { goToTarget(pathIterator, visitor) }
                else -> errorWithFirSpecificEntries(
                    "Unexpected declaration in path: ${declaration::class.simpleName}",
                    fir = declaration,
                )
            }
        } else {
            visitTargetElement(target, visitor)
        }
    }

    /**
     * [element] with [FirFile] will be processed before.
     */
    protected abstract fun visitTargetElement(
        element: FirElementWithResolveState,
        visitor: LLFirResolveTargetVisitor,
    )

    /**
     * Executions the [action] for each target that this [LLFirResolveTarget] represents.
     */
    fun forEachTarget(action: (FirElementWithResolveState) -> Unit) {
        visit(object : LLFirResolveTargetVisitor {
            override fun performAction(element: FirElementWithResolveState) {
                action(element)
            }
        })
    }

    override fun toString(): String = buildString {
        append(this@LLFirResolveTarget::class.simpleName)
        append("(")
        buildList {
            path.mapTo(this) {
                when (it) {
                    is FirFile -> it.name
                    is FirRegularClass -> it.name
                    is FirScript -> it.name
                    else -> errorWithFirSpecificEntries("Unsupported path declaration: ${it::class.simpleName}", fir = it)
                }
            }

            add(toStringForTarget())
            toStringAdditionalSuffix()?.let(this::add)
        }.joinTo(this, separator = " -> ")
        append(")")
    }

    protected open fun toStringAdditionalSuffix(): String? = null

    private fun toStringForTarget(): String = when (val fir = target) {
        is FirConstructor -> "constructor"
        is FirClassLikeDeclaration -> fir.symbol.name.asString()
        is FirCallableDeclaration -> fir.symbol.name.asString()
        is FirAnonymousInitializer -> ("<init-block>")
        is FirFileAnnotationsContainer -> "<file annotations>"
        is FirScript -> fir.name.asString()
        else -> "???"
    }
}
