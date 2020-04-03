/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.*

class PropertiesLowering : DeclarationTransformer {
    override fun lower(irFile: IrFile) {
        runPostfix(true).toFileLoweringPass().lower(irFile)
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        when (declaration) {
            is IrSimpleFunction -> {
                declaration.correspondingPropertySymbol?.owner?.let { property ->
                    if (!property.isEffectivelyExternal()) {
                        return listOf(declaration)
                    }
                }
            }
            is IrField -> {
                declaration.correspondingPropertySymbol?.owner?.let { property ->
                    if (!property.isEffectivelyExternal()) {
                        return listOf(declaration)
                    }
                }
            }
            is IrProperty -> {
                if (!declaration.isEffectivelyExternal()) {
                    return listOf()
                }
            }
        }

        return null
    }

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

class LocalDelegatedPropertiesLowering : IrElementTransformerVoid(), BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.accept(this, null)
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
