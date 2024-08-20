/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.types.defaultType

// Backed codegen can only handle try/catch in the canonical form.
// The defined for Wasm backend canonical form of try/catch:
// try {
//   ...exprs
// }
// [catch (e: JsException) { // OPTIONAL
//   ...exprs
// }]
// catch (e: Throwable) {
//   ...exprs
// }
// no-finally
internal fun IrTry.isCanonical(context: WasmBackendContext) =
    catches.all { it.catchParameter.type == context.irBuiltIns.throwableType || it.catchParameter.type == context.wasmSymbols.jsRelatedSymbols.jsException.defaultType } &&
    finallyExpression == null

internal val IrClass.isAbstractOrSealed
    get() = modality == Modality.ABSTRACT || modality == Modality.SEALED