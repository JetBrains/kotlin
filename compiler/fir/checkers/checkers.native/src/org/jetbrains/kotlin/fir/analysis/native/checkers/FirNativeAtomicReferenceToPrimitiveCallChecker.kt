/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.NativeRuntimeNames

object FirNativeAtomicReferenceToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeRuntimeNames.AtomicReference,
    NativeRuntimeNames.atomicByPrimitive,
    MppCheckerKind.Platform,
)

object FirNativeAtomicArrayToPrimitiveCallChecker : AbstractAtomicReferenceToPrimitiveCallChecker(
    NativeRuntimeNames.AtomicArray,
    NativeRuntimeNames.atomicArrayByPrimitive,
    MppCheckerKind.Platform,
) {
    override val FirBasedSymbol<*>.canInstantiateProblematicAtomicReference: Boolean
        get() = this is FirFunctionSymbol<*> && callableId == NativeRuntimeNames.Callables.AtomicArray
}
