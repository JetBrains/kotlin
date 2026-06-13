/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics

import org.jetbrains.kotlin.K1Deprecation

@K1Deprecation
data class PositionalTextDiagnostic(val diagnostic: TextDiagnostic, val start: Int, val end: Int)
