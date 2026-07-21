/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.render

class JvmDeclarationOrigin(val declaration: IrDeclaration?) {
    override fun toString(): String = declaration?.render().toString()

    companion object {
        @JvmField
        val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(null)
    }
}
