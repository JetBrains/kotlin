/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.lower.WasmPropertyReferenceLowering.Companion.DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering.Companion.FUNCTION_REFERENCE_IMPL
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering.Companion.LAMBDA_IMPL
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class WasmStaticCallableReferenceLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val irFields = mutableSetOf<IrField>()
        val firstKProperty = irFile.declarations.indexOfFirst { it.origin == DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION }

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid()
                if (declaration.isSyntheticSingleton) {
                    val functionReferenceField = declaration.getOrCreateInstanceField().apply {
                        parent = irFile
                        initializer = context.createIrBuilder(symbol).run {
                            irExprBody(irCall(declaration.primaryConstructor!!))
                        }
                    }
                    irFields.add(functionReferenceField)
                }
                return declaration
            }

            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                val constructedClass = expression.symbol.owner.constructedClass
                if (!constructedClass.isSyntheticSingleton)
                    return super.visitConstructorCall(expression)

                val instanceField = constructedClass.getOrCreateInstanceField()
                return IrGetFieldImpl(expression.startOffset, expression.endOffset, instanceField.symbol, expression.type)
            }
        })

        // Should be placed before KProperty initializations
        if (firstKProperty != -1) {
            irFile.declarations.addAll(firstKProperty, irFields)
        } else {
            irFile.declarations.addAll(irFields)
        }
    }


    private fun IrClass.getOrCreateInstanceField(): IrField = context.mapping.functionToInstanceField.getOrPut(this) {
        val klass = this
        context.irFactory.buildField {
            name = Name.identifier(klass.name.asString() + "_instance")
            type = klass.defaultType.makeNullable()
            isStatic = true
            isFinal = true
            origin = FUNCTION_REFERENCE_SINGLETON_FIELD
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            initializer = null
        }
    }
}

val FUNCTION_REFERENCE_SINGLETON_FIELD by IrDeclarationOriginImpl

fun IrField.isFunctionReferenceInstanceField(): Boolean {
    return origin == FUNCTION_REFERENCE_SINGLETON_FIELD
}

val IrClass.isSyntheticSingleton: Boolean
    get() = (origin == LAMBDA_IMPL || origin == FUNCTION_REFERENCE_IMPL) && primaryConstructor!!.valueParameters.isEmpty()
