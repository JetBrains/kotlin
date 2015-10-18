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
 * Represents a sequence of numbers or characters with a given start value, end value and step.
 * This class is intended to be used in 'for' loops, and the JVM backend suggests efficient
 * bytecode generation for it. Progressions with a step of -1 can be created through the
 * `downTo` method on classes representing primitive types.
 */
@Deprecated("This progression has unclear inclusiveness of end value. Use InclusiveRangeProgression instead.", ReplaceWith("InclusiveRangeProgression<N>"))
public interface Progression<out N : Any> : Iterable<N> {
    /**
     * The start value of the progression.
     */
    public val start: N

    /**
     * The end value of the progression (inclusive).
     */
    public val end: N

    /**
     * The step of the progression.
     */
    public val increment: Number
}


/**
 * Represents an arithmetical progression of numbers or characters with a given start value, end value and step.
 * This class is intended to be used in 'for' loops, and the JVM backend suggests efficient
 * bytecode generation for it. Progressions with a step of -1 can be created through the
 * `downTo` method on classes representing primitive types.
 */
public interface InclusiveRangeProgression<out N : Any> : Progression<N> {
    /**
     * The start value of the progression.
     */
    public override val start: N

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    public override val end: N get() = endInclusive

    /**
     * The end value of the progression (inclusive).
     */
    public val endInclusive: N

    /**
     * The step of the progression.
     */
    public override val increment: Number
}

