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
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public abstract class Enum<E : Enum<E>>(name: String, ordinal: Int): Comparable<E> {
    companion object {}

    /**
     * Returns the name of this enum constant, exactly as declared in its enum declaration.
     */
    public final val name: String

    /**
     * Returns the ordinal of this enumeration constant (its position in its enum declaration, where the initial constant
     * is assigned an ordinal of zero).
     */
    public final val ordinal: Int

    public override final fun compareTo(other: E): Int

    /**
     * Throws an exception since enum constants cannot be cloned.
     * This method prevents enum classes from inheriting from `Cloneable`.
     */
    protected final fun clone(): Any

    public override final fun equals(other: Any?): Boolean
    public override final fun hashCode(): Int
    public override fun toString(): String

    /**
     * Returns an array containing the constants of this enum type, in the order they're declared.
     * This method may be used to iterate over the constants.
     * @values
     */

    /**
     * Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
     * @throws IllegalArgumentException if this enum type has no constant with the specified name
     * @valueOf
     */
}
