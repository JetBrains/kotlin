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
 * Represents a property without any kind of receiver.
 * Such property is either originally declared in a receiverless context such as a package,
 * or has the receiver bound to it.
 */
public interface KVariable<out R> : KProperty<R> {
    /**
     * Returns the current value of the variable.
     */
    public fun get(): R
}

/**
 * Represents a variable declared as a `var`.
 */
public interface KMutableVariable<R> : KVariable<R>, KMutableProperty<R> {
    /**
     * Modifies the value of the variable.
     *
     * @param value the new value to be assigned to this variable.
     */
    public fun set(value: R)
}
