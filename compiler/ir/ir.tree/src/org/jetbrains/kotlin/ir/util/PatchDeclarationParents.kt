/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.util.*

fun <T : IrElement> T.patchDeclarationParents(initialParent: IrDeclarationParent? = null) =
    apply {
        val visitor = initialParent?.let { PatchDeclarationParentsVisitor(it) } ?: PatchDeclarationParentsVisitor()
        acceptVoid(visitor)
    }

abstract class DeclarationParentsVisitor : IrElementVisitorVoid {
    protected val declarationParentsStack = ArrayDeque<IrDeclarationParent>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        declarationParentsStack.push(declaration)
        super.visitPackageFragment(declaration)
        declarationParentsStack.pop()
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        handleParent(declaration, declarationParentsStack.peekFirst())

        if (declaration is IrDeclarationParent) {
            declarationParentsStack.push(declaration)
        }

        super.visitDeclaration(declaration)

        if (declaration is IrDeclarationParent) {
            declarationParentsStack.pop()
        }
    }

    protected abstract fun handleParent(declaration: IrDeclaration, parent: IrDeclarationParent)
}

class PatchDeclarationParentsVisitor() : DeclarationParentsVisitor() {

    constructor(containingDeclaration: IrDeclarationParent) : this() {
        declarationParentsStack.push(containingDeclaration)
    }

    override fun handleParent(declaration: IrDeclaration, parent: IrDeclarationParent) {
        declaration.parent = parent
    }
}
