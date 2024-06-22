/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.dump
import java.util.*

class IrFactoryImplForWasmIC(stageController: StageController) : IrFactory(stageController), IdSignatureRetriever {
    private val declarationToSignature = WeakHashMap<IrDeclaration, IdSignature>()

    override fun <T : IrDeclaration> T.declarationCreated(): T {
        val parentSig = stageController.currentDeclaration?.let { declarationSignature(it) } ?: return this

        stageController.createSignature(parentSig)?.let { declarationToSignature[this] = it }

        return this
    }

    override fun declarationSignature(declaration: IrDeclaration): IdSignature? {
        when (declaration) {
            is IrFunction, is IrProperty, is IrClass, is IrField -> Unit
            else -> return null
        }

        val signature = declarationToSignature[declaration]
            ?: declaration.symbol.signature
            ?: declaration.symbol.privateSignature
        if (signature != null) return signature

        val unknownDeclaration = IdSignature.ScopeLocalDeclaration(declaration.dump().hashCode(), "UNKNOWN")
        declarationToSignature[declaration] = unknownDeclaration
        return unknownDeclaration
    }
}
