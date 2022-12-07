/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

/**
 * Used as a result of exploring IR types to find out if the type uses any partially linked classifier or has a visibility conflict.
 * For more details see [LinkedClassifierExplorer.exploreType].
 */
internal sealed interface TypeExplorationResult {
    /** Indicates unusable type. */
    sealed interface UnusableType : TypeExplorationResult {
        class DueToClassifier(val classifier: ExploredClassifier.Unusable) : UnusableType

        class DueToVisibilityConflict(
            val classifierWithConflictingVisibility1: ExploredClassifier.Usable.AccessibleClassifier,
            val classifierWithConflictingVisibility2: ExploredClassifier.Usable.AccessibleClassifier
        ) : UnusableType
    }

    /** Indicates usable type that does not reference any partially linked classifiers and does not have visibility conflicts. */
    class UsableType(val classifierWithNarrowestVisibility: ExploredClassifier.Usable?) : TypeExplorationResult {
        companion object {
            val DEFAULT_PUBLIC = UsableType(null)
        }
    }
}
