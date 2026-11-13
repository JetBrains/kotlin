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
        JvmStandardClassIds.atomicByPrimitive_java,
        MppCheckerKind.Platform,
        JvmStandardClassIds.Callables.AtomicReference_compareAndSet_java,
        JvmStandardClassIds.Callables.AtomicReference_weakCompareAndSet_java,
        JvmStandardClassIds.Callables.AtomicReference_weakCompareAndSetAcquire_java,
        JvmStandardClassIds.Callables.AtomicReference_weakCompareAndSetRelease_java,
        JvmStandardClassIds.Callables.AtomicReference_weakCompareAndSetPlain_java,
        JvmStandardClassIds.Callables.AtomicReference_weakCompareAndSetVolatile_java,
        JvmStandardClassIds.Callables.AtomicReference_compareAndExchange_java,
        JvmStandardClassIds.Callables.AtomicReference_compareAndExchangeAcquire_java,
        JvmStandardClassIds.Callables.AtomicReference_compareAndExchangeRelease_java,
    ) {
    override fun isDangerousAtomicCallParameterNameWithin(function: FirFunctionSymbol<*>, name: Name): Boolean =
        super.isDangerousAtomicCallParameterNameWithin(function, name)
                || name == Name.identifier("p0") || name == Name.identifier("p1")
}

object FirJvmAtomicReferenceArrayToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        JvmStandardClassIds.atomicArrayByPrimitive_java,
        MppCheckerKind.Platform,
        JvmStandardClassIds.Callables.AtomicReferenceArray_compareAndSet,
        JvmStandardClassIds.Callables.AtomicReferenceArray_weakCompareAndSet,
        JvmStandardClassIds.Callables.AtomicReferenceArray_weakCompareAndSetAcquire,
        JvmStandardClassIds.Callables.AtomicReferenceArray_weakCompareAndSetRelease,
        JvmStandardClassIds.Callables.AtomicReferenceArray_weakCompareAndSetPlain,
        JvmStandardClassIds.Callables.AtomicReferenceArray_weakCompareAndSetVolatile,
        JvmStandardClassIds.Callables.AtomicReferenceArray_compareAndExchange,
        JvmStandardClassIds.Callables.AtomicReferenceArray_compareAndExchangeAcquire,
        JvmStandardClassIds.Callables.AtomicReferenceArray_compareAndExchangeRelease,
    ) {
    override fun isDangerousAtomicCallParameterNameWithin(function: FirFunctionSymbol<*>, name: Name): Boolean =
        super.isDangerousAtomicCallParameterNameWithin(function, name)
                || name == Name.identifier("p1") || name == Name.identifier("p2")
}
