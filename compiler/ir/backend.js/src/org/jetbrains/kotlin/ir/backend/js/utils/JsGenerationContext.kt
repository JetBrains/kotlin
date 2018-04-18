/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.*

class JsGenerationContext {
    fun newDeclaration(scope: JsScope, func: IrFunction? = null, file: IrFile? = null): JsGenerationContext {
        return JsGenerationContext(this, JsBlock(), scope, func, file)
    }

    val currentBlock: JsBlock
    val currentScope: JsScope
    val currentFile: IrFile?
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
        this.currentFile = null
    }

    constructor(parent: JsGenerationContext, block: JsBlock, scope: JsScope, func: IrFunction?, file: IrFile? = null) {
        this.parent = parent
        this.program = parent.program
        this.staticContext = parent.staticContext
        this.currentBlock = block
        this.currentScope = scope
        this.currentFunction = func
        this.currentFile = file ?: parent.currentFile
    }

    fun getNameForSymbol(symbol: IrSymbol): JsName = staticContext.getNameForSymbol(symbol, this)

    val currentPackage: PackageFragmentDescriptor
        get() = currentFile!!.packageFragmentDescriptor
}