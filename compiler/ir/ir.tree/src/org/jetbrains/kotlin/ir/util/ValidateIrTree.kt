/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

inline fun IrElement.validateTree(crossinline onFailBody: (IrElement, IrElement) -> Unit) {
    accept(object : IrTreeValidationVisitor() {
        override fun onFail(irElement: IrElement, expectedParent: IrElement) {
            onFailBody(irElement, expectedParent)
        }
    }, null)
}

abstract class IrTreeValidationVisitor : IrElementVisitor<Unit, IrElement?> {
    override fun visitElement(element: IrElement, data: IrElement?) {
        if (data != null && element.parent != data) {
            onFail(element, data)
        }
        element.acceptChildren(this, element)
    }

    protected abstract fun onFail(irElement: IrElement, expectedParent: IrElement)
}

