/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkAtomicReferenceAccess
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

abstract class AbstractAtomicReferenceToPrimitiveTypeChecker(
    val atomicReferenceClassId: ClassId,
    val appropriateCandidatesForArgument: Map<ClassId, ClassId>,
    mppKind: MppCheckerKind,
) : FirResolvedTypeRefChecker(mppKind) {
    final override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        checkAtomicReferenceAccess(
            typeRef.coneType, typeRef.source,
            atomicReferenceClassId, appropriateCandidatesForArgument,
            context, reporter,
        )
    }
}

object FirCommonAtomicReferenceToPrimitiveTypeChecker :
    AbstractAtomicReferenceToPrimitiveTypeChecker(
        StandardClassIds.AtomicReference,
        StandardClassIds.atomicByPrimitive,
        MppCheckerKind.Platform,
    )

object FirCommonAtomicReferenceArrayToPrimitiveTypeChecker :
    AbstractAtomicReferenceToPrimitiveTypeChecker(
        StandardClassIds.AtomicArray,
        StandardClassIds.atomicArrayByPrimitive,
        MppCheckerKind.Platform,
    )
