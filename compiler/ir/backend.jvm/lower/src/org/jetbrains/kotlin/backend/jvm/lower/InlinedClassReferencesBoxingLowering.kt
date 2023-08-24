/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.getAttributeOwnerBeforeInline
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class InlinedClassReferencesBoxingLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementVisitorVoid {
    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClassReference(expression: IrClassReference) {
        super.visitClassReference(expression)

        val wasTypeParameterClassRefBeforeInline =
            (expression.getAttributeOwnerBeforeInline() as? IrClassReference)?.classType?.classifierOrNull is IrTypeParameterSymbol

        if (wasTypeParameterClassRefBeforeInline && expression.classType.isPrimitiveType()) {
            // Making primitive type nullable is effectively boxing
            val boxedPrimitive = expression.classType.makeNullable()
            require(boxedPrimitive is IrSimpleType) {
                "Type is expected to be ${IrSimpleType::class.simpleName}: ${boxedPrimitive.render()}"
            }
            expression.classType = boxedPrimitive
            val classReferenceType = expression.type
            require(classReferenceType is IrSimpleType && classReferenceType.isKClass()) {
                "Type of the type reference is expected to be KClass: ${classReferenceType.render()}"
            }
            expression.type = classReferenceType.buildSimpleType {
                arguments = listOf(boxedPrimitive)
            }
        }
    }

}