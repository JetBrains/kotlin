/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.ANNOTATION_IMPLEMENTATION
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationLowering
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isInPublicInlineScope
import org.jetbrains.kotlin.backend.jvm.lower.FunctionReferenceLowering.Companion.javaClassReference
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val annotationImplementationPhase = makeIrFilePhase<JvmBackendContext>(
    { ctxt -> AnnotationImplementationLowering { JvmAnnotationImplementationTransformer(ctxt, it) } },
    name = "AnnotationImplementation",
    description = "Create synthetic annotations implementations and use them in annotations constructor calls"
)

class JvmAnnotationImplementationTransformer(val jvmContext: JvmBackendContext, file: IrFile) :
    AnnotationImplementationTransformer(jvmContext, file) {
    private val publicAnnotationImplementationClasses = mutableSetOf<IrClassSymbol>()

    // FIXME: Copied from JvmSingleAbstractMethodLowering
    private val inInlineFunctionScope: Boolean
        get() = allScopes.any { it.irElement.safeAs<IrDeclaration>()?.isInPublicInlineScope == true }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructedClass = expression.type.classOrNull
        if (constructedClass?.owner?.isAnnotationClass == true && inInlineFunctionScope) {
            publicAnnotationImplementationClasses += constructedClass
        }
        return super.visitConstructorCall(expression)
    }

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
        // Mark the implClass as part of the public ABI if it was instantiated from a public
        // inline function, since annotation implementation classes are regenerated during inlining.
        if (annotationClass.symbol in publicAnnotationImplementationClasses) {
            jvmContext.publicAbiSymbols += implClass.symbol
        }

        implClass.addFunction(
            name = "annotationType",
            returnType = jvmContext.ir.symbols.javaLangClass.starProjectedType,
            origin = ANNOTATION_IMPLEMENTATION,
            isStatic = false
        ).apply {
            body = jvmContext.createJvmIrBuilder(symbol).run {
                irBlockBody {
                    +irReturn(javaClassReference(annotationClass.defaultType))
                }
            }
        }
    }
}
