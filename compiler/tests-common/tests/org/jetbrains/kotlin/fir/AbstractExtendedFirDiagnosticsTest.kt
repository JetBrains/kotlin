/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ExtendedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExtendedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent

abstract class AbstractExtendedFirDiagnosticsTest : AbstractFirDiagnosticsTest() {
    override fun configureSession(session: FirSession) {
        session.checkersComponent.register(ExtendedDeclarationCheckers)
        session.checkersComponent.register(ExtendedExpressionCheckers)
    }
}