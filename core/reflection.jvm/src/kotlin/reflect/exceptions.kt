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

public class IllegalPropertyAccessException(cause: IllegalAccessException) : Exception(cause.getMessage()) {
    {
        [suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")]
        (this as java.lang.Throwable).initCause(cause)
    }
}

public class NoSuchPropertyException(cause: Exception? = null) : Exception() {
    {
        [suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")]
        if (cause != null) {
            (this as java.lang.Throwable).initCause(cause)
        }
    }
}

public class KotlinReflectionInternalError(message: String) : Error(message)
