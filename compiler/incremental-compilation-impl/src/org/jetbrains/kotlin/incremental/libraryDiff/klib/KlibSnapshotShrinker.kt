/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey

/** The klib counterpart of `ClasspathSnapshotShrinker`. */
internal object KlibSnapshotShrinker {

    /** Retains only the snapshots referenced by the lookup symbols stored in [lookupStorage]. */
    fun shrink(allSnapshots: List<KlibSnapshot>, lookupStorage: LookupStorage): List<KlibSnapshot> =
        shrink(allSnapshots, lookupStorage.lookupSymbols)

    /**
     * Retains only the snapshots referenced by [lookupSymbols], directly or transitively (e.g. via supertypes), so that the impact
     * of changes can still be computed (see [KlibChangesComputer.computeChangedAndImpactedSet]).
     */
    @Suppress("unused")
    fun shrink(allSnapshots: List<KlibSnapshot>, lookupSymbols: Collection<LookupSymbolKey>): List<KlibSnapshot> =
        TODO("KT-90559: shrink klib snapshots")
}

/** Flattens the snapshots of all klibs. Duplicate ids are not handled yet (see KT-90559 progress doc). */
internal fun List<KlibEntrySnapshot>.toKlibSnapshots(): List<KlibSnapshot> = flatMap { it.klibSnapshots }
