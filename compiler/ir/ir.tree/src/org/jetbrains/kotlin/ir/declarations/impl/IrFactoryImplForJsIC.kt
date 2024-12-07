/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.IdSignature

class IrFactoryImplForJsIC(stageController: StageController) : IrFactory(stageController), IdSignatureRetriever {
    override fun <T : IrDeclaration> T.declarationCreated(): T {
        val parentSig = stageController.currentDeclaration?.let { declarationSignature(it) } ?: return this

        stageController.createSignature(parentSig)?.let { this.signatureForJsIC = it }

        return this
    }

    override fun declarationSignature(declaration: IrDeclaration): IdSignature? {
        return declaration.signatureForJsIC ?: declaration.symbol.signature ?: declaration.symbol.privateSignature
    }
}

private var IrDeclaration.signatureForJsIC: IdSignature? by irAttribute(followAttributeOwner = false)
