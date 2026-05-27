/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import org.jetbrains.kotlin.backend.common.fileForTopLevelPluginDeclarations
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isEffectivelyInlineOnly
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

class JvmIrDeclarationOrigin(originKind: JvmDeclarationOriginKind, val declaration: IrDeclaration) : JvmDeclarationOrigin(originKind) {
    override val originalSourceElement: Any
        get() = declaration.attributeOwnerId

    override val generatedForCompilerPlugin: Boolean
        get() = declaration.fileOrNull?.fileForTopLevelPluginDeclarations == true
}

val IrDeclaration.descriptorOrigin: JvmIrDeclarationOrigin
    get() = when {
        origin == IrDeclarationOrigin.FILE_CLASS ->
            JvmIrDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_PART, this)
        (this is IrSimpleFunction && isSuspend && isEffectivelyInlineOnly()) ||
                origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE ||
                origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE ->
            JvmIrDeclarationOrigin(JvmDeclarationOriginKind.INLINE_VERSION_OF_SUSPEND_FUN, this)
        else -> JvmIrDeclarationOrigin(JvmDeclarationOriginKind.OTHER, this)
    }
