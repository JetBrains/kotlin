/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.constant

abstract class AnnotationArgumentVisitor<R, D> {
    abstract fun visitLongValue(value: LongValue, data: D): R
    abstract fun visitIntValue(value: IntValue, data: D): R
    abstract fun visitErrorValue(value: ErrorValue, data: D): R
    abstract fun visitShortValue(value: ShortValue, data: D): R
    abstract fun visitByteValue(value: ByteValue, data: D): R
    abstract fun visitDoubleValue(value: DoubleValue, data: D): R
    abstract fun visitFloatValue(value: FloatValue, data: D): R
    abstract fun visitBooleanValue(value: BooleanValue, data: D): R
    abstract fun visitCharValue(value: CharValue, data: D): R
    abstract fun visitStringValue(value: StringValue, data: D): R
    abstract fun visitNullValue(value: NullValue, data: D): R
    abstract fun visitEnumValue(value: EnumValue, data: D): R
    abstract fun visitArrayValue(value: ArrayValue, data: D): R
    abstract fun visitAnnotationValue(value: AnnotationValue, data: D): R
    abstract fun visitKClassValue(value: KClassValue, data: D): R
    abstract fun visitUByteValue(value: UByteValue, data: D): R
    abstract fun visitUShortValue(value: UShortValue, data: D): R
    abstract fun visitUIntValue(value: UIntValue, data: D): R
    abstract fun visitULongValue(value: ULongValue, data: D): R
}
