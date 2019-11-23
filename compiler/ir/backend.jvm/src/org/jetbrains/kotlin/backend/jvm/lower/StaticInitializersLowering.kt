/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.InitializersLoweringBase
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.Name

class StaticInitializersLowering(context: CommonBackendContext) : InitializersLoweringBase(context) {
    override fun lower(irClass: IrClass) {
        val staticInitializerStatements = extractInitializers(irClass) {
            (it is IrField && it.isStatic) || (it is IrAnonymousInitializer && it.isStatic)
        }.toMutableList()
        if (staticInitializerStatements.isNotEmpty()) {
            staticInitializerStatements.sortBy {
                when ((it as? IrSetField)?.symbol?.owner?.origin) {
                    IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY -> 1
                    IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES -> 2
                    IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE -> 3
                    else -> 4
                }
            }
            irClass.addFunction {
                startOffset = irClass.startOffset
                endOffset = irClass.endOffset
                name = clinitName
                // TODO: mark as synthesized
                origin = JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER
                returnType = context.irBuiltIns.unitType
            }.apply {
                body = IrBlockBodyImpl(irClass.startOffset, irClass.endOffset, staticInitializerStatements)
                    .deepCopyWithSymbols(this)
            }
        }
    }

    companion object {
        val clinitName = Name.special("<clinit>")
    }
}
