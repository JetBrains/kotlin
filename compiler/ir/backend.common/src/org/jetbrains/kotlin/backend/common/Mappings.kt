/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute

var IrFunction.defaultArgumentsDispatchFunction: IrFunction? by irAttribute(copyByDefault = false)

var IrClass.capturedFields: Collection<IrField>? by irAttribute(copyByDefault = false)

var IrClass.reflectedNameAccessor: IrSimpleFunction? by irAttribute(copyByDefault = false)

/**
 * If this is a `suspend` function, returns its corresponding function with continuations.
 */
var IrSimpleFunction.functionWithContinuations: IrSimpleFunction? by irAttribute(copyByDefault = false)

/**
 * If this is a function with continuation, returns its corresponding `suspend` function.
 */
var IrSimpleFunction.suspendFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrFunction.defaultArgumentsOriginalFunction: IrFunction? by irAttribute(copyByDefault = false)

var IrConstructor.capturedConstructor: IrConstructor? by irAttribute(copyByDefault = false)
