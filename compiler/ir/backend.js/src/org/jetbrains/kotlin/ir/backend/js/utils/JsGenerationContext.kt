/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.js.backend.ast.*

val emptyScope: JsScope
    get() = object : JsScope("nil") {
        override fun doCreateName(ident: String): JsName {
            error("Trying to create name in empty scope")
        }
    }

class JsGenerationContext(
    val currentFunction: IrFunction?,
    val staticContext: JsStaticContext
): IrNamer by staticContext {
    fun newDeclaration(func: IrFunction? = null): JsGenerationContext {
        return JsGenerationContext(
            currentFunction = func,
            staticContext = staticContext
        )
    }

    val continuation
        get() = if (isCoroutineDoResume()) {
            JsThisRef()
        } else {
            if (currentFunction!!.descriptor.isSuspend) {
                JsNameRef(Namer.CONTINUATION)
            } else {
                getNameForValueDeclaration(currentFunction.valueParameters.last()).makeRef()
            }
        }

    private fun isCoroutineDoResume(): Boolean {
        val overriddenSymbols = (currentFunction as? IrSimpleFunction)?.overriddenSymbols ?: return false
        return staticContext.doResumeFunctionSymbol in overriddenSymbols
    }
}