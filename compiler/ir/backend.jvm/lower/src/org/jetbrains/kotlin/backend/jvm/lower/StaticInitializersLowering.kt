/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.InitializersLoweringBase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name

class StaticInitializersLowering(override val context: JvmBackendContext) : InitializersLoweringBase(context), ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val staticInitializerStatements = extractInitializers(irClass) {
            // JVM implementations are required to generate initializers for all static fields with ConstantValue,
            // so don't add any to <clinit>.
            (it is IrField && it.isStatic && it.constantValue() == null) || (it is IrAnonymousInitializer && it.isStatic)
        }.toMutableList()
        if (staticInitializerStatements.isNotEmpty()) {
            staticInitializerStatements.sortBy {
                when ((it as? IrSetField)?.symbol?.owner?.origin) {
                    JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE -> 1
                    IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY -> 2
                    IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES -> 3
                    IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRIES -> 4
                    IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE -> 5
                    else -> 6
                }
            }
            irClass.addFunction {
                startOffset = irClass.startOffset
                endOffset = irClass.endOffset
                name = clinitName
                // TODO: mark as synthesized
                origin = JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER
                returnType = context.irBuiltIns.unitType
                visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
            }.apply {
                body = context.irFactory.createBlockBody(irClass.startOffset, irClass.endOffset, staticInitializerStatements)
                    .patchDeclarationParents(this)
            }
        }
    }

    companion object {
        val clinitName = Name.special("<clinit>")
    }
}
