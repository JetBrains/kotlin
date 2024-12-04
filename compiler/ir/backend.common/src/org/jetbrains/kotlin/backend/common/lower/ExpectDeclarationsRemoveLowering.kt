/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.declarations.*

/**
 * This pass removes all declarations with `isExpect == true`, which usually come as unactualized optional expectations, like
 * - when compiling Native stdlib cache: the following comes from `libraries/stdlib/common/src/kotlin/JsAnnotationsH.kt`
 *      @OptionalExpectation public expect annotation class JsName(val name: String)
 * - when compiling WASM: the following comes from `libraries/stdlib/common/src/kotlin/JsAnnotationsH.kt`
 *      @OptionalExpectation public expect annotation class JsFileName(val name: String)
 * - when compiling JS: the following comes from `libraries/stdlib/common/src/kotlin/JvmAnnotationsH.kt`
 *      @OptionalExpectation public expect annotation class JvmMultifileClass()
 */
class ExpectDeclarationsRemoveLowering(val context: LoweringContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        irFile.declarations.removeAll {
            when (it) {
                is IrClass -> it.isExpect
                is IrFunction -> it.isExpect
                is IrProperty -> it.isExpect
                else -> false
            }
        }
    }
}
