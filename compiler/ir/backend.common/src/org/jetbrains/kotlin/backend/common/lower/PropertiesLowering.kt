/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.*

class PropertiesLowering : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitFile(declaration: IrFile): IrFile {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { lowerProperty(it) }
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { lowerProperty(it) }
        return declaration
    }

    override fun visitScript(declaration: IrScript): IrStatement {
        declaration.transformChildrenVoid(this)
        declaration.transformDeclarationsFlat { lowerProperty(it) }
        return declaration
    }

    private fun lowerProperty(declaration: IrDeclaration): List<IrDeclaration>? =
        if (declaration is IrProperty && !declaration.isEffectivelyExternal())
            listOfNotNull(declaration.backingField, declaration.getter, declaration.setter)
        else null

    companion object {
        fun checkNoProperties(irFile: IrFile) {
            irFile.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitProperty(declaration: IrProperty) {
                    error("No properties should remain at this stage")
                }
            })
        }
    }
}

class LocalDelegatedPropertiesLowering : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
        declaration.transformChildrenVoid(this)

        val initializer = declaration.delegate.initializer!!
        declaration.delegate.initializer = IrBlockImpl(
            initializer.startOffset, initializer.endOffset, initializer.type, null,
            listOfNotNull(
                declaration.getter,
                declaration.setter,
                initializer
            )
        )

        return declaration.delegate
    }
}
