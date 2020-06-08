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

import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

open class ScopeWithIr(val scope: Scope, val irElement: IrElement)

abstract class IrElementTransformerVoidWithContext : IrElementTransformerVoid() {

    private val scopeStack = mutableListOf<ScopeWithIr>()

    protected open fun createScope(declaration: IrSymbolOwner): ScopeWithIr =
        ScopeWithIr(Scope(declaration.symbol), declaration)

    final override fun visitFile(declaration: IrFile): IrFile {
        scopeStack.push(createScope(declaration))
        val result = visitFileNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitClass(declaration: IrClass): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitClassNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitProperty(declaration: IrProperty): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitPropertyNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitField(declaration: IrField): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitFieldNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitFunction(declaration: IrFunction): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitFunctionNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitAnonymousInitializerNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitValueParameterNew(declaration)
        scopeStack.pop()
        return result
    }

    final override fun visitScript(declaration: IrScript): IrStatement {
        scopeStack.push(createScope(declaration))
        val result = visitScriptNew(declaration)
        scopeStack.pop()
        return result
    }

    protected val currentFile get() = scopeStack.lastOrNull { it.irElement is IrFile }!!.irElement as IrFile
    protected val currentScript get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrScriptSymbol }
    protected val currentClass get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrClassSymbol }
    protected val currentFunction get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrFunctionSymbol }
    protected val currentProperty get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrPropertySymbol }
    protected val currentAnonymousInitializer get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrAnonymousInitializer }
    protected val currentValueParameter get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrValueParameter }
    protected val currentScope get() = scopeStack.peek()
    protected val parentScope get() = if (scopeStack.size < 2) null else scopeStack[scopeStack.size - 2]
    protected val allScopes get() = scopeStack
    protected val currentDeclarationParent get() = scopeStack.lastOrNull { it.irElement is IrDeclarationParent }?.irElement as? IrDeclarationParent

    @DescriptorBasedIr
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

    open fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer): IrStatement {
        return super.visitAnonymousInitializer(declaration)
    }

    open fun visitValueParameterNew(declaration: IrValueParameter): IrStatement {
        return super.visitValueParameter(declaration)
    }

    open fun visitScriptNew(declaration: IrScript): IrStatement {
        return super.visitScript(declaration)
    }
}

@OptIn(DescriptorBasedIr::class)
abstract class IrElementVisitorVoidWithContext : IrElementVisitorVoid {

    private val scopeStack = mutableListOf<ScopeWithIr>()

    protected open fun createScope(declaration: IrSymbolOwner): ScopeWithIr =
        ScopeWithIr(Scope(declaration.symbol), declaration)

    final override fun visitFile(declaration: IrFile) {
        scopeStack.push(createScope(declaration))
        visitFileNew(declaration)
        scopeStack.pop()
    }

    final override fun visitClass(declaration: IrClass) {
        scopeStack.push(createScope(declaration))
        visitClassNew(declaration)
        scopeStack.pop()
    }

    final override fun visitProperty(declaration: IrProperty) {
        scopeStack.push(createScope(declaration))
        visitPropertyNew(declaration)
        scopeStack.pop()
    }

    final override fun visitField(declaration: IrField) {
        @Suppress("DEPRECATION") val isDelegated = declaration.descriptor.isDelegated
        if (isDelegated) scopeStack.push(createScope(declaration))
        visitFieldNew(declaration)
        if (isDelegated) scopeStack.pop()
    }

    final override fun visitFunction(declaration: IrFunction) {
        scopeStack.push(createScope(declaration))
        visitFunctionNew(declaration)
        scopeStack.pop()
    }

    final override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        scopeStack.push(createScope(declaration))
        visitAnonymousInitializerNew(declaration)
        scopeStack.pop()
    }

    final override fun visitValueParameter(declaration: IrValueParameter) {
        scopeStack.push(createScope(declaration))
        visitValueParameterNew(declaration)
        scopeStack.pop()
    }

    protected val currentFile get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrFileSymbol }
    protected val currentClass get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrClassSymbol }
    protected val currentFunction get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrFunctionSymbol }
    protected val currentProperty get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrPropertySymbol }
    protected val currentAnonymousInitializer get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrAnonymousInitializer }
    protected val currentValueParameter get() = scopeStack.lastOrNull { it.scope.scopeOwnerSymbol is IrValueParameter }
    protected val currentScope get() = scopeStack.peek()
    protected val parentScope get() = if (scopeStack.size < 2) null else scopeStack[scopeStack.size - 2]
    protected val allScopes get() = scopeStack

    @DescriptorBasedIr
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

    open fun visitAnonymousInitializerNew(declaration: IrAnonymousInitializer) {
        super.visitAnonymousInitializer(declaration)
    }

    open fun visitValueParameterNew(declaration: IrValueParameter) {
        super.visitValueParameter(declaration)
    }
}