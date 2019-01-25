/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.irasdescriptors

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.llvmSymbolOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// This file contains some IR utilities which actually use descriptors.
// TODO: port this code to IR.

internal val IrDeclaration.isAnonymousObject get() = DescriptorUtils.isAnonymousObject(this.descriptor)
internal val IrDeclaration.isLocal get() = DescriptorUtils.isLocal(this.descriptor)

internal val IrDeclaration.module get() = this.descriptor.module

@Deprecated("Do not call this method in the compiler front-end.")
internal val IrField.isDelegate get() = @Suppress("DEPRECATION") this.descriptor.isDelegated

internal fun IrFunction.getObjCMethodInfo() = this.descriptor.getObjCMethodInfo()
internal fun IrFunction.getExternalObjCMethodInfo() = this.descriptor.getExternalObjCMethodInfo()
internal fun IrFunction.isObjCClassMethod() = this.descriptor.isObjCClassMethod()

internal fun IrClass.isObjCMetaClass() = this.descriptor.isObjCMetaClass()

internal val IrDeclaration.llvmSymbolOrigin get() = this.descriptor.llvmSymbolOrigin

internal fun IrType.isObjCObjectType() = this.toKotlinType().isObjCObjectType()

