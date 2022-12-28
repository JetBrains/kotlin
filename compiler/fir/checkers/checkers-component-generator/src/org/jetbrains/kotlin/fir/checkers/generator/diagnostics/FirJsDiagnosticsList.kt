/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtDeclaration

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object JS_DIAGNOSTICS_LIST : DiagnosticList("FirJsErrors") {
    val DECLARATIONS by object : DiagnosticGroup("Declarations") {
        val EXPORTED_NAME_CLASH by error<KtDeclaration> {
            parameter<String>("nameWhichClash")
            parameter<FirBasedSymbol<*>>("anotherExportedDeclarationWhichHasTheSameName")
        }
    }
}
