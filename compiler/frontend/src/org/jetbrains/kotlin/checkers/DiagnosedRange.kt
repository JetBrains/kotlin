/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic

class DiagnosedRange constructor(val start: Int) {
    var end: Int = 0
    private val diagnostics = ContainerUtil.newSmartList<TextDiagnostic>()

    fun getDiagnostics(): List<TextDiagnostic> {
        return diagnostics
    }

    fun addDiagnostic(diagnostic: String) {
        diagnostics.add(TextDiagnostic.parseDiagnostic(diagnostic))
    }
}
