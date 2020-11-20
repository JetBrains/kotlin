/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findNonInterfaceSupertype
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration

object FirEnumClassSimpleChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.ENUM_CLASS) {
            return
        }

        declaration.findNonInterfaceSupertype(context)?.let {
            reporter.reportClassInSupertypeForEnum(it.source)
        }

        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportTypeParametersInEnum(declaration.typeParameters.firstOrNull()?.source)
        }
    }

    private fun DiagnosticReporter.reportClassInSupertypeForEnum(source: FirSourceElement?) {
        source?.let { report(FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM.on(it)) }
    }

    private fun DiagnosticReporter.reportTypeParametersInEnum(source: FirSourceElement?) {
        source?.let { report(FirErrors.TYPE_PARAMETERS_IN_ENUM.on(it)) }
    }
}