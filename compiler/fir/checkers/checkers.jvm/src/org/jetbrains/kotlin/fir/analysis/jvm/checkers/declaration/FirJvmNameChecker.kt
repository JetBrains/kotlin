/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirJvmNameChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirAnnotatedDeclaration) {
            return
        }

        val jvmName = declaration.findJvmNameAnnotation() ?: return
        val name = jvmName.findArgumentByName(Name.identifier("name")) ?: return

        if (name.typeRef.coneType != context.session.builtinTypes.stringType.type) {
            return
        }

        val value = name.safeAs<FirConstExpression<*>>()?.value as? String ?: return

        if (!Name.isValidIdentifier(value)) {
            reporter.reportOn(jvmName.source, FirJvmErrors.ILLEGAL_JVM_NAME, context)
        }
    }

    private fun FirAnnotatedDeclaration.findJvmNameAnnotation(): FirAnnotationCall? {
        return annotations.firstOrNull {
            it.calleeReference.safeAs<FirResolvedNamedReference>()?.name?.toString() == "JvmName"
        }
    }
}
