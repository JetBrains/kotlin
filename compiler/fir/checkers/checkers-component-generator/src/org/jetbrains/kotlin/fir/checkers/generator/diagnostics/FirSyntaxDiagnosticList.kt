/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object SYNTAX_DIAGNOSTIC_LIST : DiagnosticList("FirSyntaxErrors") {
    val Syntax by object : DiagnosticGroup("Syntax") {
        val SYNTAX by error<PsiElement> {
            parameter<String>("message")
        }
    }
}