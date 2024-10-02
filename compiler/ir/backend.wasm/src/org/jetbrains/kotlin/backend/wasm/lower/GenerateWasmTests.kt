/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.TestGenerator
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name

/**
 * Generates code to execute kotlin.test cases.
 */
internal class GenerateWasmTests(private val context: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val generator = TestGenerator(context = context)
        irModule.files.forEach { irFile ->
            val testContainerIfAny = generator.createTestContainer(irFile)
            if (testContainerIfAny != null) {
                val declarator = makeTestFunctionDeclarator(irFile, testContainerIfAny)
                context.getFileContext(irFile).testFunctionDeclarator = declarator
            }
        }
    }

    private fun makeTestFunctionDeclarator(irFile: IrFile, containerFunction: IrSimpleFunction): IrSimpleFunction {
        val testFunReference = org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = context.irBuiltIns.kFunctionN(0).defaultType,
            symbol = containerFunction.symbol,
            typeArgumentsCount = 0
        )
        val suitName = irFile.packageFqName.asString().toIrConst(context.irBuiltIns.stringType)

        val testFunctionDeclarator = context.irFactory.stageController.restrictTo(containerFunction) {
            context.irFactory.addFunction(irFile) {
                name = Name.identifier("declare test fun")
                returnType = context.irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                val call = context.createIrBuilder(symbol).irCall(context.wasmSymbols.registerRootSuiteBlock!!.owner).also { call ->
                    call.putValueArgument(0, suitName)
                    call.putValueArgument(1, testFunReference)
                }
                body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(call))
            }
        }
        return testFunctionDeclarator
    }
}