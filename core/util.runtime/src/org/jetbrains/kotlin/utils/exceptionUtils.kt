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

package org.jetbrains.kotlin.utils

import java.io.Closeable

/**
 * Translate exception to unchecked exception.
 *
 * Return type is specified to make it possible to use it like this:
 *     throw ExceptionUtils.rethrow(e);
 * In this case compiler knows that code after this rethrowing won't be executed.
 */
fun rethrow(e: Throwable): RuntimeException {
    throw e
}

fun closeQuietly(closeable: Closeable?) {
    if (closeable != null) {
        try {
            closeable.close()
        }
        catch (ignored: Throwable) {
            // Do nothing
        }
    }
}

fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}
