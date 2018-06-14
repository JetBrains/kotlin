/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class BackingFieldVisitor(val context: Context) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        super.visitProperty(declaration)

        if (declaration.isDelegated) {
            val irClass = declaration.parent as? IrClass
            val list = irClass?.let { context.ir.classesDelegatedBackingFields.getOrPut(irClass.descriptor) { mutableListOf() } }
            list?.add(declaration.backingField!!.descriptor)
        }
        if (declaration.backingField == null || declaration.isDelegated) return
        assert(declaration.backingField!!.descriptor == declaration.descriptor) {
            "backing field descriptor mismatch: ${declaration.backingField!!.descriptor} != ${declaration.descriptor}"
        }

        context.ir.propertiesWithBackingFields.add(declaration.descriptor)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isInner)
            declaration.declarations += context.specialDeclarationsFactory.getOuterThisField(declaration)

        // Mark all dangling fields (they are created when class is inherited via delegation).
        declaration.declarations.filterIsInstance<IrField>().forEach {
            val list = context.ir.classesDelegatedBackingFields.getOrPut(declaration.descriptor) { mutableListOf() }
            list.add(it.descriptor)
        }

        super.visitClass(declaration)
    }
}

internal fun markBackingFields(context: Context) {
    context.irModule!!.accept(BackingFieldVisitor(context), null)
}

