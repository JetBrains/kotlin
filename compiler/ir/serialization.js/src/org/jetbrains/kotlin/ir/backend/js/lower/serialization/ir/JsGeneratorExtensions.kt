/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions

class JsGeneratorExtensions : GeneratorExtensions() {
    override val debugInfoOnlyOnVariablesInDestructuringDeclarations: Boolean
        get() = true
}