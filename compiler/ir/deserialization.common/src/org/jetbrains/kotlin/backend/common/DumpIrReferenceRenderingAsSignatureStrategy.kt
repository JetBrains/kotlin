/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions.ReferenceRenderingStrategy
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * This is a custom [ReferenceRenderingStrategy] that, for non-private and non-local declarations,
 * renders signatures instead of KT-like representation of the symbol owner. This way it allows
 * rendering meaningful information about the referenced declaration even if the symbol is unbound.
 * Which helps to make IR dumps more meaningful in certain situations (KLIB tool, some IR tests).
 */
class DumpIrReferenceRenderingAsSignatureStrategy(irMangler: IrMangler) : ReferenceRenderingStrategy.Custom {
    private val signatureComputer = PublicIdSignatureComputer(irMangler)

    override fun renderReference(symbol: IrSymbol): String? {
        val declaration = runIf(symbol.isBound) { symbol.owner as? IrDeclaration }

        if (declaration != null) {
            if (signatureComputer.mangler.run { !declaration.isExported(compatibleMode = false) })
                return null

            // An enum entry is an instance of `IrDeclaration` but not an instance of `IrDeclarationWithVisibility`.
            // So, to make a proper check here, it's necessary to take the first outer `IrDeclarationWithVisibility`.
            val firstDeclarationWithVisibility = (declaration as? IrDeclarationWithVisibility)
                ?: declaration.parents.firstIsInstanceOrNull<IrDeclarationWithVisibility>()

            if (firstDeclarationWithVisibility?.isEffectivelyPrivate() != false)
                return null
        }

        val signature = symbol.signature ?: declaration?.let(signatureComputer::computeSignature)
        if (signature == null || signature.isLocal)
            return null

        return signature.render()
    }
}
