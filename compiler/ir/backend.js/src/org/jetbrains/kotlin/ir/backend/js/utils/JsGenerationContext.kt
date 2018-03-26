/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrElementToJsExpressionTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrElementToJsStatementTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.toJsName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.Name

class JsGenerationContext {
    fun newDeclaration(scope: JsScope): JsGenerationContext {
        return JsGenerationContext(this, JsBlock(), scope)
    }

    val currentBlock: JsBlock
    val currentScope: JsScope
    val parent: JsGenerationContext?
    val staticContext: JsStaticContext
    private val program: JsProgram

    constructor(rootScope: JsRootScope) {

        this.parent = null
        this.program = rootScope.program
        this.staticContext = JsStaticContext(rootScope, program.globalBlock, SimpleNameGenerator())
        this.currentScope = rootScope
        this.currentBlock = program.globalBlock
    }

    constructor(parent: JsGenerationContext, block: JsBlock, scope: JsScope) {
        this.parent = parent
        this.program = parent.program
        this.staticContext = parent.staticContext
        this.currentBlock = block
        this.currentScope = scope
    }

    fun getNameForSymbol(symbol: IrSymbol): JsName = staticContext.getNameForSymbol(symbol)
    fun getSpecialRefForName(name: Name): JsExpression = staticContext.getSpecialRefForName(name)
}