/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.resolve.jvm.diagnostics

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.OTHER

open class JvmDeclarationOrigin(val originKind: JvmDeclarationOriginKind, val declaration: IrDeclaration?) {
    open val originalSourceElement: Any?
        get() = declaration?.attributeOwnerId

    override fun toString(): String = when (this) {
        NO_ORIGIN -> "NO_ORIGIN"
        NO_ORIGIN_SUSPEND_FOR_INLINE -> "NO_ORIGIN_SUSPEND_FOR_INLINE"
        else -> "origin=$originKind"
    }

    companion object {
        @JvmField
        val NO_ORIGIN: JvmDeclarationOrigin = JvmDeclarationOrigin(OTHER, null)

        @JvmField
        val NO_ORIGIN_SUSPEND_FOR_INLINE: JvmDeclarationOrigin =
            JvmDeclarationOrigin(JvmDeclarationOriginKind.INLINE_VERSION_OF_SUSPEND_FUN, null)
    }
}
