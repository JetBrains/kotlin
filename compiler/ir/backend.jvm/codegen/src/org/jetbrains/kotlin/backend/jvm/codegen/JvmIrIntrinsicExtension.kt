/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList

interface JvmIrIntrinsicExtension : IrIntrinsicExtension {
    fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod?

    /**
     * Should return `true` if marker was processed.
     * If this method returns `false`, a regular `TYPE_OF` intrinsic would be inserted.
     */
    fun rewritePluginDefinedOperationMarker(
        v: InstructionAdapter,
        next: AbstractInsnNode,
        instructions: InsnList,
        type: IrType,
        jvmBackendContext: JvmBackendContext
    ): Boolean
}