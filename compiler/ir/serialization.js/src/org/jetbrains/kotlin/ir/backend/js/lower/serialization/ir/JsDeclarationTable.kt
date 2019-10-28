/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.render

class JsUniqIdClashTracker : UniqIdClashTracker {
    private val committedUniqIds = mutableMapOf<UniqId, IrDeclaration>()

    override fun commit(declaration: IrDeclaration, uniqId: UniqId) {
        if (uniqId.isLocal) return // don't track local ids

        if (uniqId in committedUniqIds) {
            val clashedDeclaration = committedUniqIds[uniqId]!!
            if (declaration !is IrTypeParameter && declaration.descriptor.containingDeclaration !is PropertyDescriptor) {
                // TODO: handle clashes properly
                error("UniqId clash: $uniqId; Existed declaration ${clashedDeclaration.render()} clashed with new ${declaration.render()}")
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

        committedUniqIds[uniqId] = declaration
    }
}

class JsGlobalDeclarationTable(builtIns: IrBuiltIns) : GlobalDeclarationTable(JsMangler, JsUniqIdClashTracker()) {
    init {
        loadKnownBuiltins(builtIns)
    }
}