/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirFile

/**
 * Extension to support cases where top-level declarations conflict despite the different module, e.g. when a file is analyzed in
 * the context of another module.
 */
abstract class FirConflictsFromDifferentModuleExtension : FirSessionComponent {
    /**
     * Whether top-level declarations of [otherModuleData] can conflict with top-level declarations of the session's module.
     */
    abstract fun canConflictWithDeclarationsOfModule(otherModuleData: FirModuleData): Boolean

    /**
     * Whether [useSiteFile] is analyzed in place of [targetFile], so declarations of [targetFile] cannot conflict with declarations of
     * [useSiteFile].
     */
    abstract fun isAnalyzedInPlaceOf(useSiteFile: FirFile, targetFile: FirFile): Boolean
}

val FirSession.conflictsFromDifferentModuleExtension: FirConflictsFromDifferentModuleExtension?
        by FirSession.nullableSessionComponentAccessor()
