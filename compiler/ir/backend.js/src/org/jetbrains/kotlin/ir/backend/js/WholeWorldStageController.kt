/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.util.IdSignature

// Only allows to apply a lowering to the whole world and save the result
class WholeWorldStageController : StageController() {
    override var currentStage: Int = 0

    // TODO assert lowered

    override var currentDeclaration: IrDeclaration? = null
    private var index: Int = 0

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

    override fun <T> withInitialIr(block: () -> T): T {
        val oldStage = currentStage
        currentStage = 0
        val oldCurrentDeclaration = currentDeclaration
        currentDeclaration = null
        return try {
            block()
        } finally {
            currentStage = oldStage
            currentDeclaration = oldCurrentDeclaration
        }
    }

    override fun createSignature(parentSignature: IdSignature): IdSignature {
        return IdSignature.LoweredDeclarationSignature(parentSignature, currentStage, index++)
    }
}