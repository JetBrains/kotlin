/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType

private val EMPTY_TYPE_ARRAY = arrayOfNulls<IrType?>(0)
private val EMPTY_EXPRESSION_ARRAY = arrayOfNulls<IrExpression?>(0)

fun initializeTypeArguments(size: Int) = if (size == 0) EMPTY_TYPE_ARRAY else arrayOfNulls(size)
fun initializeParameterArguments(size: Int) = if (size == 0) EMPTY_EXPRESSION_ARRAY else arrayOfNulls(size)