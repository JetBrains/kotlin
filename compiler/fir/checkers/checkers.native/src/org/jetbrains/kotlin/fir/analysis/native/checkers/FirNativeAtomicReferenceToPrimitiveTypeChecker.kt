/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.type.AbstractAtomicReferenceToPrimitiveTypeChecker
import org.jetbrains.kotlin.name.NativeRuntimeNames

object FirNativeAtomicReferenceToPrimitiveTypeChecker :
    AbstractAtomicReferenceToPrimitiveTypeChecker(
        NativeRuntimeNames.AtomicReference,
        NativeRuntimeNames.atomicByPrimitive,
        MppCheckerKind.Platform
    )

object FirNativeAtomicArrayToPrimitiveTypeChecker :
    AbstractAtomicReferenceToPrimitiveTypeChecker(
        NativeRuntimeNames.AtomicArray,
        NativeRuntimeNames.atomicArrayByPrimitive,
        MppCheckerKind.Platform
    )
