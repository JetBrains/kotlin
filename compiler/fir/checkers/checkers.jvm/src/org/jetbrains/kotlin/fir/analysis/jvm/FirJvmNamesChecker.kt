/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.name.Name

object FirJvmNamesChecker {
    // See The Java Virtual Machine Specification, section 4.7.9.1 https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1
    private val INVALID_CHARS = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

    // These characters can cause problems on Windows. '?*"|' are not allowed in file names, and % leads to unexpected env var expansion.
    private val DANGEROUS_CHARS = setOf('?', '*', '"', '|', '%')


    fun checkNameAndReport(name: Name, declarationSource: KtSourceElement?, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declarationSource != null && declarationSource.kind !is KtFakeSourceElementKind && !name.isSpecial) {
            val nameString = name.asString()
            if (nameString.any { it in INVALID_CHARS }) {
                reporter.reportOn(
                    declarationSource,
                    FirErrors.INVALID_CHARACTERS,
                    "contains illegal characters: ${INVALID_CHARS.intersect(nameString.toSet()).joinToString("")}",
                    context
                )
            } else if (nameString.any { it in DANGEROUS_CHARS }) {
                reporter.reportOn(
                    declarationSource,
                    FirErrors.DANGEROUS_CHARACTERS,
                    DANGEROUS_CHARS.intersect(nameString.toSet()).joinToString(""),
                    context
                )
            }
        }

    }
}