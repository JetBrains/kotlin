/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isOverridable
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.java.findJvmNameAnnotation
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmNameChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val jvmName = declaration.findJvmNameAnnotation() ?: return
        val name = jvmName.findArgumentByName(StandardNames.NAME) ?: return

        if (name.resolvedType != context.session.builtinTypes.stringType.type) {
            return
        }

        val value = (name as? FirLiteralExpression<*>)?.value as? String ?: return

        if (!Name.isValidIdentifier(value)) {
            reporter.reportOn(jvmName.source, FirJvmErrors.ILLEGAL_JVM_NAME, context)
        }

        if (declaration is FirFunction && !context.isRenamableFunction(declaration)) {
            reporter.reportOn(jvmName.source, FirJvmErrors.INAPPLICABLE_JVM_NAME, context)
        } else if (declaration is FirCallableDeclaration) {
            val containingClass = declaration.getContainingClass(context.session)

            if (
                declaration.isOverride ||
                containingClass != null && containingClass.modality != Modality.FINAL && declaration.isOverridable ||
                containingClass?.isValueClassThatRequiresMangling() == true
            ) {
                reporter.reportOn(jvmName.source, FirJvmErrors.INAPPLICABLE_JVM_NAME, context)
            }
        }
    }

    private fun CheckerContext.isRenamableFunction(function: FirFunction): Boolean {
        val containingClass = function.getContainingClassSymbol(session)
        return containingClass != null || !function.symbol.callableId.isLocal
    }

    private fun FirRegularClass.isValueClassThatRequiresMangling(): Boolean {
        // value classes have inline modifiers in FIR
        return isInline && name != StandardClassIds.Result.shortClassName
    }
}
