/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNativeImplementation
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.js.backend.ast.JsCode
import org.jetbrains.kotlin.js.backend.ast.JsSingleLineComment
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class JsPolyfills private constructor(
    private val polyfills: MutableSet<String>,
    private val generateRegionComments: Boolean = false,
) : Iterable<String> by polyfills {
    constructor(polyfills: Iterable<String>) : this(polyfills.toMutableSet())
    constructor(generateRegionComments: Boolean = false) : this(mutableSetOf<String>(), generateRegionComments)

    fun registerDeclarationNativeImplementation(declaration: IrDeclaration) {
        if (!declaration.isEffectivelyExternal()) return
        val implementation = declaration.getJsNativeImplementation() ?: return
        polyfills.add(implementation.trimIndent())
    }

    fun addAllNeededPolyfillsTo(body: MutableList<JsStatement>) {
        if (polyfills.isEmpty()) return
        body.startRegion("block: polyfills")
        body += polyfills.map { JsCode(it) }
        body.endRegion()
    }

    operator fun plusAssign(another: JsPolyfills) {
        polyfills.addAll(another.polyfills)
    }

    private fun MutableList<JsStatement>.startRegion(description: String = "") {
        if (generateRegionComments) {
            this += JsSingleLineComment("region $description")
        }
    }

    private fun MutableList<JsStatement>.endRegion() {
        if (generateRegionComments) {
            this += JsSingleLineComment("endregion")
        }
    }

}