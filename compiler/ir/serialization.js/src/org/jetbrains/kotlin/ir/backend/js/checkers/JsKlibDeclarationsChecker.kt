/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.ir.IrDiagnosticReporter

interface JsKlibDeclarationsChecker<D> {
    fun check(declarations: List<D>, context: JsKlibDiagnosticContext, reporter: IrDiagnosticReporter)
}

typealias JsKlibExportedDeclarationsChecker = JsKlibDeclarationsChecker<JsKlibExportingDeclaration>
