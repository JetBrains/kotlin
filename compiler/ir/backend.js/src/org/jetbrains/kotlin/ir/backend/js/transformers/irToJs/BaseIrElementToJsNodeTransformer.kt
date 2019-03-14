/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.TODO
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.js.backend.ast.JsNode

interface BaseIrElementToJsNodeTransformer<out R : JsNode, in D> : IrElementVisitor<R, D> {
    override fun visitElement(element: IrElement, data: D): R {
        TODO(element)
    }
}
