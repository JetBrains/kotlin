/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirJvmAtomicReferenceToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        JvmStandardClassIds.ATOMIC_REFERENCE_CLASS_ID,
        JvmStandardClassIds.atomicByPrimitive,
        MppCheckerKind.Platform,
    )

object FirJvmAtomicReferenceArrayToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        JvmStandardClassIds.ATOMIC_REFERENCE_ARRAY_CLASS_ID,
        JvmStandardClassIds.atomicArrayByPrimitive,
        MppCheckerKind.Platform,
    )
