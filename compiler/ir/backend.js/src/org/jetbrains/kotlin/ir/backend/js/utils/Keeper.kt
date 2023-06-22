/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class Keeper(private val keep: Set<String>) : IrElementVisitor<Unit, Keeper.KeepData> {
    private val keptDeclarations: MutableSet<IrDeclaration> = mutableSetOf()

    fun shouldKeep(declaration: IrDeclaration): Boolean {
        return declaration in keptDeclarations ||
                (declaration is IrOverridableDeclaration<*> && declaration.overriddenSymbols.any { shouldKeep(it.owner as IrDeclaration) })
    }

    override fun visitElement(element: IrElement, data: KeepData) {
        element.acceptChildren(this, data)
    }

    /** Keep declarations can work both ways
     * if member of a class is in keep, the class should be also kept
     * if a class is kept, members of the class should be also kept
     * but there can be nested classes, and for nested classes we need only to propagate "keep" from top-level to nested (not vice versa)
     * because we have 2 directions, we need 2 boolean flags
     * [KeepData.classInKeep] responsible to propagate "keep" from class level to members direction
     * [KeepData.classShouldBeKept] responsible to bubble "keep" from members to class level direction
     */
    override fun visitClass(declaration: IrClass, data: KeepData) {
        val prevShouldBeKept = data.classShouldBeKept
        val prevClassInKeep = data.classInKeep
        data.classShouldBeKept = false
        val keptClass = data.classInKeep || isInKeep(declaration)
        if (keptClass) {
            keptDeclarations.add(declaration)
        }
        data.classInKeep = keptClass
        super.visitClass(declaration, data)
        if (data.classShouldBeKept) {
            keptDeclarations.add(declaration)
        }
        data.classShouldBeKept = prevShouldBeKept
        data.classInKeep = prevClassInKeep
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: KeepData) {
        super.visitDeclaration(declaration, data)
        if (declaration in keptDeclarations) {
            return
        }
        if (declaration is IrDeclarationWithName && isInKeep(declaration) || data.classInKeep) {
            keptDeclarations.add(declaration)
            data.classShouldBeKept = true
            return
        }
    }

    private fun isInKeep(declaration: IrDeclarationWithName): Boolean {
        return declaration.fqNameWhenAvailable?.asString() in keep
    }

    class KeepData(var classInKeep: Boolean, var classShouldBeKept: Boolean)
}
