/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIntrinsicTransformers
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.js.backend.ast.JsClassModel
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsRootScope


class JsStaticContext(
    val rootScope: JsRootScope,
    val backendContext: JsIrBackendContext,
    private val irNamer: IrNamer
) : IrNamer by irNamer {

    val intrinsics = JsIntrinsicTransformers(backendContext)
    // TODO: use IrSymbol instead of JsName
    val classModels = mutableMapOf<JsName, JsClassModel>()
    val coroutineImplDeclaration = backendContext.ir.symbols.coroutineImpl.owner
    val doResumeFunctionSymbol = coroutineImplDeclaration.declarations
        .filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "doResume" }.symbol

    val initializerBlock = JsGlobalBlock()
}