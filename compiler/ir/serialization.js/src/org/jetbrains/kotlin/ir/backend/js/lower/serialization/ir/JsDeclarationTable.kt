/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IdSignatureClashTracker
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render

class JsUniqIdClashTracker : IdSignatureClashTracker {
    private val committedIdSignatures = mutableMapOf<IdSignature, IrDeclaration>()

    override fun commit(declaration: IrDeclaration, signature: IdSignature) {
        if (!signature.isPublic) return // don't track local ids

        if (signature in committedIdSignatures) {
            val clashedDeclaration = committedIdSignatures[signature]!!
            if (declaration !is IrTypeParameter && declaration.descriptor.containingDeclaration !is PropertyDescriptor) {
                // TODO: handle clashes properly
                error("IdSignature clash: $signature; Existed declaration ${clashedDeclaration.render()} clashed with new ${declaration.render()}")
            } else {
                // Check whether they are type parameters of the same extension property but different accessors
                val parent = declaration.parent
                val clashedParent = clashedDeclaration.parent
                require(parent is IrSimpleFunction)
                require(clashedParent is IrSimpleFunction)
                require(clashedDeclaration !== parent)
                require(clashedParent.correspondingPropertySymbol === parent.correspondingPropertySymbol)
            }
        }

        committedIdSignatures[signature] = declaration
    }
}

class JsGlobalDeclarationTable(signatureSerializer: IdSignatureSerializer, builtIns: IrBuiltIns) :
    GlobalDeclarationTable(signatureSerializer, JsManglerIr, JsUniqIdClashTracker()) {
    init {
        loadKnownBuiltins(builtIns)
    }
}