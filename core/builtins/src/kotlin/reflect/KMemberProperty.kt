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

package kotlin.reflect

/**
 * Represents a property declared in a class.
 *
 * @param T the type of the receiver. Must be derived either from a class declaring this property,
 *        or any subclass of that class.
 * @param R the type of the property.
 */
public trait KMemberProperty<T : Any, out R> : KProperty<R> {
    /**
     * Returns the current value of the property.
     *
     * @param receiver the instance owning the property.
     */
    public fun get(receiver: T): R
}

/**
 * Represents a `var` property declared in a class.
 */
public trait KMutableMemberProperty<T : Any, R> : KMemberProperty<T, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver the instance owning the property.
     * @param value the new value to be assigned to this property.
     */
    public fun set(receiver: T, value: R)
}
