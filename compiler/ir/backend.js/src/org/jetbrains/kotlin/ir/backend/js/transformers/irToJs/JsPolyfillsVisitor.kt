/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNativeImplementation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.js.util.TextOutput

class JsPolyfillsVisitor {
    private val polyfills = mutableSetOf<String>()

    fun visitDeclaration(declaration: IrDeclaration) {
        val implementation = declaration.getJsNativeImplementation() ?: return
        polyfills.add(implementation)
    }

    fun addAllNeededPolyfillsTo(output: TextOutput) {
        if (polyfills.isEmpty()) return
        output.print("// region block: polyfills")
        output.print(polyfills.joinToString("\n"))
        output.print("// endregion\n")
    }
}