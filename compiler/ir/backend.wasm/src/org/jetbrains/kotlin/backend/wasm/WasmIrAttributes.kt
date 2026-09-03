/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag

var IrFunction.topLevelFunctionForNestedExternal: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrFunction.jsFunctionForExternalAdapterFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrClass.getInstanceFunctionForExternalObject: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrClass.instanceCheckForExternalClass: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrClass.getJsClassForExternalClass: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrCatch.toCatchThrowableOrJsException: Boolean by irFlag(copyByDefault = true)

/**
 * Set on an `IMPLICIT_CAST` which the compiler has proven to always succeed, so that
 * [org.jetbrains.kotlin.backend.wasm.lower.WasmTypeOperatorLowering] lowers it into a plain narrowing
 * (which may still box or unbox) instead of a runtime type check.
 *
 * See [org.jetbrains.kotlin.backend.common.lower.optimizations.ProvenImplicitCastBuilder].
 */
var IrTypeOperatorCall.castIsProvenToSucceed: Boolean by irFlag(copyByDefault = true)
