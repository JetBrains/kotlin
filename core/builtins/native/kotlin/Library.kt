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
 * Returns true if the receiver and the [other] object are the same object instance, or if they
 * are both null.
 */
@Deprecated("This function is deprecated, use === instead", ReplaceWith("this === other"))
public fun Any?.identityEquals(other: Any?): Boolean // = this === other

/**
 * Returns a string representation of the object. Can be called with a null receiver, in which case
 * it returns the string "null".
 */
public fun Any?.toString(): String

/**
 * Concatenates this string with the string representation of the given [other] object. If either the receiver
 * or the [other] object are null, they are represented as the string "null".
 */
public operator fun String?.plus(other: Any?): String

/**
 * Returns an array of objects of the given type with the given [size], initialized with null values.
 */
public fun arrayOfNulls<reified T>(size: Int): Array<T?>
