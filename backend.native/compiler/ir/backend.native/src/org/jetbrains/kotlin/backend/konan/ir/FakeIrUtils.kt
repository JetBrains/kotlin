/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.llvmSymbolOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.file

// This file contains some IR utilities which actually use descriptors.
// TODO: port this code to IR.

internal val IrDeclaration.llvmSymbolOrigin get() = this.file.packageFragmentDescriptor.llvmSymbolOrigin

internal fun IrType.isObjCObjectType() = this.toKotlinType().isObjCObjectType()

