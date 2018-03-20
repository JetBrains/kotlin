/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class PropertiesLowering : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitFile(declaration: IrFile): IrFile {
        declaration.transformChildrenVoid(this)
        declaration.declarations.transformFlat { lowerProperty(it) }
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid(this)
        declaration.declarations.transformFlat { lowerProperty(it) }
        return declaration
    }

    private fun lowerProperty(declaration: IrDeclaration): List<IrDeclaration>? =
            if (declaration is IrProperty)
                ArrayList<IrDeclaration>(3).apply {
                    if (!DescriptorUtils.isAnnotationClass(declaration.descriptor.containingDeclaration)) {
                        addIfNotNull(declaration.backingField)
                    }
                    addIfNotNull(declaration.getter)
                    addIfNotNull(declaration.setter)
                }
            else
                null
}
