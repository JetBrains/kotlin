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
 * Represents an extension property declared in a class.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/extensions.html#extension-properties)
 * for more information.
 *
 * @param T the type of the instance which should be used to obtain the value of the property.
 *          Must be derived either from a class declaring this property, or any subclass of that class.
 * @param E the type of the extension receiver.
 * @param R the type of the property.
 */
public interface KMemberExtensionProperty<T : Any, E, out R> : KProperty<R> {
    /**
     * Returns the current value of the property.
     *
     * @param instance the instance to obtain the value of the property from.
     * @param extensionReceiver the instance of the extension receiver.
     */
    public fun get(instance: T, extensionReceiver: E): R
}

/**
 * Represents a `var` extension property declared in a class.
 */
public interface KMutableMemberExtensionProperty<T : Any, E, R> : KMemberExtensionProperty<T, E, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param instance the instance to obtain the value of the property from.
     * @param extensionReceiver the instance of the extension receiver.
     * @param value the new value to be assigned to this property.
     */
    public fun set(instance: T, extensionReceiver: E, value: R)
}
