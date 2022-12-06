/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.ClassifierExplorationResult.Fully.AccessibleClassifier
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ClassifierExplorationResult.Partially

internal sealed interface TypeExplorationResult {
    class UnusableType(val cause: Partially) : TypeExplorationResult

    class UsableType(val narrowestVisibility: ABIVisibility, val dueTo: AccessibleClassifier?) : TypeExplorationResult
}
