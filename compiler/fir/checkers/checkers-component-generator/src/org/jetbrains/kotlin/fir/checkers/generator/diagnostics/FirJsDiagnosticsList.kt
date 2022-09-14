/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.psi.KtExpression

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object JS_DIAGNOSTICS_LIST : DiagnosticList("FirJsErrors") {
    val ANNOTATIONS by object : DiagnosticGroup("Annotations") {
        val WRONG_JS_QUALIFIER by error<KtExpression>()
    }
}
