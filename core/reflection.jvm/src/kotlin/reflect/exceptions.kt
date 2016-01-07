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
 * An exception that is thrown when `call` is invoked on a callable or `get` or `set` is invoked on a property
 * and that callable is not accessible (in JVM terms) from the calling method.
 *
 * @param cause the original exception thrown by the JVM.
 *
 * @see [kotlin.reflect.jvm.isAccessible]
 */
class IllegalCallableAccessException(cause: IllegalAccessException) : Exception(cause.message) {
    init {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        (this as java.lang.Throwable).initCause(cause)
    }
}

/**
 * An exception that is thrown when the code tries to introspect a property of a class or a package
 * and that class or the package no longer has that property.
 */
class NoSuchPropertyException(cause: Exception? = null) : Exception() {
    init {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        if (cause != null) {
            (this as java.lang.Throwable).initCause(cause)
        }
    }
}

/**
 * Signals that Kotlin reflection had reached an inconsistent state from which it cannot recover.
 */
class KotlinReflectionInternalError(message: String) : Error(message)
