/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

abstract class FirNameConflictsTracker : FirSessionComponent {
    abstract class ClassifierRedeclaration {
        abstract val classifierSymbol: FirClassLikeSymbol<*>

        /**
         * The [FirFile] that contains the [classifierSymbol].
         *
         * This property is optional, as the containing file can also be retrieved via the classifier symbol, but it's recommended to
         * provide it if the file is already known.
         */
        abstract val containingFile: FirFile?
    }

    /**
     * Returns the (possible) [ClassifierRedeclaration]s of *Kotlin* classifiers with the same [classId] declared in the session.
     *
     * A redeclaration is a conflict between two or more *Kotlin* classifiers that have the same class ID. If there is no redeclaration with
     * [classId], an empty collection should be returned. Depending on how classifier name conflicts are tracked, the function may return
     * all conflicting classifiers, or only some of the (actually redeclared) classifiers.
     *
     * Kotlin/Java redeclarations are checked by `FirJvmConflictsChecker` and should not be reported by this component. Otherwise, there
     * will be false positives (`expect` Kotlin classes are allowed to be "redeclared" by a Java class in the same module) and duplicate
     * errors (one reported by `FirJvmConflictsChecker`, one through this component).
     */
    abstract fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration>

    /**
     * Registers a classifier redeclaration with the conflict tracker.
     *
     * The implementation may be a no-op if the conflict tracker gets its information from a different source (such as IDE indices).
     */
    abstract fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: FirClassLikeSymbol<*>,
        newSymbolFile: FirFile,
        prevSymbol: FirClassLikeSymbol<*>,
        prevSymbolFile: FirFile?,
    )
}

val FirSession.nameConflictsTracker: FirNameConflictsTracker? by FirSession.nullableSessionComponentAccessor()
