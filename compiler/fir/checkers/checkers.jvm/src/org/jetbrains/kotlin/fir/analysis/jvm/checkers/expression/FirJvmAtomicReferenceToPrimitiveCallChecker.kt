/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractAtomicReferenceToPrimitiveCallChecker
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

object FirJvmAtomicReferenceToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        JvmStandardClassIds.atomicByPrimitive,
        MppCheckerKind.Platform,
        JvmStandardClassIds.Callables.atomicReferenceCompareAndSet,
        JvmStandardClassIds.Callables.atomicReferenceWeakCompareAndSet,
        JvmStandardClassIds.Callables.atomicReferenceWeakCompareAndSetAcquire,
        JvmStandardClassIds.Callables.atomicReferenceWeakCompareAndSetRelease,
        JvmStandardClassIds.Callables.atomicReferenceWeakCompareAndSetPlain,
        JvmStandardClassIds.Callables.atomicReferenceWeakCompareAndSetVolatile,
        JvmStandardClassIds.Callables.atomicReferenceCompareAndExchange,
        JvmStandardClassIds.Callables.atomicReferenceCompareAndExchangeAcquire,
        JvmStandardClassIds.Callables.atomicReferenceCompareAndExchangeRelease,
    ) {
    override fun isDangerousAtomicCallParameterNameWithin(function: FirFunctionSymbol<*>, name: Name): Boolean =
        super.isDangerousAtomicCallParameterNameWithin(function, name)
                || name == Name.identifier("p0") || name == Name.identifier("p1")
}

object FirJvmAtomicReferenceArrayToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        JvmStandardClassIds.atomicArrayByPrimitive,
        MppCheckerKind.Platform,
        JvmStandardClassIds.Callables.atomicReferenceArrayCompareAndSet,
        JvmStandardClassIds.Callables.atomicReferenceArrayWeakCompareAndSet,
        JvmStandardClassIds.Callables.atomicReferenceArrayWeakCompareAndSetAcquire,
        JvmStandardClassIds.Callables.atomicReferenceArrayWeakCompareAndSetRelease,
        JvmStandardClassIds.Callables.atomicReferenceArrayWeakCompareAndSetPlain,
        JvmStandardClassIds.Callables.atomicReferenceArrayWeakCompareAndSetVolatile,
        JvmStandardClassIds.Callables.atomicReferenceArrayCompareAndExchange,
        JvmStandardClassIds.Callables.atomicReferenceArrayCompareAndExchangeAcquire,
        JvmStandardClassIds.Callables.atomicReferenceArrayCompareAndExchangeRelease,
    ) {
    override fun isDangerousAtomicCallParameterNameWithin(function: FirFunctionSymbol<*>, name: Name): Boolean =
        super.isDangerousAtomicCallParameterNameWithin(function, name)
                || name == Name.identifier("p1") || name == Name.identifier("p2")
}
