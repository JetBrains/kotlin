/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.getWasmCoroutineMode
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.addChild

/**
 * Move intrinsics marked with @ExcludedFromCodegen to special excluded files.
 * All references to these declarations must be lowered or treated in a special way in a codegen.
 *
 * Also excludes optional coroutines internal wasm declarations.
 */
class ExcludeDeclarationsFromCodegen(private val context: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        fun isExcluded(declaration: IrDeclaration): Boolean {
            // Annotation can be applied to top-level declarations ...
            if (declaration.hasExcludedFromCodegenAnnotation())
                return true

            // ... or files as a whole
            val parentFile = declaration.parent as? IrFile
            if (parentFile?.hasExcludedFromCodegenAnnotation() == true)
                return true

            // Exclude stack-switching-only declarations when stack switching disabled
            // Exclude state-machine-only declarations when stack switching enabled
            val isStackSwitchingMode = declaration.getWasmCoroutineMode() ?: parentFile?.getWasmCoroutineMode()
            if (isStackSwitchingMode != null) {
                return isStackSwitchingMode != context.wasmCoroutinesStackSwitching
            }

            return false
        }

        for (file in irModule.files) {
            val it = file.declarations.iterator()
            while (it.hasNext()) {
                val d = it.next() as? IrDeclarationWithName ?: continue
                if (isExcluded(d)) {
                    it.remove()
                    // Move to "excluded" package fragment preserving fq-name
                    context.getExcludedPackageFragmentOrCreate(file.packageFqName).addChild(d)
                }
            }
        }
    }
}
