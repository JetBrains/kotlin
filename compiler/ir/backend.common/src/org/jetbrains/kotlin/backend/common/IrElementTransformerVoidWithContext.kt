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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

open class ScopeWithIr(val scope: Scope, val irElement: IrElement)

abstract class IrElementTransformerVoidWithContext : IrElementTransformerVoid() {

    private val scopeStack = mutableListOf<ScopeWithIr>()

    protected open fun createScope(declaration: IrSymbolOwner): ScopeWithIr =
        ScopeWithIr(Scope(declaration.symbol), declaration)

    protected fun unsafeEnterScope(declaration: IrSymbolOwner) {
        scopeStack.push(createScope(declaration))
    }

    protected fun unsafeLeaveScope() {
        scopeStack.pop()
    }

    protected inline fun <T> withinScope(declaration: IrSymbolOwner, fn: () -> T): T {
        unsafeEnterScope(declaration)
        val result = fn()
        unsafeLeaveScope()
        return result
    }

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
