/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.hasJsPolyfill
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import java.util.TreeMap

class JsPolyfills {
    private val polyfillsPerFile = hashMapOf<IrFile, MutableSet<IrDeclaration>>()

    fun registerDeclarationNativeImplementation(file: IrFile, declaration: IrDeclaration) {
        if (!declaration.hasJsPolyfill()) return
        val declarations = polyfillsPerFile[file] ?: hashSetOf()
        declarations.add(declaration)
        polyfillsPerFile[file] = declarations
    }

    fun saveOnlyIntersectionOfNextDeclarationsFor(file: IrFile, declarations: Set<IrDeclaration>) {
        val polyfills = polyfillsPerFile[file] ?: return
        polyfillsPerFile[file] = polyfills.intersect(declarations) as MutableSet<IrDeclaration>
    }

    fun getAllPolyfillsFor(file: IrFile): List<JsStatement> =
        polyfillsPerFile[file].orEmpty().asImplementationList()

    private fun Iterable<IrDeclaration>.asImplementationList(): List<JsStatement> {
        val orderedMapOfPolyfills = TreeMap<String, List<JsStatement>>()

        for (declaration in this) {
            val polyfillCodeExpression = declaration.getAnnotation(JsAnnotations.JsPolyfillFqn)?.arguments?.get(0) ?: compilationException(
                "there is no @JsPolyfill annotation, while the declaration was added to the polyfilled declaration set",
                declaration
            )

            val polyfillCodeString = (polyfillCodeExpression as IrConst).value as String

            if (polyfillCodeString in orderedMapOfPolyfills) continue

            orderedMapOfPolyfills[polyfillCodeString] = translateJsCodeIntoStatementList(polyfillCodeExpression, declaration) ?: emptyList()
        }

        return orderedMapOfPolyfills.flatMap { it.value }
    }
}
