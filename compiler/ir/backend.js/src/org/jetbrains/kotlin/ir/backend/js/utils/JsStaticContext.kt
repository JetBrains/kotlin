/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIntrinsicTransformers
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrClassModel
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.config.JSConfigurationKeys


class JsStaticContext(
    val backendContext: JsIrBackendContext,
    private val irNamer: IrNamer,
    val globalNameScope: NameTable<IrDeclaration>,
) : IrNamer by irNamer {
    val intrinsics = JsIntrinsicTransformers(backendContext)
    val classModels = mutableMapOf<IrClassSymbol, JsIrClassModel>()
    val coroutineImplDeclaration = backendContext.ir.symbols.coroutineImpl.owner

    val initializerBlock = JsGlobalBlock()

    val genSourcemaps = backendContext.configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)
}
