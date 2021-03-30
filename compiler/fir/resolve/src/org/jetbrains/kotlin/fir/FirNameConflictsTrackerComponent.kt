/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

abstract class FirNameConflictsTrackerComponent : FirSessionComponent {

    abstract fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: FirClassLikeSymbol<*>, newSymbolFile: FirFile,
        prevSymbol: FirClassLikeSymbol<*>, prevSymbolFile: FirFile?
    )
}

val FirSession.nameConflictsTracker: FirNameConflictsTrackerComponent? by FirSession.nullableSessionComponentAccessor()
