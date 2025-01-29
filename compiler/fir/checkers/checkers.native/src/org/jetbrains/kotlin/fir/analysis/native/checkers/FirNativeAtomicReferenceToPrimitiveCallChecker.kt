/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlin.name.NativeRuntimeNames

object FirNativeAtomicReferenceToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeRuntimeNames.atomicByPrimitive,
    MppCheckerKind.Platform,
    NativeRuntimeNames.Callables.atomicReferenceCompareAndSet,
    NativeRuntimeNames.Callables.atomicReferenceCompareAndExchange,
)

object FirNativeAtomicArrayToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeRuntimeNames.atomicArrayByPrimitive,
    MppCheckerKind.Platform,
    NativeRuntimeNames.Callables.atomicArrayCompareAndSet,
    NativeRuntimeNames.Callables.atomicArrayCompareAndExchange,
)
