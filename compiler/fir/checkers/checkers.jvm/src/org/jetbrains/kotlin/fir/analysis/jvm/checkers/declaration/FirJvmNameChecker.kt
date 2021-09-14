/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isOpen
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirJvmNameChecker : FirBasicDeclarationChecker() {
    private val NAME = Name.identifier("name")

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirAnnotatedDeclaration) {
            return
        }

        val jvmName = declaration.findJvmNameAnnotation() ?: return
        val name = jvmName.findArgumentByName(NAME) ?: return

        if (name.typeRef.coneType != context.session.builtinTypes.stringType.type) {
            return
        }

        val value = name.safeAs<FirConstExpression<*>>()?.value as? String ?: return

        if (!Name.isValidIdentifier(value)) {
            reporter.reportOn(jvmName.source, FirJvmErrors.ILLEGAL_JVM_NAME, context)
        }

        if (declaration is FirFunction && !context.isRenamableFunction(declaration)) {
            reporter.reportOn(jvmName.source, FirJvmErrors.INAPPLICABLE_JVM_NAME, context)
        } else if (declaration is FirCallableDeclaration) {
            val containingClass = declaration.getContainingClass(context.session)

            if (
                declaration.isOverride ||
                declaration.isOpen ||
                containingClass?.isInlineThatRequiresMangling() == true
            ) {
                reporter.reportOn(jvmName.source, FirJvmErrors.INAPPLICABLE_JVM_NAME, context)
            }
        }
    }

    private fun FirAnnotatedDeclaration.findJvmNameAnnotation(): FirAnnotation? {
        return annotations.firstOrNull {
            it.annotationTypeRef.coneType.classId == StandardClassIds.Annotations.JvmName
        }
    }

    private fun CheckerContext.isRenamableFunction(function: FirFunction): Boolean {
        val containingClass = function.getContainingClassSymbol(session)
        return containingClass != null || !function.symbol.callableId.isLocal
    }

    private fun FirRegularClass.isInlineThatRequiresMangling(): Boolean {
        return isInline && name == StandardClassIds.Result.shortClassName
    }
}
