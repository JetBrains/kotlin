/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsStatement

/**
 * This extension point allows to customize JavaScript code generation for the JS IR backend.
 */
interface IrToJsCodegenExtension {

    companion object : ProjectExtensionDescriptor<IrToJsCodegenExtension>(
        "org.jetbrains.kotlin.irToJsCodegenExtension",
        IrToJsCodegenExtension::class.java
    )

    /**
     * @param pluginContext Reserved for future use.
     * @see [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrDeclarationToJsTransformer].
     */
    fun createIrDeclarationToJsTransformer(pluginContext: IrToJsCodegenExtensionContext): IrElementVisitor<JsStatement?, JsGenerationContext>? =
        null

    /**
     * @param pluginContext Reserved for future use.
     * @see [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrElementToJsStatementTransformer].
     */
    fun createIrElementToJsStatementTransformer(pluginContext: IrToJsCodegenExtensionContext): IrElementVisitor<JsStatement?, JsGenerationContext>? =
        null

    /**
     * @param pluginContext Reserved for future use.
     * @see [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrElementToJsExpressionTransformer].
     */
    fun createIrElementToJsExpressionTransformer(pluginContext: IrToJsCodegenExtensionContext): IrElementVisitor<JsExpression?, JsGenerationContext>? =
        null

    /**
     * @param pluginContext Reserved for future use.
     * @see [org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrFunctionToJsTransformer].
     */
    fun createIrFunctionTransformer(pluginContext: IrToJsCodegenExtensionContext): IrElementVisitor<JsFunction?, JsGenerationContext>? =
        null
}
