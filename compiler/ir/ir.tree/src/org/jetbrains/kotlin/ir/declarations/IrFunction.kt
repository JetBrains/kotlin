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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrBody : IrElement

interface IrFunction : IrDeclarationNonRoot {
    override val descriptor: FunctionDescriptor
    val body: IrBody
}

abstract class IrFunctionBase(
        sourceLocation: SourceLocation,
        containingDeclaration: IrDeclaration
) : IrDeclarationNonRootBase(sourceLocation, containingDeclaration), IrFunction {
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body.accept(visitor, data)
    }
}

class IrFunctionImpl(
        sourceLocation: SourceLocation,
        containingDeclaration: IrDeclaration,
        override val descriptor: FunctionDescriptor,
        override val body: IrBody
) : IrFunctionBase(sourceLocation, containingDeclaration) {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitFunction(this, data)
}

