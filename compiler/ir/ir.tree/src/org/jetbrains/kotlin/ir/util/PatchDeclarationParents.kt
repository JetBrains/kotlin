/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * For each [IrDeclaration] in the IR subtree with the root in `this`, sets its [IrDeclaration.parent]
 * property to its _actual_ parent in the subtree.
 *
 * @param initialParent If this parameter is not `null`, assign topmost [IrDeclaration]s'
 * parents to that value (starting with `this`, if it is an [IrDeclaration]).
 * If null, skip those topmost [IrDeclaration]s' and start assigning parents one level below
 * (this is, once an [IrDeclarationParent] is found).
 */
fun <T : IrElement> T.patchDeclarationParents(initialParent: IrDeclarationParent? = null): T = apply {
    accept(PatchDeclarationParentsVisitor, initialParent)
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
abstract class DeclarationParentsVisitor : IrElementVisitor<Unit, IrDeclarationParent?> {
    override fun visitElement(element: IrElement, actualParent: IrDeclarationParent?) {
        element.acceptChildren(this, actualParent)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment, actualParent: IrDeclarationParent?) {
        declaration.acceptChildren(this, declaration)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, actualParent: IrDeclarationParent?) {
        if (actualParent != null) {
            handleParent(declaration, actualParent)
        }

        val downParent = declaration as? IrDeclarationParent ?: actualParent
        declaration.acceptChildren(this, downParent)
    }

    protected abstract fun handleParent(declaration: IrDeclaration, actualParent: IrDeclarationParent)
}

private object PatchDeclarationParentsVisitor : DeclarationParentsVisitor() {
    override fun handleParent(declaration: IrDeclaration, actualParent: IrDeclarationParent) {
        declaration.parent = actualParent
    }
}
