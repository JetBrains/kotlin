/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.getSourceLocation
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.getStartSourceLocation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsScope

val emptyScope: JsScope = object : JsScope("nil") {
    override fun doCreateName(ident: String): JsName {
        error("Trying to create name in empty scope")
    }

    override fun copyOwnNames(other: JsScope?) {
        error("Trying to copy names to empty scope")
    }
}

class JsGenerationContext(
    val currentFile: IrFile,
    val currentFunction: IrFunction?,
    val staticContext: JsStaticContext,
    val localNames: LocalNameGenerator? = null,
    private val nameCache: MutableMap<IrElement, JsName> = hashMapOf(),
    private val useBareParameterNames: Boolean = false,
) : IrNamer by staticContext {
    private val startLocationCache = hashMapOf<Int, JsLocation>()
    private val endLocationCache = hashMapOf<Int, JsLocation>()

    fun newFile(file: IrFile, func: IrFunction? = null, localNames: LocalNameGenerator? = null): JsGenerationContext {
        return JsGenerationContext(
            currentFile = file,
            currentFunction = func,
            staticContext = staticContext,
            localNames = localNames,
            nameCache = nameCache,
            useBareParameterNames = useBareParameterNames,
        )
    }

    fun newDeclaration(func: IrFunction? = null, localNames: LocalNameGenerator? = null): JsGenerationContext {
        return JsGenerationContext(
            currentFile = currentFile,
            currentFunction = func,
            staticContext = staticContext,
            localNames = localNames,
            nameCache = nameCache,
            useBareParameterNames = useBareParameterNames,
        )
    }

    fun getNameForValueDeclaration(declaration: IrDeclarationWithName): JsName {
        return nameCache.getOrPut(declaration) {
            if (useBareParameterNames) {
                JsName(sanitizeName(declaration.name.asString()), true)
            } else {
                val name = localNames!!.variableNames.names[declaration]
                    ?: error("Variable name is not found ${declaration.name}")
                JsName(name, true)
            }
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
            JsName(name, true)
        }
    }

    fun checkIfJsCode(symbol: IrFunctionSymbol): Boolean = symbol == staticContext.backendContext.intrinsics.jsCode

    fun checkIfHasAssociatedJsCode(symbol: IrFunctionSymbol): Boolean = staticContext.backendContext.getJsCodeForFunction(symbol) != null

    fun getStartLocationForIrElement(irElement: IrElement, originalName: String? = null) =
        getLocationForIrElement(irElement, originalName, startLocationCache) { startOffset }

    fun getEndLocationForIrElement(irElement: IrElement, originalName: String? = null) =
        getLocationForIrElement(irElement, originalName, endLocationCache) { endOffset }

    private inline fun getLocationForIrElement(
        irElement: IrElement,
        originalName: String?,
        cache: MutableMap<Int, JsLocation>,
        offsetSelector: IrElement.() -> Int,
    ): JsLocation? = cache.getOrPut(irElement.offsetSelector()) {
        irElement.getSourceLocation(currentFile.fileEntry, offsetSelector) ?: return null
    }.copy(name = originalName)
}
