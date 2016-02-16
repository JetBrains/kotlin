/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin.ranges

/**
 * An iterator over a progression of values of type `Char`.
 * @property step the number by which the value is incremented on each step.
 */
internal class CharProgressionIterator(first: Char, last: Char, val step: Int) : CharIterator() {
    private var next = first.toInt()
    private val finalElement = last.toInt()
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    override fun hasNext(): Boolean = hasNext

    override fun nextChar(): Char {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += step
        }
        return value.toChar()
    }
}

/**
 * An iterator over a progression of values of type `Int`.
 * @property step the number by which the value is incremented on each step.
 */
internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator() {
    private var next = first
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    override fun hasNext(): Boolean = hasNext

    override fun nextInt(): Int {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }
}

/**
 * An iterator over a progression of values of type `Long`.
 * @property step the number by which the value is incremented on each step.
 */
internal class LongProgressionIterator(first: Long, last: Long, val step: Long) : LongIterator() {
    private var next = first
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last

    override fun hasNext(): Boolean = hasNext

    override fun nextLong(): Long {
        val value = next
        if (value == finalElement) {
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }
}

