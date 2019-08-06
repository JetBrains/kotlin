/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrCallableMethod(
    val owner: Type,
    val valueParameterTypes: List<Type>,
    val invokeOpcode: Int,
    val asmMethod: Method,
    val dispatchReceiverType: Type?,
    val extensionReceiverType: Type?,
    val isInterfaceMethod: Boolean
)
