/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPersistingElementBase

open class MutableController(override var currentStage: Int = 0) : StageController {

    override var bodiesEnabled: Boolean = true

    override fun <T> withStage(stage: Int, fn: () -> T): T {
        val prevStage = currentStage
        currentStage = stage
        try {
            return fn()
        } finally {
            currentStage = prevStage
        }
    }

    override fun <T> withInitialIr(block: () -> T): T = restrictionImpl(null) { withStage(0, block) }

    override fun <T> withInitialStateOf(declaration: IrDeclaration, block: () -> T): T = withStage((declaration as? IrPersistingElementBase<*>)?.createdOn ?: 0, block)

    private var restricted: Boolean = false

    private var restrictedToDeclaration: IrDeclaration? = null

    override fun <T> restrictTo(declaration: IrDeclaration, fn: () -> T): T = restrictionImpl(declaration, fn)

    private fun <T> restrictionImpl(declaration: IrDeclaration?, fn: () -> T): T {
        val prev = restrictedToDeclaration
        restrictedToDeclaration = declaration
        val wereBodiesEnabled = bodiesEnabled
        bodiesEnabled = false
        val wasRestricted = restricted
        restricted = true
        val wereDeclarationListsRestricted = declarationListsRestricted
        declarationListsRestricted = true
        try {
            return fn()
        } finally {
            restrictedToDeclaration = prev
            bodiesEnabled = wereBodiesEnabled
            restricted = wasRestricted
            declarationListsRestricted = wereDeclarationListsRestricted
        }
    }

    override fun <T> bodyLowering(fn: () -> T): T {
        val wereBodiesEnabled = bodiesEnabled
        bodiesEnabled = true
        val wasRestricted = restricted
        restricted = true
        val wereDeclarationListsRestricted = declarationListsRestricted
        declarationListsRestricted = true
        try {
            return fn()
        } finally {
            bodiesEnabled = wereBodiesEnabled
            restricted = wasRestricted
            declarationListsRestricted = wereDeclarationListsRestricted
        }
    }

    override fun canModify(element: IrElement): Boolean {
        return !restricted || restrictedToDeclaration === element || element is IrPersistingElementBase<*> && element.createdOn == currentStage
    }

    private var declarationListsRestricted = false

    override fun <T> unrestrictDeclarationListsAccess(fn: () -> T): T {
        val wereDeclarationListsRestricted = declarationListsRestricted
        declarationListsRestricted = false
        try {
            return fn()
        } finally {
            declarationListsRestricted = wereDeclarationListsRestricted
        }
    }

    override fun canAccessDeclarationsOf(irClass: IrClass): Boolean {
        return !declarationListsRestricted || irClass.visibility == Visibilities.LOCAL /*|| currentStage == (irClass as? IrPersistingElementBase<*>)?.createdOn ?: 0*/
    }
}