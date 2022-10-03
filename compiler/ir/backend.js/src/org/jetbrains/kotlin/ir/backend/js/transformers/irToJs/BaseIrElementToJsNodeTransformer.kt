/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.TODO
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.js.backend.ast.JsNode

interface BaseIrElementToJsNodeTransformer<out R : JsNode, in D> : IrElementVisitor<R, D> {

    val plugins: List<IrElementVisitor<R?, D>>

    override fun visitElement(element: IrElement, data: D): R {
        TODO(element)
    }
}

/**
 * If [visitor] has plugins, tries to transform [this] using the plugins. The first successful transformation will be returned.
 * If there were no successful transformations, or there are no plugins, transforms [this] using the [visitor] itself.
 */
fun <R : JsNode, D> IrElement.acceptWithPlugins(visitor: BaseIrElementToJsNodeTransformer<R, D>, data: D): R {
    visitor.plugins.firstNotNullOfOrNull { accept(it, data) }?.let { return it }
    return accept(visitor, data)
}
