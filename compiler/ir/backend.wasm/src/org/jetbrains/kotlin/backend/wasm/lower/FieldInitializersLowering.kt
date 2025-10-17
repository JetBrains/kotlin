/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Move initialization of global fields to start function.
 *
 * WebAssembly allows only constant expressions to be used directly in
 * field initializers.
 */
class FieldInitializersLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        var nonConstantFieldInitializer: IrSimpleFunction? = null
        var objectInstanceFieldInitializer: IrSimpleFunction? = null

        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            private fun createInitializerFunction(): IrSimpleFunction {
                val newFunction = context.irFactory.createSimpleFunction(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = JsIrBuilder.SYNTHESIZED_DECLARATION,
                    name = Name.identifier("fieldInitializer"),
                    visibility = DescriptorVisibilities.PRIVATE,
                    isInline = false,
                    isExpect = false,
                    returnType = context.irBuiltIns.unitType,
                    modality = Modality.FINAL,
                    symbol = IrSimpleFunctionSymbolImpl(),
                    isTailrec = false,
                    isSuspend = false,
                    isOperator = false,
                    isInfix = false
                )
                newFunction.parent = irFile
                newFunction.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                return newFunction
            }

            override fun visitField(declaration: IrField) {
                super.visitField(declaration)

                // External properties can be "initialized" with `= defineExternally`. Ignoring it.
                if (declaration.isExternal) return
                if (!declaration.isStatic) return

                val initializer = declaration.initializer?.expression ?: return
                if (initializer is IrConst && initializer.kind !is IrConstKind.String) return

                val initializeFunction = when {
                    declaration.isObjectInstanceField() -> objectInstanceFieldInitializer
                    else -> nonConstantFieldInitializer
                }

                val currentFunction = initializeFunction ?: run {
                    context.irFactory.stageController.restrictTo(declaration) {
                        createInitializerFunction()
                    }.also {
                        when {
                            declaration.isObjectInstanceField() -> objectInstanceFieldInitializer = it
                            else -> nonConstantFieldInitializer = it
                        }
                    }
                }

                val initializerStatement = context
                    .createIrBuilder(currentFunction.symbol)
                    .at(initializer)
                    .irSetField(null, declaration, initializer)

                (currentFunction.body as IrBlockBody).statements.add(initializerStatement)

                // Replace initializer with default one
                declaration.initializer = null
            }
        })

        if (objectInstanceFieldInitializer != null || nonConstantFieldInitializer != null) {
            with(context.getFileContext(irFile)) {
                this.objectInstanceFieldInitializer = objectInstanceFieldInitializer?.also { irFile.declarations.add(it) }
                this.nonConstantFieldInitializer = nonConstantFieldInitializer?.also { irFile.declarations.add(it) }
            }
        }
    }
}