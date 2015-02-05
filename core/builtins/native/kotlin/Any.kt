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
 * The root of the Kotlin class hierarchy. Every Kotlin class has [Any] as a superclass.
 */
public open class Any {
    /**
     * Indicates whether some other object is "equal to" this one. Implementations must follow
     * the same contract as the [Java equals() method](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#equals%28java.lang.Object%29).
     * Note that the `==` operator in Kotlin code is translated into a null-safe call to [equals].
     */
    public open fun equals(other: Any?): Boolean

    /**
     * Returns a hash code value for the object. Implementations must follow the same contract
     * as the [Java hashCode() method](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#hashCode%28%29).
     */
    public open fun hashCode(): Int

    /**
     * Returns a string representation of the object.
     */
    public open fun toString(): String
}
