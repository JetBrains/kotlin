/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute

/**
 * Original element before inlining. Useful only with IR
 * inliner. `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
 * idempotence invariant and can contain a chain of declarations.
 *
 * `null` <=> `this` element wasn't inlined.
 */
var IrElement.originalBeforeInline: IrElement? by irAttribute()

var IrFunction.defaultArgumentsDispatchFunction: IrFunction? by irAttribute()

var IrClass.capturedFields: Collection<IrField>? by irAttribute()

var IrClass.reflectedNameAccessor: IrSimpleFunction? by irAttribute()

/**
 * If this is a `suspend` function, returns its corresponding function with continuations.
 */
var IrSimpleFunction.functionWithContinuations: IrSimpleFunction? by irAttribute()

/**
 * If this is a function with continuation, returns its corresponding `suspend` function.
 */
var IrSimpleFunction.suspendFunction: IrSimpleFunction? by irAttribute()

var IrFunction.defaultArgumentsOriginalFunction: IrFunction? by irAttribute()

var IrConstructor.capturedConstructor: IrConstructor? by irAttribute()
