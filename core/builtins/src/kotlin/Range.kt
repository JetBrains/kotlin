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

package kotlin

/**
 * Represents a range of values (for example, numbers or characters).
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/ranges.html) for more information.
 */
@Deprecated("This range has unclear inclusiveness of end value. Use InclusiveRange instead.", ReplaceWith("InclusiveRange<T>"))
public interface Range<T : Comparable<T>> {
    /**
     * The minimum value in the range.
     */
    public val start: T

    /**
     * The maximum value in the range (inclusive).
     */
    public val end: T

    /**
     * Checks if the specified value belongs to the range.
     */
    public operator fun contains(item: T): Boolean

    /**
     * Checks if the range is empty.
     */
    public fun isEmpty(): Boolean = start > end
}

/**
 * Represents a range of values (for example, numbers or characters).
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/ranges.html) for more information.
 */
public interface InclusiveRange<T: Comparable<T>> : Range<T> {
    /**
     * The minimum value in the range.
     */
    public override val start: T

    /**
     * The maximum value in the range (inclusive).
     */
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    public override val end: T get() = endInclusive

    /**
     * The maximum value in the range (inclusive).
     */
    public val endInclusive: T

    /**
     * Checks if the specified value belongs to the range.
     */
    public override operator fun contains(item: T): Boolean = item >= start && item <= endInclusive

    /**
     * Checks if the range is empty.
     */
    public override fun isEmpty(): Boolean = start > endInclusive
}
