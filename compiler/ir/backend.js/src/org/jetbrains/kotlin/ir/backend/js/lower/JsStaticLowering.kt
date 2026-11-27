/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.excludeFromJsExport
import org.jetbrains.kotlin.ir.backend.js.utils.isJsStaticDeclaration
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.v2.IrSignatureMapping
import org.jetbrains.kotlin.ir.builders.declarations.v2.IrSimpleFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.builders.declarations.v2.irProperty
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
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

        declaration.excludeFromJsExport(context)

        return if (parentClass.isCompanion) {
            staticScopeOwner.declarations.add(proxyDeclaration)
            null
        } else listOf(declaration, proxyDeclaration)
    }

    private fun IrProperty.generateStaticPropertyProxy(proxyParent: IrClass): IrProperty {
        val originalProperty = this
        return context.irFactory.irProperty {
            updateFrom(originalProperty)
            annotations = originalProperty.copyAnnotations()
            parent = proxyParent
            originalProperty.getter?.let { original -> getter { copyStaticProxySignature(original) } }
            originalProperty.setter?.let { original -> setter { copyStaticProxySignature(original) } }
        }.apply {
            getter?.generateStaticProxyBody(originalProperty.getter!!)
            setter?.generateStaticProxyBody(originalProperty.setter!!)
        }
    }

    private fun IrSimpleFunction.generateStaticMethodProxy(proxyParent: IrClass): IrSimpleFunction {
        val originalFun = this
        val mapping: IrSignatureMapping
        return context.irFactory.irFunction {
            parent = proxyParent
            mapping = copyStaticProxySignature(originalFun)
        }.apply {
            copyDefaultValuesFrom(mapping.parameters, mapping.typeSubstitution)
            generateStaticProxyBody(originalFun)
        }
    }

    /** The header and the signature of [originalFun] without the dispatch receiver: the proxy is static. */
    private fun IrSimpleFunctionBuilder.copyStaticProxySignature(originalFun: IrSimpleFunction): IrSignatureMapping {
        updateFrom(originalFun)
        annotations = originalFun.copyAnnotations()
        val substitution = copyTypeParameters(originalFun.typeParameters)
        returnType = originalFun.returnType.substitute(substitution)
        return IrSignatureMapping(substitution, copyParameters(originalFun.nonDispatchParameters, substitution))
    }

    private fun IrSimpleFunction.generateStaticProxyBody(originalFun: IrSimpleFunction) {
        val proxy = this
        body = runIf(!isExternal) {
            context.createIrBuilder(symbol).irBlockBody {
                val delegatingCall = irCall(originalFun).apply {
                    passTypeArgumentsFrom(proxy)
                    arguments.clear()
                    if (originalFun.dispatchReceiverParameter != null) {
                        arguments.add(irGetObject(originalFun.parentAsClass.symbol))
                    }
                    proxy.parameters.mapTo(arguments) { irGet(it) }
                }

                +irReturn(delegatingCall)
            }
        }
    }
}
