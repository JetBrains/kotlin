/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.JsStandardClassIds

class JsStaticLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration.parentClassOrNull?.isCompanion != true || !declaration.isJsStaticDeclaration()) return null
        val containingClass = declaration.parentAsClass.parentAsClass

        val proxyDeclaration = when (declaration) {
            is IrSimpleFunction -> declaration.takeIf { it.correspondingPropertySymbol == null }?.generateStaticMethodProxy(containingClass)
            is IrProperty -> declaration.generateStaticPropertyProxy(containingClass)
            else -> error("Unexpected declaration type: ${declaration::class.simpleName}")
        } ?: return null

        containingClass.declarations.add(proxyDeclaration)

        declaration.excludeFromJsExport()

        return null
    }

    private fun IrProperty.generateStaticPropertyProxy(proxyParent: IrClass): IrProperty {
        val originalProperty = this
        return context.irFactory.buildProperty {
            updateFrom(originalProperty)
            name = originalProperty.name
        }.apply {
            copyAnnotationsFrom(originalProperty)
            parent = proxyParent
            getter = originalProperty.getter?.generateStaticMethodProxy(proxyParent)?.also { it.correspondingPropertySymbol = symbol }
            setter = originalProperty.setter?.generateStaticMethodProxy(proxyParent)?.also { it.correspondingPropertySymbol = symbol }
        }
    }

    private fun IrSimpleFunction.generateStaticMethodProxy(proxyParent: IrClass): IrSimpleFunction {
        val originalFun = this
        return context.irFactory.buildFun {
            updateFrom(originalFun)
            name = originalFun.name
            returnType = originalFun.returnType
        }.apply proxy@{
            copyTypeParametersFrom(originalFun)
            copyAnnotationsFrom(originalFun)

            parent = proxyParent
            extensionReceiverParameter = originalFun.extensionReceiverParameter?.copyTo(this)
            valueParameters = originalFun.valueParameters.map { it.copyTo(this) }

            body = context.createIrBuilder(symbol).irBlockBody {
                val delegatingCall = irCall(originalFun).apply {
                    passTypeArgumentsFrom(this@proxy)
                    if (originalFun.dispatchReceiverParameter != null) {
                        dispatchReceiver = irGetObject(originalFun.parentAsClass.symbol)
                    }
                    extensionReceiverParameter?.let { extensionReceiver = irGet(it) }
                    for ((i, valueParameter) in valueParameters.withIndex()) {
                        putValueArgument(i, irGet(valueParameter))
                    }
                }

                +irReturn(delegatingCall)
            }
        }
    }


    private fun IrDeclaration.isJsStaticDeclaration(): Boolean =
        hasAnnotation(JsStandardClassIds.Annotations.JsStatic) ||
                (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.hasAnnotation(JsStandardClassIds.Annotations.JsStatic) == true ||
                (this as? IrProperty)?.getter?.hasAnnotation(JsStandardClassIds.Annotations.JsStatic) == true


    private fun IrDeclaration.excludeFromJsExport() {
        annotations += generateJsExportIgnoreCall()
    }

    private fun generateJsExportIgnoreCall(): IrConstructorCall {
        return JsIrBuilder.buildConstructorCall(context.intrinsics.jsExportIgnoreAnnotationSymbol.owner.primaryConstructor!!.symbol)
    }
}
