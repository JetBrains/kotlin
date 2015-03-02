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
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public open class Throwable(message: String? = null, cause: Throwable? = null) {
    /**
     * Returns the detail message of this throwable.
     */
    public fun getMessage(): String?

    /**
     * Returns the cause of this throwable.
     */
    public fun getCause(): Throwable?

    /**
     * Prints the stack trace of this throwable to the standard output.
     */
    public fun printStackTrace(): Unit
}
