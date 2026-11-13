/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlin.name.NativeStandardClassIds

object FirNativeAtomicReferenceToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeStandardClassIds.atomicByPrimitive,
    MppCheckerKind.Platform,
    NativeStandardClassIds.Callables.atomicReferenceCompareAndSet,
    NativeStandardClassIds.Callables.atomicReferenceCompareAndExchange,
)

object FirNativeAtomicArrayToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeStandardClassIds.atomicArrayByPrimitive,
    MppCheckerKind.Platform,
    NativeStandardClassIds.Callables.atomicArrayCompareAndSet,
    NativeStandardClassIds.Callables.atomicArrayCompareAndExchange,
)
