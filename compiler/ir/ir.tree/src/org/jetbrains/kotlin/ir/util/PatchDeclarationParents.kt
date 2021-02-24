/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.util.*

fun <T : IrElement> T.patchDeclarationParents(initialParent: IrDeclarationParent? = null) =
    apply {
        val visitor = initialParent?.let { PatchDeclarationParentsVisitor(it) } ?: PatchDeclarationParentsVisitor()
        acceptVoid(visitor)
    }

class PatchDeclarationParentsVisitor() : IrElementVisitorVoid {

    constructor(containingDeclaration: IrDeclarationParent) : this() {
        declarationParentsStack.push(containingDeclaration)
    }

    private val declarationParentsStack = ArrayDeque<IrDeclarationParent>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        declarationParentsStack.push(declaration)
        super.visitPackageFragment(declaration)
        declarationParentsStack.pop()
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        patchParent(declaration)

        if (declaration is IrDeclarationParent) {
            declarationParentsStack.push(declaration)
        }

        super.visitDeclaration(declaration)

        if (declaration is IrDeclarationParent) {
            declarationParentsStack.pop()
        }
    }

    private fun patchParent(declaration: IrDeclaration) {
        declaration.parent = declarationParentsStack.peekFirst()
    }
}
