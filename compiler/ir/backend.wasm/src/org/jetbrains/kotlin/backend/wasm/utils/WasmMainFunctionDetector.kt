/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys


class WasmMainFunctionDetector(override val context: WasmBackendContext) : JsMainFunctionDetector(context) {

    private val entryFunctionNamesPerModule =
        context.configuration.get(WasmConfigurationKeys.WASM_ENTRY_FUNCTIONS_PER_MODULE, "")
            .split(",")
            .map { it.split(":") }
            .associate { it[0] to it[1] }

    override fun getMainFunctionOrNull(module: IrModuleFragment): IrSimpleFunction? {

        val moduleName = module.name.asString()
        val definedEntryFunctionName =
            entryFunctionNamesPerModule[moduleName] ?: return super.getMainFunctionOrNull(module)

        if (definedEntryFunctionName == "None") {
            return null
        }

        val candidates = module.files.flatMap { file ->
            val fqn = file.packageFqName.asString()
            if (!definedEntryFunctionName.startsWith("$fqn.")) {
                emptyList()
            } else {
                file.declarations.filterIsInstance<IrSimpleFunction>().filter {
                    it.isMain(
                        allowEmptyParameters = true,
                        definedEntryFunctionName = definedEntryFunctionName.substringAfter("$fqn.")
                    )
                }
            }
        }

        if (candidates.isEmpty()) {
            compilationException(
                "Cannot find symbol of user-defined entry function $definedEntryFunctionName in module $moduleName",
                type = null
            )
        }
        if (candidates.size > 1) {
            compilationException(
                "Found multiple candidates for entry function $definedEntryFunctionName in module $moduleName",
                type = null
            )
        }

        return candidates.first()
    }
}