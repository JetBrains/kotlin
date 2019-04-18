/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.intrinsics.KClassJavaProperty
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetterCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isKClass
import org.jetbrains.kotlin.ir.util.isNonPrimitiveArray
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.Variance

internal val annotationPhase = makeIrFilePhase(
    ::AnnotationLowering,
    name = "Annotation",
    description = "Remove constructors and modify field types in annotation classes"
)

/**
 * Remove the constructors from annotation classes and change the types of KClass
 * and Array<KClass> fields to use java.lang.Class instead. This phase also rewrites
 * the uses of annotation class fields appropriately.
 */
private class AnnotationLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitClass(irClass: IrClass): IrStatement {
        if (!irClass.isAnnotationClass) return super.visitClass(irClass)

        irClass.declarations.removeIf {
            it is IrConstructor
        }

        for (declaration in irClass.declarations)
            if (declaration is IrSimpleFunction)
                lowerAnnotationField(declaration)

        return irClass
    }

    // Lower the types on annotation class fields (KClass -> Class, Array<KClass> -> Array<Class>)
    private fun lowerAnnotationField(declaration: IrSimpleFunction) {
        val property = declaration.correspondingPropertySymbol?.owner ?: return
        val field = property.backingField ?: return

        val newType = when {
            field.type.isKClass() ->
                javaClassType((field.type as IrSimpleType).arguments)
            field.type.isKClassArray() -> {
                val projection = field.type.singleTypeProjectionOrNull as IrTypeProjection

                javaClassArrayType(
                    projection.variance,
                    (projection.type as IrSimpleType).arguments
                )
            }
            else -> return
        }

        val newField = buildField {
            updateFrom(field)
            name = field.name
            type = newType
        }

        newField.correspondingPropertySymbol = property.symbol
        newField.initializer = field.initializer
        property.backingField = newField
        declaration.returnType = newType
        declaration.body = null
    }

    /**
     * Wrap property accesses to annotation class fields if needed.
     * For example, assume that we have
     *
     *     annotation class Ann(val c: KClass<*>, val ca: Array<KClass<*>>)
     *
     * and a variable `a: Ann`. Then we wrap a call of the form `a.c` in a call
     * to `getOrCreateKotlinClass` while `a.c.java` is reduced to `a.c`. Similarly,
     * a call of the form `a.ca` is wrapped with a call to `getOrCreateKotlinClasses`.
     */
    override fun visitCall(expression: IrCall): IrExpression {
        // Skip the KClass wrapper when it is only used to project out the Class instance
        var wrapIntoKClass = true
        var subject: IrExpression = expression
        if (expression.isGetJava()) {
            subject = expression.extensionReceiver!!
            wrapIntoKClass = false
        }

        // Check for a property access on a KClass or Array<KClass> field of an
        // annotation class instance.
        val receiver = subject as? IrGetterCallImpl
            ?: return super.visitCall(expression)

        val wrapIntoArray = receiver.type.isKClassArray()
        if (!wrapIntoArray && !receiver.type.isKClass())
            return super.visitCall(expression)

        val function = (receiver.symbol.owner as? IrSimpleFunction)
            ?.takeIf { (it.parent as? IrClass)?.isAnnotationClass ?: false }
            ?: return super.visitCall(expression)

        val field = function.correspondingProperty?.backingField
            ?: return super.visitCall(expression)

        // Wrap the property access with a call to getOrCreateKClass(es) and fix the type
        val irBuilder = context.createIrBuilder(function.symbol, expression.startOffset, expression.endOffset)
        val getField = irBuilder.irGet(field.type, receiver.dispatchReceiver!!, receiver.symbol)
        if (!wrapIntoKClass)
            return getField

        return irBuilder.irCall(
            if (wrapIntoArray) context.ir.symbols.getOrCreateKotlinClasses else context.ir.symbols.getOrCreateKotlinClass
        ).apply {
            putValueArgument(0, getField)
        }
    }

    private fun javaClassType(typeArguments: List<IrTypeArgument>): IrType =
        context.ir.symbols.javaLangClass.createType(false, typeArguments)

    private fun javaClassArrayType(variance: Variance, typeArguments: List<IrTypeArgument>): IrType {
        val argument = makeTypeProjection(javaClassType(typeArguments), variance)
        return context.irBuiltIns.arrayClass.createType(false, listOf(argument))
    }

    private fun IrCall.isGetJava(): Boolean =
        context.irIntrinsics.getIntrinsic(descriptor.original) is KClassJavaProperty
}

private fun IrClassSymbol.getFunctionByName(name: String, numParams: Int): IrSimpleFunctionSymbol =
    functions
        .filter { it.owner.name.asString() == name }
        .single { it.owner.valueParameters.size == numParams }

private fun IrType.isKClassArray() =
    isNonPrimitiveArray() && singleTypeProjectionOrNull?.isKClass() ?: false

private val IrType.singleTypeProjectionOrNull: IrType?
    get() = (singleTypeArgumentOrNull as? IrTypeProjection)?.type

private val IrType.singleTypeArgumentOrNull: IrTypeArgument?
    get() = (this as? IrSimpleType)?.arguments?.singleOrNull()
