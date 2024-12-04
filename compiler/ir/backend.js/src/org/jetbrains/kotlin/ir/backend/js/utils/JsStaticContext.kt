/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIntrinsicTransformers
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrClassModel
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.js.backend.ast.JsCompositeBlock


class JsStaticContext(
    val backendContext: JsIrBackendContext,
    private val irNamer: IrNamer,
    val globalNameScope: NameTable<IrDeclaration>,
    val mode: TranslationMode,
) : IrNamer by irNamer {
    val intrinsics = JsIntrinsicTransformers(backendContext)
    val classModels = mutableMapOf<IrClassSymbol, JsIrClassModel>()

    val initializerBlock = JsCompositeBlock()
    val eagerInitializerBlock = JsCompositeBlock()

    val isPerFile: Boolean get() = mode.granularity === JsGenerationGranularity.PER_FILE
}
