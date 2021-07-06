/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IdSignatureClashTracker
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

class JsUniqIdClashTracker() : IdSignatureClashTracker {
    private val committedIdSignatures = mutableMapOf<IdSignature, IrDeclaration>()

    override fun commit(declaration: IrDeclaration, signature: IdSignature) {
        if (!signature.isPublic) return // don't track local ids

        if (signature in committedIdSignatures) {
            val clashedDeclaration = committedIdSignatures[signature]!!
            val parent = declaration.parent
            val clashedParent = clashedDeclaration.parent
            if (declaration is IrTypeParameter && parent is IrSimpleFunction && parent.parent is IrProperty && parent !== clashedParent) {
                // Check whether they are type parameters of the same extension property but different accessors
                require(clashedParent is IrSimpleFunction)
                require(clashedParent.correspondingPropertySymbol === parent.correspondingPropertySymbol)
            } else {
                // TODO: handle clashes properly
                error("IdSignature clash: $signature; Existed declaration ${clashedDeclaration.render()} clashed with new ${declaration.render()}")
            }
        }

        committedIdSignatures[signature] = declaration
    }
}

class JsGlobalDeclarationTable(builtIns: IrBuiltIns, tracker: IdSignatureClashTracker = JsUniqIdClashTracker()) :
    GlobalDeclarationTable(JsManglerIr, tracker) {
    init {
        loadKnownBuiltins(builtIns)
    }
}