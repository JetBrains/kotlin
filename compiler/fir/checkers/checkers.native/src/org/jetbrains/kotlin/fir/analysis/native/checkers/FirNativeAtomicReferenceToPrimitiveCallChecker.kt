/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlin.name.NativeStandardClassIds

object FirNativeAtomicReferenceToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeStandardClassIds.atomicByPrimitive_native,
    MppCheckerKind.Platform,
    NativeStandardClassIds.Callables.AtomicReference_compareAndSet_native,
    NativeStandardClassIds.Callables.AtomicReference_compareAndExchange_native,
)

object FirNativeAtomicArrayToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeStandardClassIds.atomicArrayByPrimitive_native,
    MppCheckerKind.Platform,
    NativeStandardClassIds.Callables.AtomicArray_compareAndSet_native,
    NativeStandardClassIds.Callables.AtomicArray_compareAndExchange_native,
)
