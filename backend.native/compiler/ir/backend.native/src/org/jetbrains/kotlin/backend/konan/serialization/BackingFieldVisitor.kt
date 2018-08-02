/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
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
        assert(declaration.backingField!!.descriptor == declaration.descriptor)

        context.ir.propertiesWithBackingFields.add(declaration.descriptor)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isInner)
            declaration.addChild(context.specialDeclarationsFactory.getOuterThisField(declaration))

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

