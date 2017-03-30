/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

abstract class IrElementTransformerVoidWithContext(): IrElementTransformerVoid() {
    private fun <E> MutableList<E>.push(element: E) = this.add(element)

    private fun <E> MutableList<E>.pop() = this.removeAt(size - 1)

    private fun <E> MutableList<E>.peek(): E? = if (size == 0) null else this[size - 1]

    private val functionsStack = mutableListOf<FunctionDescriptor>()
    private val classesStack = mutableListOf<ClassDescriptor>()

    override final fun visitFunction(declaration: IrFunction): IrStatement {
        functionsStack.push(declaration.descriptor)
        val result = visitFunctionNew(declaration)
        functionsStack.pop()
        return result
    }

    override final fun visitClass(declaration: IrClass): IrStatement {
        classesStack.push(declaration.descriptor)
        val result = visitClassNew(declaration)
        classesStack.pop()
        return result
    }

    protected val currentFunction get() = functionsStack.peek()
    protected val currentClass get() = classesStack.peek()

    open fun visitFunctionNew(declaration: IrFunction) : IrStatement {
        return super.visitFunction(declaration)
    }

    open fun visitClassNew(declaration: IrClass) : IrStatement {
        return super.visitClass(declaration)
    }
}