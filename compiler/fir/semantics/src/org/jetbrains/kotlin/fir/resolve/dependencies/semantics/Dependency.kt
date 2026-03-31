/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies.semantics

sealed interface Dependency {

    val from: NodeIndex<*>

    val to: NodeIndex<*>

    data class Access(override val from: NodeIndex<*>, override val to: NodeIndex<*>) : Dependency

    sealed interface TimeDependency : Dependency {
        // reduces the amount of dynamic casting
        val alwaysHappens: Boolean

        val possiblyHappens: Boolean
    }

    data class HappensBefore(override val from: NodeIndex<*>, override val to: NodeIndex<*>) : TimeDependency {
        override val alwaysHappens: Boolean = true
        override val possiblyHappens: Boolean = true
    }

    data class MayHappenBefore(override val from: NodeIndex<*>, override val to: NodeIndex<*>) : TimeDependency {
        override val alwaysHappens: Boolean = false
        override val possiblyHappens: Boolean = true
    }

    companion object {
        operator fun Dependency.component1(): NodeIndex<*> = from
        operator fun Dependency.component2(): NodeIndex<*> = to
    }
}