/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

// Auto-generated file. DO NOT EDIT!

package kotlin

import kotlin.internal.getProgressionFinalElement

/**
 * An iterator over a progression of values of type `Byte`.
 * @property increment the number by which the value is incremented on each step.
 */
internal class ByteProgressionIterator(start: Byte, end: Byte, val increment: Int) : ByteIterator() {
    private var next = start.toInt()
    private val finalElement: Byte = getProgressionFinalElement(start.toInt(), end.toInt(), increment).toByte()
    private var hasNext: Boolean = if (increment > 0) start <= end else start >= end

    override fun hasNext(): Boolean = hasNext

    override fun nextByte(): Byte {
        val value = next
        if (value == finalElement.toInt()) {
            hasNext = false
        }
        else {
            next += increment
        }
        return value.toByte()
    }
}

/**
 * An iterator over a progression of values of type `Char`.
 * @property increment the number by which the value is incremented on each step.
 */
internal class CharProgressionIterator(start: Char, end: Char, val increment: Int) : CharIterator() {
    private var next = start.toInt()
    private val finalElement: Char = getProgressionFinalElement(start.toInt(), end.toInt(), increment).toChar()
    private var hasNext: Boolean = if (increment > 0) start <= end else start >= end

    override fun hasNext(): Boolean = hasNext

    override fun nextChar(): Char {
        val value = next
        if (value == finalElement.toInt()) {
            hasNext = false
        }
        else {
            next += increment
        }
        return value.toChar()
    }
}

/**
 * An iterator over a progression of values of type `Short`.
 * @property increment the number by which the value is incremented on each step.
 */
internal class ShortProgressionIterator(start: Short, end: Short, val increment: Int) : ShortIterator() {
    private var next = start.toInt()
    private val finalElement: Short = getProgressionFinalElement(start.toInt(), end.toInt(), increment).toShort()
    private var hasNext: Boolean = if (increment > 0) start <= end else start >= end

    override fun hasNext(): Boolean = hasNext

    override fun nextShort(): Short {
        val value = next
        if (value == finalElement.toInt()) {
            hasNext = false
        }
        else {
            next += increment
        }
        return value.toShort()
    }
}

/**
 * An iterator over a progression of values of type `Int`.
 * @property increment the number by which the value is incremented on each step.
 */
internal class IntProgressionIterator(start: Int, end: Int, val increment: Int) : IntIterator() {
    private var next = start
    private val finalElement: Int = getProgressionFinalElement(start, end, increment)
    private var hasNext: Boolean = if (increment > 0) start <= end else start >= end

    override fun hasNext(): Boolean = hasNext

    override fun nextInt(): Int {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += increment
        }
        return value
    }
}

/**
 * An iterator over a progression of values of type `Long`.
 * @property increment the number by which the value is incremented on each step.
 */
internal class LongProgressionIterator(start: Long, end: Long, val increment: Long) : LongIterator() {
    private var next = start
    private val finalElement: Long = getProgressionFinalElement(start, end, increment)
    private var hasNext: Boolean = if (increment > 0) start <= end else start >= end

    override fun hasNext(): Boolean = hasNext

    override fun nextLong(): Long {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += increment
        }
        return value
    }
}

/**
 * An iterator over a progression of values of type `Float`.
 * @property increment the number by which the value is incremented on each step.
 */
@Deprecated("This progression implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)
internal class FloatProgressionIterator(start: Float, val end: Float, val increment: Float) : FloatIterator() {
    private var next = start

    override fun hasNext(): Boolean = if (increment > 0) next <= end else next >= end

    override fun nextFloat(): Float {
        val value = next
        next += increment
        return value
    }
}

/**
 * An iterator over a progression of values of type `Double`.
 * @property increment the number by which the value is incremented on each step.
 */
@Deprecated("This progression implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)
internal class DoubleProgressionIterator(start: Double, val end: Double, val increment: Double) : DoubleIterator() {
    private var next = start

    override fun hasNext(): Boolean = if (increment > 0) next <= end else next >= end

    override fun nextDouble(): Double {
        val value = next
        next += increment
        return value
    }
}

