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

package kotlin.reflect.full

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

/**
 * An exception that is thrown when `call` is invoked on a callable or `get` or `set` is invoked on a property
 * and that callable is not accessible (in JVM terms) from the calling method.
 *
 * @param cause the original exception thrown by the JVM.
 *
 * @see [kotlin.reflect.jvm.isAccessible]
 */
@SinceKotlin("1.1")
class IllegalCallableAccessException(cause: IllegalAccessException) : Exception(cause)

/**
 * An exception that is thrown when `getDelegate` is invoked on a [KProperty] object that was not made accessible
 * with [isAccessible].
 *
 * @see [kotlin.reflect.KProperty0.getDelegate]
 * @see [kotlin.reflect.KProperty1.getDelegate]
 * @see [kotlin.reflect.KProperty2.getDelegate]
 * @see [kotlin.reflect.jvm.isAccessible]
 */
@SinceKotlin("1.1")
class IllegalPropertyDelegateAccessException(cause: IllegalAccessException) : Exception(
        "Cannot obtain the delegate of a non-accessible property. Use \"isAccessible = true\" to make the property accessible",
        cause
)

/**
 * An exception that is thrown when the code tries to introspect a property of a class or a package
 * and that class or the package no longer has that property.
 */
@SinceKotlin("1.1")
class NoSuchPropertyException(cause: Exception? = null) : Exception(cause)
