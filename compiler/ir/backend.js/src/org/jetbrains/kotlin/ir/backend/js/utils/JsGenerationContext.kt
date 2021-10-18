/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.backend.ast.JsThisRef

val emptyScope: JsScope
    get() = object : JsScope("nil") {
        override fun doCreateName(ident: String): JsName {
            error("Trying to create name in empty scope")
        }
    }

class JsGenerationContext(
    val currentFile: IrFile,
    val currentFunction: IrFunction?,
    val staticContext: JsStaticContext,
    val localNames: LocalNameGenerator? = null
): IrNamer by staticContext {
    fun newFile(file: IrFile, func: IrFunction? = null, localNames: LocalNameGenerator? = null): JsGenerationContext {
        return JsGenerationContext(
            currentFile = file,
            currentFunction = func,
            staticContext = staticContext,
            localNames = localNames,
        ).also {
            it.nameCache += nameCache
        }
    }

    fun newDeclaration(func: IrFunction? = null, localNames: LocalNameGenerator? = null): JsGenerationContext {
        return JsGenerationContext(
            currentFile = currentFile,
            currentFunction = func,
            staticContext = staticContext,
            localNames = localNames,
        ).also {
            it.nameCache += nameCache
        }
    }

    private val nameCache = mutableMapOf<IrElement, JsName>()

    fun getNameForValueDeclaration(declaration: IrDeclarationWithName): JsName {
        return nameCache.getOrPut(declaration) {
            val name = localNames!!.variableNames.names[declaration]
                ?: error("Variable name is not found ${declaration.name}")
            JsName(name, true)
        }
    }

    fun getNameForLoop(loop: IrLoop): JsName? {
        return nameCache.getOrPut(loop) {
            val name = localNames!!.localLoopNames.names[loop] ?: return null
            JsName(name, true)
        }
    }

    fun getNameForReturnableBlock(block: IrReturnableBlock): JsName? {
        return nameCache.getOrPut(block) {
            val name = localNames!!.localReturnableBlockNames.names[block] ?: return null
            return JsName(name, true)
        }
    }

    fun checkIfJsCode(symbol: IrFunctionSymbol): Boolean = symbol == staticContext.backendContext.intrinsics.jsCode
}
