/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic

interface DiagnosticRenderer<in D : UnboundDiagnostic> {
    fun render(diagnostic: D): String
}
