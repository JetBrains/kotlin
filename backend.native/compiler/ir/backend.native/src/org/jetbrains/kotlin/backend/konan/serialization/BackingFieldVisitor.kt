package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class BackingFieldVisitor(val context: Context) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        if (declaration.backingField == null) return
        assert(declaration.backingField!!.descriptor 
            == declaration.descriptor || declaration.isDelegated)

        context.ir.propertiesWithBackingFields.add(declaration.descriptor)
    }
}

internal fun markBackingFields(context: Context) {
    context.irModule!!.accept(BackingFieldVisitor(context), null)
}

