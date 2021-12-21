/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.getJsNativeImplementation
import org.jetbrains.kotlin.ir.backend.js.utils.hasJsNativeImplementation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.js.backend.ast.JsCode
import org.jetbrains.kotlin.js.backend.ast.JsSingleLineComment
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class JsPolyfills(private val generateRegionComments: Boolean = false) {
    private val declarationsWithNativeImplementations = hashMapOf<IrModuleFragment, HashSet<IrDeclaration>>()

    fun registerDeclarationNativeImplementation(module: IrModuleFragment, declaration: IrDeclaration) {
        if (!declaration.hasJsNativeImplementation()) return
        val declarations = declarationsWithNativeImplementations[module] ?: hashSetOf()
        declarations.add(declaration)
        declarationsWithNativeImplementations[module] = declarations
    }

    fun saveOnlyIntersectionOfNextDeclarationsFor(module: IrModuleFragment, declarations: Set<IrDeclaration>) {
        val polyfills = declarationsWithNativeImplementations[module] ?: return
        declarationsWithNativeImplementations[module] = polyfills.intersect(declarations).toHashSet()
    }

    fun getAllPolyfillsFor(module: IrModuleFragment): List<JsStatement> {
        val declarations = declarationsWithNativeImplementations[module] ?: emptySet()

        if (declarations.isEmpty()) return emptyList()

        return mutableListOf<JsStatement>().apply {
            plusAssign(
                declarations.asSequence()
                .map { it.getJsNativeImplementation() }
                .distinct()
                .map { JsCode(it) }
            )
        }
    }
}