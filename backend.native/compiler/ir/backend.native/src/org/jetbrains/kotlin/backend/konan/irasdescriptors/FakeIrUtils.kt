/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.irasdescriptors

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.llvmSymbolOrigin
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module

// This file contains some IR utilities which actually use descriptors.
// TODO: port this code to IR.

internal val IrDeclaration.annotations get() = this.descriptor.annotations
internal val IrDeclaration.isAnonymousObject get() = DescriptorUtils.isAnonymousObject(this.descriptor)
internal val IrFunction.isExternal get() = this.descriptor.isExternal
internal val IrDeclaration.isLocal get() = DescriptorUtils.isLocal(this.descriptor)

internal val IrDeclaration.module get() = this.descriptor.module

@Deprecated("Do not call this method in the compiler front-end.")
internal val IrField.isDelegate get() = @Suppress("DEPRECATION") this.descriptor.isDelegated

internal fun IrFunction.getObjCMethodInfo() = this.descriptor.getObjCMethodInfo()
internal fun IrClass.isExternalObjCClass() = this.descriptor.isExternalObjCClass()
internal fun IrClass.isKotlinObjCClass() = this.descriptor.isKotlinObjCClass()
internal fun IrConstructor.getObjCInitMethod() = this.descriptor.getObjCInitMethod()
internal fun IrFunction.getExternalObjCMethodInfo() = this.descriptor.getExternalObjCMethodInfo()
internal fun IrFunction.isObjCClassMethod() = this.descriptor.isObjCClassMethod()
internal fun IrFunction.canObjCClassMethodBeCalledVirtually(overridden: IrFunction) =
        this.descriptor.canObjCClassMethodBeCalledVirtually(overridden.descriptor)
internal fun IrClass.isObjCClass() = this.descriptor.isObjCClass()
internal fun IrClass.isObjCMetaClass() = this.descriptor.isObjCMetaClass()
internal fun IrFunction.isExternalObjCClassMethod() = this.descriptor.isExternalObjCClassMethod()

internal val IrDeclaration.llvmSymbolOrigin get() = this.descriptor.llvmSymbolOrigin

internal fun IrFunction.isMain() = MainFunctionDetector.isMain(this.descriptor)

internal fun IrTypeOperatorCall.getTypeOperandClass(context: Context): IrClass? =
        context.ir.getClass(this.typeOperand)

internal fun IrCatch.getCatchParameterTypeClass(context: Context): IrClass? =
        context.ir.getClass(this.catchParameter.type)
