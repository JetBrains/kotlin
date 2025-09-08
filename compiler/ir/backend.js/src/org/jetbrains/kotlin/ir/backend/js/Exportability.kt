/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

sealed class Exportability {
    object Allowed : Exportability()
    object NotNeeded : Exportability()
    object Implicit : Exportability()
    class Prohibited(val reason: String) : Exportability()
}
