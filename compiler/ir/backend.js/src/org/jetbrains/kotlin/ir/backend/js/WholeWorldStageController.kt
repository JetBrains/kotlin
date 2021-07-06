/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.util.IdSignature

// Only allows to apply a lowering to the whole world and save the result
class WholeWorldStageController : StageController() {
    override var currentStage: Int = 0

    // TODO assert lowered

    override var currentDeclaration: IrDeclaration? = null
    private var index: Int = 0

    var declarationListsRestricted: Boolean = false

    override fun <T> restrictTo(declaration: IrDeclaration, fn: () -> T): T {
        val previousCurrentDeclaration = currentDeclaration
        val previousIndex = index

        currentDeclaration = declaration
        index = 0

        return try {
            fn()
        } finally {
            currentDeclaration = previousCurrentDeclaration
            index = previousIndex
        }
    }

    override fun createSignature(parentSignature: IdSignature): IdSignature {
        return IdSignature.LoweredDeclarationSignature(parentSignature, currentStage, index++)
    }

    override fun <T> withStage(stage: Int, fn: () -> T): T {
        val oldStage = currentStage
        currentStage = stage
        return try {
            fn()
        } finally {
            currentStage = oldStage
        }
    }

    override fun <T> withInitialIr(block: () -> T): T {
        return withStage(0) {
            declarationRestriction(true, block)
        }
    }

    override fun <T> bodyLowering(fn: () -> T): T {
        val wasRestricted = declarationListsRestricted
        declarationListsRestricted = true

        return try {
            fn()
        } finally {
            declarationListsRestricted = wasRestricted
        }
    }

    override fun <T> unrestrictDeclarationListsAccess(fn: () -> T): T {
        return declarationRestriction(false, fn)
    }

    private fun <T> declarationRestriction(value: Boolean, fn: () -> T): T{
        val wasRestricted = declarationListsRestricted
        declarationListsRestricted = value

        return try {
            fn()
        } finally {
            declarationListsRestricted = wasRestricted
        }
    }

    override fun canAccessDeclarationsOf(irClass: IrClass): Boolean {
        return !declarationListsRestricted || irClass.visibility == DescriptorVisibilities.LOCAL/* && irClass !in context.extractedLocalClasses*/
    }
}