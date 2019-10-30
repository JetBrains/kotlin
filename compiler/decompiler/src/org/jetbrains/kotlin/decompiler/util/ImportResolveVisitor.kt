/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class ImportResolveVisitor : IrElementVisitor<Unit, String> {
    val declarationNamesSet = mutableSetOf<String>()
    val invocationNamesSet = mutableSetOf<String>()
    override fun visitFile(declaration: IrFile, data: String) {
        with(declaration) {
            val packageName = fqName.asString()
            declarations
                .forEach { it.accept(this@ImportResolveVisitor, packageName) }
        }
    }

    override fun visitClass(declaration: IrClass, data: String) {
        with(declaration) {
            val declarationNameWithPrefix = "$data.${this.name.identifier}"
            declarationNamesSet.add(declarationNameWithPrefix)
            declarations.forEach { it.accept(this@ImportResolveVisitor, declarationNameWithPrefix) }

        }
        super.visitClass(declaration, data)
    }


    override fun visitElement(element: IrElement, data: String) {
        TODO("not implemented")
    }
}