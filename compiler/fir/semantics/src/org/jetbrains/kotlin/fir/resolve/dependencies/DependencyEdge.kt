/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull

sealed interface DependencyEdge {

    val from: DependencyNodeIndex

    val to: DependencyNodeIndex

    companion object {
        operator fun DependencyEdge.component1(): DependencyNodeIndex = from
        operator fun DependencyEdge.component2(): DependencyNodeIndex = to
    }
}

sealed interface InformationEdge : DependencyEdge {
    override val from: AccessibleIndex

    val accessSources: Set<KtSourceElement>

    fun merge(other: Set<KtSourceElement>): InformationEdge

    companion object {
        operator fun InformationEdge.component1(): AccessibleIndex = from
        operator fun InformationEdge.component3(): Set<KtSourceElement> = accessSources
    }
}

sealed interface HappensBeforeEdge : DependencyEdge {
    val holdsInAllExecutions: Boolean get() = false
}

data class IsReferencedBy(
    override val from: AccessibleIndex,
    override val to: DependencyNodeIndex,
    override val accessSources: Set<KtSourceElement> = emptySet(),
) : InformationEdge {
    constructor(from: AccessibleIndex, to: DependencyNodeIndex, accessSource: KtSourceElement?) : this(
        from = from,
        to = to,
        accessSources = SmartSet.create<KtSourceElement>().also { it.addIfNotNull(accessSource) }
    )

    override fun merge(other: Set<KtSourceElement>): IsReferencedBy = copy(accessSources = SmartSet.create(accessSources + other))
}

data class IsCalledBy(
    override val from: FunctionIndex<*>,
    override val to: DependencyNodeIndex,
    override val accessSources: Set<KtSourceElement> = emptySet(),
) : InformationEdge, HappensBeforeEdge {
    constructor(from: FunctionIndex<*>, to: DependencyNodeIndex, accessSource: KtSourceElement?) : this(
        from = from,
        to = to,
        accessSources = SmartSet.create<KtSourceElement>().also { it.addIfNotNull(accessSource) }
    )

    override fun merge(other: Set<KtSourceElement>): IsCalledBy = copy(accessSources = SmartSet.create(accessSources + other))
}

data class MustHappenBefore(
    override val from: DependencyNodeIndex,
    override val to: DependencyNodeIndex,
) : HappensBeforeEdge {
    override val holdsInAllExecutions: Boolean = true
}

data class MayHappenBefore(
    override val from: DependencyNodeIndex,
    override val to: DependencyNodeIndex,
) : HappensBeforeEdge
