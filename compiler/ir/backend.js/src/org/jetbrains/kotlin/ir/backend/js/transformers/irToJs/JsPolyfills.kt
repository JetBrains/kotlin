/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.ir.backend.js.utils.hasJsPolyfill
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.config.compileLambdasAsEs6ArrowFunctions
import org.jetbrains.kotlin.name.Name
import java.util.*

class JsPolyfills(configuration: CompilerConfiguration) {
    private val useEs6Arrows = configuration.compileLambdasAsEs6ArrowFunctions

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

    private fun List<JsStatement>.wrapInIIFE(): List<JsStatement> {
        val funExpr = JsFunction(emptyScope, JsBlock(this), "polyfill iife").apply {
            isEs6Arrow = useEs6Arrows
        }
        val invocation = JsInvocation(funExpr)
        return listOf(invocation.makeStmt())
    }

    private fun Iterable<IrDeclaration>.asImplementationList(): List<JsStatement> {
        val orderedMapOfPolyfills = TreeMap<String, List<JsStatement>>()

        for (declaration in this) {
            val polyfillCodeExpression = declaration.getAnnotation(JsAnnotations.JsPolyfillFqn)
                ?.argumentMapping[Name.identifier("implementation")]
                ?: compilationException(
                    "there is no @JsPolyfill annotation, while the declaration was added to the polyfilled declaration set",
                    declaration
                )

            val polyfillCodeString = (polyfillCodeExpression as IrConst).value as String

            if (polyfillCodeString in orderedMapOfPolyfills) continue

            orderedMapOfPolyfills[polyfillCodeString] = translateJsCodeIntoStatementList(polyfillCodeExpression, declaration)
                // Wrap the polyfill code in an immediately invoked function expression so that 'var's declared in the polyfill
                // don't pollute the global scope.
                ?.wrapInIIFE()
                ?: emptyList()
        }

        return orderedMapOfPolyfills.flatMap { it.value }
    }
}
