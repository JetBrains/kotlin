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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrAnonymousInitializer : IrDeclaration {
    override val descriptor: ClassDescriptor // TODO special descriptor for anonymous initializer blocks

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.ANONYMOUS_INITIALIZER

    var body: IrBlockBody
}

class IrAnonymousInitializerImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: ClassDescriptor
) : IrDeclarationBase(startOffset, endOffset, origin), IrAnonymousInitializer {
    private var bodyImpl: IrBlockBody? = null
    override var body: IrBlockBody
        get() = bodyImpl!!
        set(value) {
            value.assertDetached()
            bodyImpl?.detach()
            bodyImpl = value
            value.setTreeLocation(this, ANONYMOUS_INITIALIZER_BODY_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                ANONYMOUS_INITIALIZER_BODY_SLOT -> body
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ANONYMOUS_INITIALIZER_BODY_SLOT -> body = newChild.assertCast()
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitAnonymousInitializer(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body.accept(visitor, data)
    }
}