/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.ANNOTATION_IMPLEMENTATION
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationLowering
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.javaClassReference
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.render

internal val annotationImplementationPhase = makeIrFilePhase<JvmBackendContext>(
    { ctxt -> AnnotationImplementationLowering { JvmAnnotationImplementationTransformer(ctxt, it) } },
    name = "AnnotationImplementation",
    description = "Create synthetic annotations implementations and use them in annotations constructor calls"
)

class JvmAnnotationImplementationTransformer(val jvmContext: JvmBackendContext, file: IrFile) :
    AnnotationImplementationTransformer(jvmContext, file) {
    override fun IrType.kClassToJClassIfNeeded(): IrType = when {
        this.isKClass() -> jvmContext.ir.symbols.javaLangClass.starProjectedType
        this.isKClassArray() -> jvmContext.irBuiltIns.arrayClass.typeWith(
            jvmContext.ir.symbols.javaLangClass.starProjectedType
        )
        else -> this
    }

    private fun IrType.isKClassArray() =
        this is IrSimpleType && isArray() && arguments.single().typeOrNull?.isKClass() == true

    override fun IrBuilderWithScope.kClassExprToJClassIfNeeded(irExpression: IrExpression): IrExpression {
        with(this) {
            return irGet(
                jvmContext.ir.symbols.javaLangClass.starProjectedType,
                null,
                jvmContext.ir.symbols.kClassJava.owner.getter!!.symbol
            ).apply {
                extensionReceiver = irExpression
            }
        }
    }

    override fun generatedEquals(irBuilder: IrBlockBodyBuilder, type: IrType, arg1: IrExpression, arg2: IrExpression): IrExpression {
        return if (type.isArray() || type.isPrimitiveArray()) {
            val targetType = if (type.isPrimitiveArray()) type else jvmContext.ir.symbols.arrayOfAnyNType
            val requiredSymbol = jvmContext.ir.symbols.arraysClass.owner.findDeclaration<IrFunction> {
                it.name.asString() == "equals" && it.valueParameters.size == 2 && it.valueParameters.first().type == targetType
            }
            requireNotNull(requiredSymbol) { "Can't find Arrays.equals method for type ${targetType.render()}" }
            irBuilder.irCall(
                requiredSymbol.symbol
            ).apply {
                putValueArgument(0, arg1)
                putValueArgument(1, arg2)
            }
        } else super.generatedEquals(irBuilder, type, arg1, arg2)
    }

    override fun implementPlatformSpecificParts(annotationClass: IrClass, implClass: IrClass) {
        implClass.addFunction(
            name = "annotationType",
            returnType = jvmContext.ir.symbols.javaLangClass.starProjectedType,
            origin = ANNOTATION_IMPLEMENTATION,
            isStatic = false
        ).apply {
            body = jvmContext.createIrBuilder(symbol).irBlockBody {
                +irReturn(javaClassReference(annotationClass.defaultType, jvmContext))
            }
        }
    }
}
