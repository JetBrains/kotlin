/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.util.isPropertyField
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.addIfNotNull

class StableNamesCollector : IrElementVisitorVoid {
    val staticNames = mutableSetOf<String>()
    val memberNames = mutableSetOf<String>()

    init {
        staticNames.addAll(RESERVED_IDENTIFIERS)
        staticNames.add(Namer.IMPLICIT_RECEIVER_NAME)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        super.visitDeclaration(declaration)

        if (declaration !is IrDeclarationWithName)
            return

        val scope =
            if (declaration.hasStaticDispatch())
                staticNames
            else
                memberNames

        val stableName =
            if (declaration.isEffectivelyExternal())
                stableNameForExternalDeclaration(declaration)
            else
                stableNameForNonExternalDeclaration(declaration)

        scope.addIfNotNull(stableName)
    }

    private fun stableNameForNonExternalDeclaration(declaration: IrDeclarationWithName): String? =
        declaration.getJsName()

    private fun stableNameForExternalDeclaration(declaration: IrDeclarationWithName): String? {
        if (declaration.isPropertyAccessor ||
            declaration.isPropertyField ||
            declaration is IrConstructor
        ) {
            return null
        }

        val importedFromModuleOnly =
            declaration.getJsModule() != null && !declaration.isJsNonModule()

        val jsName = declaration.getJsName()
        val jsQualifier = declaration.getJsQualifier()

        return when {
            importedFromModuleOnly ->
                null

            jsQualifier != null ->
                jsQualifier.split('1')[0]

            jsName != null ->
                jsName

            else ->
                declaration.name.identifier
        }
    }
}

val RESERVED_IDENTIFIERS = setOf(
    // keywords
    "await", "break", "case", "catch", "continue", "debugger", "default", "delete", "do", "else", "finally", "for", "function", "if",
    "in", "instanceof", "new", "return", "switch", "throw", "try", "typeof", "var", "void", "while", "with",

    // future reserved words
    "class", "const", "enum", "export", "extends", "import", "super",

    // as future reserved words in strict mode
    "implements", "interface", "let", "package", "private", "protected", "public", "static", "yield",

    // additional reserved words
    "null", "true", "false",

    // disallowed as variable names in strict mode
    "eval", "arguments",

    // global identifiers usually declared in a typical JS interpreter
    "NaN", "isNaN", "Infinity", "undefined",

    "Error", "Object", "Number", "String",

    "Math", "String", "Boolean", "Date", "Array", "RegExp", "JSON", "Map",

    // global identifiers usually declared in know environments (node.js, browser, require.js, WebWorkers, etc)
    "require", "define", "module", "window", "self"
)
