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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.descriptors.*

internal class ScopeWithIr(val scope: Scope, val irElement: IrElement)

abstract internal class IrElementTransformerVoidWithContext : IrElementTransformerVoid() {

    private val scopeStack = mutableListOf<ScopeWithIr>()

    override final fun visitFile(declaration: IrFile): IrFile {
        scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        val result = visitFileNew(declaration)
        scopeStack.pop()
        return result
    }

    override final fun visitClass(declaration: IrClass): IrStatement {
        scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        val result = visitClassNew(declaration)
        scopeStack.pop()
        return result
    }

    override final fun visitProperty(declaration: IrProperty): IrStatement {
        scopeStack.push(ScopeWithIr(Scope(declaration.descriptor), declaration))
        val result = visitPropertyNew(declaration)
        scopeStack.pop()
        return result
    }

    override final fun visitField(declaration: IrField): IrStatement {
        scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        val result = visitFieldNew(declaration)
        scopeStack.pop()
        return result
    }

    override final fun visitFunction(declaration: IrFunction): IrStatement {
        scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        val result = visitFunctionNew(declaration)
        scopeStack.pop()
        return result
    }

    protected val currentFile get() = scopeStack.lastOrNull { it.irElement is IrFile }!!.irElement as IrFile
    protected val currentClass get() = scopeStack.lastOrNull { it.scope.scopeOwner is ClassDescriptor }
    protected val currentFunction get() = scopeStack.lastOrNull { it.scope.scopeOwner is FunctionDescriptor }
    protected val currentProperty get() = scopeStack.lastOrNull { it.scope.scopeOwner is PropertyDescriptor }
    protected val currentScope get() = scopeStack.peek()
    protected val parentScope get() = if (scopeStack.size < 2) null else scopeStack[scopeStack.size - 2]
    protected val allScopes get() = scopeStack

    fun printScopeStack() {
        scopeStack.forEach { println(it.scope.scopeOwner) }
    }

    open fun visitFileNew(declaration: IrFile): IrFile {
        return super.visitFile(declaration)
    }

    open fun visitClassNew(declaration: IrClass): IrStatement {
        return super.visitClass(declaration)
    }

    open fun visitFunctionNew(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration)
    }

    open fun visitPropertyNew(declaration: IrProperty): IrStatement {
        return super.visitProperty(declaration)
    }

    open fun visitFieldNew(declaration: IrField): IrStatement {
        return super.visitField(declaration)
    }
}

abstract internal class IrElementVisitorVoidWithContext : IrElementVisitorVoid {

    private val scopeStack = mutableListOf<ScopeWithIr>()

    override final fun visitFile(declaration: IrFile) {
        scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        visitFileNew(declaration)
        scopeStack.pop()
    }

    override final fun visitClass(declaration: IrClass) {
        scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        visitClassNew(declaration)
        scopeStack.pop()
    }

    override final fun visitProperty(declaration: IrProperty) {
        scopeStack.push(ScopeWithIr(Scope(declaration.descriptor), declaration))
        visitPropertyNew(declaration)
        scopeStack.pop()
    }

    override final fun visitField(declaration: IrField) {
        val isDelegated = declaration.descriptor.isDelegated
        if (isDelegated) scopeStack.push(ScopeWithIr(Scope(declaration.symbol), declaration))
        visitFieldNew(declaration)
        if (isDelegated) scopeStack.pop()
    }

    override final fun visitFunction(declaration: IrFunction) {
        scopeStack.push(ScopeWithIr(Scope(declaration.descriptor), declaration))
        visitFunctionNew(declaration)
        scopeStack.pop()
    }

    protected val currentFile get() = scopeStack.lastOrNull { it.scope.scopeOwner is PackageFragmentDescriptor }
    protected val currentClass get() = scopeStack.lastOrNull { it.scope.scopeOwner is ClassDescriptor }
    protected val currentFunction get() = scopeStack.lastOrNull { it.scope.scopeOwner is FunctionDescriptor }
    protected val currentProperty get() = scopeStack.lastOrNull { it.scope.scopeOwner is PropertyDescriptor }
    protected val currentScope get() = scopeStack.peek()
    protected val parentScope get() = if (scopeStack.size < 2) null else scopeStack[scopeStack.size - 2]

    fun printScopeStack() {
        scopeStack.forEach { println(it.scope.scopeOwner) }
    }

    open fun visitFileNew(declaration: IrFile) {
        super.visitFile(declaration)
    }

    open fun visitClassNew(declaration: IrClass) {
        super.visitClass(declaration)
    }

    open fun visitFunctionNew(declaration: IrFunction) {
        super.visitFunction(declaration)
    }

    open fun visitPropertyNew(declaration: IrProperty) {
        super.visitProperty(declaration)
    }

    open fun visitFieldNew(declaration: IrField) {
        super.visitField(declaration)
    }
}