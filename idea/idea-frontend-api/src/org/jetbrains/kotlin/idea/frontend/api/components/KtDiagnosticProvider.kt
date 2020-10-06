/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

abstract class KtDiagnosticProvider : KtAnalysisSessionComponent() {
    abstract fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic>
    abstract fun collectDiagnosticsForFile(ktFile: KtFile): Collection<Diagnostic>
}
