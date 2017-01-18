/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:JvmName("KProperties")
package kotlin.reflect.full

import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2
import kotlin.reflect.jvm.internal.KPropertyImpl

/**
 * Returns the instance of a delegated **extension property**, or `null` if this property is not delegated.
 * Throws an exception if this is not an extension property.
 *
 * @see [KProperty1.getDelegate]
 */
@SinceKotlin("1.1")
fun KProperty1<*, *>.getExtensionDelegate(): Any? {
    @Suppress("UNCHECKED_CAST")
    return (this as KProperty1<Any?, *>).getDelegate(KPropertyImpl.EXTENSION_PROPERTY_DELEGATE)
}

/**
 * Returns the instance of a delegated **member extension property**, or `null` if this property is not delegated.
 * Throws an exception if this is not an extension property.
 *
 * @param receiver the instance of the class used to retrieve the value of the property delegate.
 *
 * @see [KProperty2.getDelegate]
 */
@SinceKotlin("1.1")
fun <D> KProperty2<D, *, *>.getExtensionDelegate(receiver: D): Any? {
    @Suppress("UNCHECKED_CAST")
    return (this as KProperty2<D, Any?, *>).getDelegate(receiver, KPropertyImpl.EXTENSION_PROPERTY_DELEGATE)
}
