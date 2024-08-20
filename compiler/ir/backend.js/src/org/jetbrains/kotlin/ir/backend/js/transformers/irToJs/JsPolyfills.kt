/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.hasJsPolyfill
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class JsPolyfills {
    private val polyfillsPerFile = hashMapOf<IrFile, HashSet<IrDeclaration>>()

    fun registerDeclarationNativeImplementation(file: IrFile, declaration: IrDeclaration) {
        if (!declaration.hasJsPolyfill()) return
        val declarations = polyfillsPerFile[file] ?: hashSetOf()
        declarations.add(declaration)
        polyfillsPerFile[file] = declarations
    }

    fun saveOnlyIntersectionOfNextDeclarationsFor(file: IrFile, declarations: Set<IrDeclaration>) {
        val polyfills = polyfillsPerFile[file] ?: return
        polyfillsPerFile[file] = polyfills.intersect(declarations).toHashSet()
    }

    fun getAllPolyfillsFor(file: IrFile): List<JsStatement> =
        polyfillsPerFile[file].orEmpty().asImplementationList()

    private fun Iterable<IrDeclaration>.asImplementationList() =
        asSequence().asImplementationList()

    private fun Sequence<IrDeclaration>.asImplementationList(): List<JsStatement> {
        return map { it to it.getAnnotation(JsAnnotations.JsPolyfillFqn)!!.getValueArgument(0)!! }
            .distinctBy { (it.second as IrConst).value as String }
            .flatMap { (container, polyfill) -> translateJsCodeIntoStatementList(polyfill, null, container).orEmpty() }
            .toList()
    }
}
