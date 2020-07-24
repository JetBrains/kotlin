/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSealedClassConstructorCallChecker : FirQualifiedAccessChecker() {
    override fun check(functionCall: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val constructorFir = functionCall.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol
            ?.fir.safeAs<FirConstructor>()
            ?: return

        val typeClassId = constructorFir.returnTypeRef.safeAs<FirResolvedTypeRef>()
            ?.type.safeAs<ConeClassLikeType>()
            ?.lookupTag
            ?.classId
            ?: return

        val typeFir = typeClassId.toRegularClass(context)
            ?: return

        if (typeFir.status.modality == Modality.SEALED) {
            reporter.report(functionCall.calleeReference.source)
        }
    }

    private fun ClassId.toRegularClass(context: CheckerContext): FirRegularClass? = if (!isLocal) {
        context.session.firSymbolProvider.getClassLikeSymbolByFqName(this)
            ?.fir.safeAs()
    } else {
        context.containingDeclarations
            .lastOrNull { it.safeAs<FirRegularClass>()?.classId == this }
            .safeAs()
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.SEALED_CLASS_CONSTRUCTOR_CALL.on(it)) }
    }
}