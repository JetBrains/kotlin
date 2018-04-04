/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIntrinsicTransformers
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.backend.ast.JsRootScope
import org.jetbrains.kotlin.name.Name


class JsStaticContext(
    private val rootScope: JsRootScope,
    private val globalBlock: JsGlobalBlock,
    private val nameGenerator: NameGenerator,
    backendContext: JsIrBackendContext
) {
    val intrinsics = JsIntrinsicTransformers(backendContext)

    fun getNameForSymbol(irSymbol: IrSymbol) = nameGenerator.getNameForSymbol(irSymbol, rootScope)
}