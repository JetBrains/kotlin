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
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

interface JvmIrIntrinsicExtension : IrIntrinsicExtension {
    fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod?

    fun applyPluginDefinedReifiedOperationMarker(
        insn: MethodInsnNode,
        instructions: InsnList,
        type: IrType,
        jvmBackendContext: JvmBackendContext,
    ): Int = -1
}