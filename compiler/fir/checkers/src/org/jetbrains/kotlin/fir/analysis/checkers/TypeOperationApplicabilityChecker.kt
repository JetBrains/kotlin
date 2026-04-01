/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.types.ConeKotlinType

// an abstract general checker for applicability of type operations, with a simple "yes"/"no" result
abstract class TypeOperationApplicabilityChecker {
    abstract fun isApplicable(leftType: ConeKotlinType, rightType: ConeKotlinType): Boolean
}
