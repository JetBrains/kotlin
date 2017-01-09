/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.lower

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
        irFile.transformChildrenVoid(this)
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
