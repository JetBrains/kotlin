/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.NameTable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

fun <T> wasmNameTable() = NameTable<T>(sanitizer = ::sanitizeWatIdentifier)

fun generateWatTopLevelNames(packages: List<IrPackageFragment>): Map<IrDeclarationWithName, String> {
    val names = wasmNameTable<IrDeclarationWithName>()

    fun nameTopLevelDecl(declaration: IrDeclarationWithName, prefix: String = "") {
        val suggestedName = declaration.fqNameWhenAvailable?.toString()
            ?: "fqname???" + declaration.name.asString()
        names.declareFreshName(declaration, prefix + suggestedName)
    }

    for (p in packages) {
        p.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                val prefix = if (declaration.origin == IrDeclarationOrigin.BRIDGE) "BRIDGE_" else ""
                nameTopLevelDecl(declaration, prefix)
                super.visitSimpleFunction(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                nameTopLevelDecl(declaration)
                super.visitConstructor(declaration)
            }

            override fun visitField(declaration: IrField) {
                if (declaration.isTopLevel)
                    nameTopLevelDecl(declaration)
                else
                    nameTopLevelDecl(declaration, declaration.parentAsClass.name.identifier + ".")
                super.visitField(declaration)
            }
        })
    }

    return names.names
}

fun generateWatTypeNames(packages: List<IrPackageFragment>): Map<IrDeclarationWithName, String> {
    val names = wasmNameTable<IrDeclarationWithName>()

    fun nameType(declaration: IrDeclarationWithName, typeKind: String) {
        val suggestedName = declaration.fqNameWhenAvailable?.toString()
            ?: "fqname???" + declaration.name.asString()
        names.declareFreshName(declaration, "type_of_${typeKind}_" + suggestedName)
    }

    for (p in packages) {
        p.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                nameType(declaration, "fun")
                super.visitSimpleFunction(declaration)
            }

            override fun visitClass(declaration: IrClass) {
                if (!declaration.isInterface)
                    nameType(declaration, "class")
                super.visitClass(declaration)
            }
        })
    }

    return names.names
}


fun sanitizeWatIdentifier(ident: String): String {
    if (ident.isEmpty())
        return "_"
    if (ident.all(::isValidWatIdentifier))
        return ident
    return ident.map { if (isValidWatIdentifier(it)) it else "_" }.joinToString("")
}

// https://webassembly.github.io/spec/core/text/values.html#text-id
fun isValidWatIdentifier(c: Char): Boolean =
    c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z'
            // TODO: SpiderMonkey js shell can't parse some of the
            //  permitted identifiers: '?', '<'
            // || c in "!#$%&′*+-./:<=>?@\\^_`|~"
            || c in "$.@_"

