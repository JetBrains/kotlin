/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

abstract class IrConst<T> : IrExpression() {
    abstract val kind: IrConstKind<T>
    abstract val value: T

    abstract fun copyWithOffsets(startOffset: Int, endOffset: Int): IrConst<T>
}
