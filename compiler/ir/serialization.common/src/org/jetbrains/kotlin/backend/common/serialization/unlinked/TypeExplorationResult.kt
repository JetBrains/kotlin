/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.ClassifierExplorationResult.Fully
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ClassifierExplorationResult.Partially

internal sealed interface TypeExplorationResult {
    /** Indicates unusable type. */
    sealed interface UnusableType : TypeExplorationResult {
        class DueToClassifier(val classifier: Partially) : UnusableType

        class DueToVisibilityConflict(
            val classifierWithConflictingVisibility1: Fully.AccessibleClassifier,
            val classifierWithConflictingVisibility2: Fully.AccessibleClassifier
        ) : UnusableType
    }

    /** Indicates usable type that does not reference any partially linked classifiers and does not have visibility conflicts. */
    class UsableType(val classifierWithNarrowestVisibility: Fully?) : TypeExplorationResult {
        companion object {
            val DEFAULT_PUBLIC = UsableType(null)
        }
    }
}
