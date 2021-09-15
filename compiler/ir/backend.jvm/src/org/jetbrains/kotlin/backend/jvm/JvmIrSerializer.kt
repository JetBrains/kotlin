/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile

interface JvmIrSerializer {
    fun serializeTopLevelIrClass(irClass: IrClass): ByteArray?
    fun serializeIrFile(irFile: IrFile): ByteArray?
}