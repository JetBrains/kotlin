/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.util.Printer

class IrCallableMethod(
    val owner: Type,
    val invokeOpcode: Int,
    val signature: JvmMethodSignature,
    val isInterfaceMethod: Boolean,
    val returnType: IrType,
) {
    val asmMethod: Method = signature.asmMethod

    val valueParameterTypes: List<Type> =
        signature.valueParameters.filter { it.kind != JvmMethodParameterKind.RECEIVER }.map { it.asmType }

    override fun toString(): String =
        "${Printer.OPCODES[invokeOpcode]} $owner.$asmMethod" + (if (isInterfaceMethod) " (itf)" else "")
}
