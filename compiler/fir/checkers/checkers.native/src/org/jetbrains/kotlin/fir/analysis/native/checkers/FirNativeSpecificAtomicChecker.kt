/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirCallableDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirNativeSpecificAtomicChecker : FirCallableDeclarationChecker(MppCheckerKind.Platform) {
    override fun check(
        declaration: FirCallableDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!declaration.visibility.isPublicAPI || declaration is FirValueParameter || declaration is FirAnonymousFunction) return
        declaration.receiverParameter?.typeRef?.let {
            checkType(it, context, reporter)
        }
        declaration.contextParameters.forEach {
            checkType(it.returnTypeRef, context, reporter)
        }
        declaration.returnTypeRef.takeIf { it.source?.kind is KtRealSourceElementKind }?.let {
            checkType(it, context, reporter)
        }
        // Note: not much sense to check type parameter bounds, or class supertypes: all atomics are final types
        if (declaration !is FirFunction || declaration is FirPropertyAccessor) return
        declaration.valueParameters.forEach {
            checkType(it.returnTypeRef, context, reporter)
        }
    }

    private fun checkType(
        typeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val classId = typeRef.coneType.fullyExpandedClassId(context.session) ?: return
        if (classId.packageFqName != CONCURRENT_PACKAGE) return
        if (classId.parentClassId != null) return
        val name = classId.shortClassName
        if (name !in CONCURRENT_NAME_SET) return
        reporter.reportOn(typeRef.source, FirNativeErrors.NATIVE_SPECIFIC_ATOMIC, name, context)
    }

    private val CONCURRENT_PACKAGE = FqName("kotlin.concurrent")
    private val CONCURRENT_NAME_SET = listOf(
        "AtomicIntArray",
        "AtomicLongArray",
        "AtomicArray",
        "AtomicInt",
        "AtomicLong",
        "AtomicReference",
    ).mapTo(mutableSetOf()) { Name.identifier(it) }.toSet()
}
