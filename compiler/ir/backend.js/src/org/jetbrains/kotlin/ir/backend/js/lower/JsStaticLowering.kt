/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isJsStaticDeclaration
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Make for each `@JsStatic` declaration inside the companion object a proxy declaration inside its parent class static scope.
 */
class JsStaticLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val parentClass = declaration.parent as? IrClass ?: return null
        if (!parentClass.isObject || !declaration.isJsStaticDeclaration()) return null
        val staticScopeOwner = if (parentClass.isCompanion) parentClass.parentAsClass else parentClass

        val proxyDeclaration = when (declaration) {
            is IrSimpleFunction -> declaration.takeIf { it.correspondingPropertySymbol == null }?.generateStaticMethodProxy(staticScopeOwner)
            is IrProperty -> declaration.generateStaticPropertyProxy(staticScopeOwner)
            else -> irError("Unexpected declaration type") {
                withIrEntry("declaration", declaration)
            }
        } ?: return null

        declaration.excludeFromJsExport()

        return if (parentClass.isCompanion) {
            staticScopeOwner.declarations.add(proxyDeclaration)
            null
        } else listOf(declaration, proxyDeclaration)
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
        }.apply proxy@{
            copyAnnotationsFrom(originalFun)

            parent = proxyParent

            copyFunctionSignatureFrom(originalFun, returnType = originalFun.returnType)
            parameters = nonDispatchParameters // Drop the dispatch parameter

            body = runIf(!isExternal) {
                context.createIrBuilder(symbol).irBlockBody {
                    val delegatingCall = irCall(originalFun).apply {
                        passTypeArgumentsFrom(this@proxy)
                        arguments.clear()
                        if (originalFun.dispatchReceiverParameter != null) {
                            arguments.add(irGetObject(originalFun.parentAsClass.symbol))
                        }
                        this@proxy.parameters.mapTo(arguments) { irGet(it) }
                    }

                    +irReturn(delegatingCall)
                }
            }
        }
    }


    private fun IrDeclaration.excludeFromJsExport() {
        annotations += generateJsExportIgnoreAnnotation()
    }

    private fun generateJsExportIgnoreAnnotation(): IrAnnotation {
        return JsIrBuilder.buildAnnotation(context.symbols.jsExportIgnoreAnnotationSymbol.owner.primaryConstructor!!.symbol)
    }
}
