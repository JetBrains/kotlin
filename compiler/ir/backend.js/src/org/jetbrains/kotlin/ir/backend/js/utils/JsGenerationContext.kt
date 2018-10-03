/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.js.backend.ast.*

class JsGenerationContext {
    fun newDeclaration(scope: JsScope, func: IrFunction? = null): JsGenerationContext {
        return JsGenerationContext(this, if (func != null) JsBlock() else JsGlobalBlock(), scope, func)
    }

    val currentBlock: JsBlock
    val currentScope: JsScope
    val currentFunction: IrFunction?
    val parent: JsGenerationContext?
    val staticContext: JsStaticContext
    private val program: JsProgram

    constructor(rootScope: JsRootScope, backendContext: JsIrBackendContext) {

        this.parent = null
        this.program = rootScope.program
        this.staticContext = JsStaticContext(rootScope, program.globalBlock, SimpleNameGenerator(), backendContext)
        this.currentScope = rootScope
        this.currentBlock = program.globalBlock
        this.currentFunction = null
    }

    constructor(parent: JsGenerationContext, block: JsBlock, scope: JsScope, func: IrFunction?) {
        this.parent = parent
        this.program = parent.program
        this.staticContext = parent.staticContext
        this.currentBlock = block
        this.currentScope = scope
        this.currentFunction = func
    }

    fun getNameForSymbol(symbol: IrSymbol): JsName = staticContext.getNameForSymbol(symbol, this)
    fun getNameForType(type: IrType): JsName = staticContext.getNameForType(type, this)
    fun getNameForLoop(loop: IrLoop): JsName? = staticContext.getNameForLoop(loop, this)

    val continuation
        get() = if (isCoroutineDoResume()) {
            JsThisRef()
        } else {
            if (currentFunction!!.descriptor.isSuspend) {
                JsNameRef(currentScope.declareName(Namer.CONTINUATION))
            } else {
                getNameForSymbol(currentFunction.valueParameters.last().symbol).makeRef()
            }
        }

    private fun isCoroutineDoResume(): Boolean {
        val overriddenSymbols = (currentFunction as? IrSimpleFunction)?.overriddenSymbols ?: return false
        return staticContext.doResumeFunctionSymbol in overriddenSymbols
    }
}