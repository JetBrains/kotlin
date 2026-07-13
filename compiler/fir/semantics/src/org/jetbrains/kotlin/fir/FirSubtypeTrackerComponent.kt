/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.name.ClassId

/**
 * Reports class-hierarchy edges to incremental compilation.
 *
 * This exists to record supertype relationships that are not captured by the serialized Kotlin metadata alone — most
 * notably edges that cross a Java class (`A(kotlin) <- B(java) <- C(kotlin)`), where `B`'s `extends A` is invisible to
 * the proto-based hierarchy recording under K2. Reporting these edges lets IC's `subtypesMap` (and therefore
 * `withSubtypes`) propagate a supertype change through the Java intermediary to the Kotlin subtype (KT-11196).
 */
abstract class FirSubtypeTrackerComponent : FirSessionComponent {
    /**
     * Records that [subtype] directly extends/implements [supertype].
     */
    abstract fun report(supertype: ClassId, subtype: ClassId)
}

val FirSession.subtypeTracker: FirSubtypeTrackerComponent? by FirSession.nullableSessionComponentAccessor()
