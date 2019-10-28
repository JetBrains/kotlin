/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.checkers.declaration

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.diagnostics.DiagnosticReporter

abstract class FirDeclarationChecker<in D : FirDeclaration> {
    abstract fun check(declaration: D, reporter: DiagnosticReporter)
}