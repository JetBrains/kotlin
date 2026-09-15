/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * Whether this declaration, as a member of a container, contributes its own graph to the container graph.
 *
 * These predicates describe the shape of a container graph without looking at the graph itself, so they hold before the container is
 * resolved. `ControlFlowGraphBuilder` builds container graphs out of exactly these members.
 */
val FirControlFlowGraphOwner.memberShouldHaveGraph: Boolean
    get() = when (this) {
        is FirProperty -> initializer != null || delegate != null || hasExplicitBackingField
        is FirField -> initializer != null
        else -> true
    }

/**
 * @return true for [FirControlFlowGraphOwner] which, as a class instance member,
 * should be part of the class init graph.
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForClass: Boolean
    get() = when (this) {
        is FirProperty if isCompanionBlockMember -> false
        is FirProperty, is FirField -> memberShouldHaveGraph
        is FirConstructor, is FirAnonymousInitializer -> true
        is FirFunction, is FirClass -> false
        is FirEnumEntry -> false
        else -> true
    }

/**
 * @return true for [FirControlFlowGraphOwner] which, as a class static member,
 * should be part of the class static graph.
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForStatic: Boolean
    get() = when (this) {
        is FirProperty if isCompanionBlockMember -> true
        is FirClass if status.isCompanion -> true
        is FirEnumEntry -> true
        else -> false
    }

/**
 * @return true for [FirControlFlowGraphOwner] which, as a class member, should be part of either of the two class graphs.
 *
 * @see isUsedInControlFlowGraphBuilderForClass
 * @see isUsedInControlFlowGraphBuilderForStatic
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForClassOrStatic: Boolean
    get() = isUsedInControlFlowGraphBuilderForClass || isUsedInControlFlowGraphBuilderForStatic

/**
 * @return true for [FirControlFlowGraphOwner] which, as a file member, should be part of the file
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForFile: Boolean
    get() = when (this) {
        is FirProperty -> memberShouldHaveGraph
        else -> false
    }

/**
 * @return true for a symbol whose declaration, as a file member, should be part of the file graph.
 *
 * Being a syntactic property of the declaration, this needs no resolution, unlike looking the declaration up among
 * [ControlFlowGraph.subGraphs] of the file graph, which requires the file to be resolved first.
 *
 * @see isUsedInControlFlowGraphBuilderForFile
 */
val FirBasedSymbol<*>.isUsedInControlFlowGraphBuilderForFile: Boolean
    get() = (fir as? FirControlFlowGraphOwner)?.isUsedInControlFlowGraphBuilderForFile == true

/**
 * @return true for [FirControlFlowGraphOwner] which, as a script statement, should be part of the script
 */
val FirControlFlowGraphOwner.isUsedInControlFlowGraphBuilderForScript: Boolean
    get() = when (this) {
        is FirProperty, is FirField, is FirAnonymousInitializer -> memberShouldHaveGraph
        else -> false
    }

/**
 * Whether this is a container which always gets a graph of its own, so that a missing graph means it is not resolved yet.
 *
 * A missing graph is otherwise ambiguous. An owner which never gets one keeps its content in the graph being traversed – a local
 * property is such an owner, it is a statement of the container graph and gets no graph of its own even with an initializer, so
 * the traversal has to descend into it. A container keeps its content in its own graph instead, and descending into an unresolved
 * one would collect properties the traversed graph knows nothing about. In the Analysis API a container is analyzed without its
 * nested declarations being resolved, so an unresolved nested container is reachable there.
 */
val FirElement.isContainerWithOwnGraph: Boolean
    get() = when (this) {
        is FirFile, is FirScript, is FirClass -> true
        else -> false
    }
