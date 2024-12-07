/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirConstructorChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.java.enhancement.inheritedKtPrivateCls

object FirJavaClassInheritsKtPrivateClassDeclarationChecker : FirConstructorChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirConstructor, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitJavaClassInheritingPrivateKotlinClass))
            return

        val delegatedConstructorCall = declaration.delegatedConstructor ?: return
        val resolvedDelegatedConstructor = declaration.symbol.resolvedDelegatedConstructor ?: return
        val inheritedKtPrivateCls = resolvedDelegatedConstructor.inheritedKtPrivateCls
        if (inheritedKtPrivateCls != null) {
            val javaClassId = resolvedDelegatedConstructor.containingClassLookupTag()!!.classId
            reporter.reportOn(
                delegatedConstructorCall.source, JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS, javaClassId, inheritedKtPrivateCls, context
            )
        }
    }
}
