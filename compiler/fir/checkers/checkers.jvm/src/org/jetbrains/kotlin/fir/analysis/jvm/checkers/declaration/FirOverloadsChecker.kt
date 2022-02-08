/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_CLASS_ID

object FirOverloadsChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(JVM_OVERLOADS_CLASS_ID) ?: return
        //todo need to have expect declaration here to check if it has default values
        if (declaration.isActual) return

        val containingDeclaration = declaration.getContainingClassSymbol(context.session)
        when {
            containingDeclaration?.classKind == ClassKind.INTERFACE ->
                reporter.reportOn(annotation.source, FirJvmErrors.OVERLOADS_INTERFACE, context)
            declaration.isAbstract ->
                reporter.reportOn(annotation.source, FirJvmErrors.OVERLOADS_ABSTRACT, context)
            (declaration is FirSimpleFunction && declaration.isLocal) ||
                    context.containingDeclarations.any { it.isLocalClassOrAnonymousObject() } ->
                reporter.reportOn(annotation.source, FirJvmErrors.OVERLOADS_LOCAL, context)
            declaration is FirConstructor && containingDeclaration?.classKind == ClassKind.ANNOTATION_CLASS ->
                reporter.reportOn(annotation.source, FirJvmErrors.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR, context)
            !declaration.visibility.isPublicAPI && declaration.visibility != Visibilities.Internal ->
                reporter.reportOn(annotation.source, FirJvmErrors.OVERLOADS_PRIVATE, context)
            declaration.valueParameters.none { it.defaultValue != null } ->
                reporter.reportOn(annotation.source, FirJvmErrors.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, context)
        }
    }

}