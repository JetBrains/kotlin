/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.ir.wrapWithLambdaCall
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.TestGenerator
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.Name

/**
 * Generates code to execute kotlin.test cases.
 */
internal class GenerateWasmTests(private val backendContext: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val generator = TestGenerator(context = backendContext)
        irModule.files.forEach { irFile ->
            val testContainerIfAny = generator.createTestContainer(irFile)
            if (testContainerIfAny != null) {
                val declarator = backendContext.irFactory.stageController.restrictTo(testContainerIfAny) {
                    makeTestFunctionDeclarator(irFile, testContainerIfAny)
                }
                backendContext.getFileContext(irFile).testFunctionDeclarator = declarator
            }
        }
    }

    private fun makeTestFunctionDeclarator(irFile: IrFile, containerFunction: IrSimpleFunction): IrSimpleFunction {
        val declarator = backendContext.irFactory.addFunction(irFile) {
            name = Name.identifier("declare test fun")
            returnType = backendContext.irBuiltIns.unitType
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }

        val referenceToContainer = backendContext.irFactory.stageController.restrictTo(declarator) {
            containerFunction.wrapWithLambdaCall(declarator, backendContext)
        }

        declarator.body = backendContext.createIrBuilder(declarator.symbol).irBlockBody {
            +irCall(backendContext.wasmSymbols.registerRootSuiteBlock!!.owner).also { call ->
                call.arguments[0] = irString(irFile.packageFqName.asString())
                call.arguments[1] = referenceToContainer
            }
        }

        return declarator
    }
}