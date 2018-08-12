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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.utils.sure

class BuiltInsInitializer<out T : KotlinBuiltIns>(
        private val constructor: () -> T
) {
    @Volatile private var instance: T? = null

    @Volatile private var initializing: Boolean = false

    private var initializationFailed: Throwable? = null

    @Synchronized private fun initialize() {
        if (instance == null) {
            if (initializationFailed != null) {
                throw IllegalStateException(
                        "Built-in library initialization failed previously: " + initializationFailed!!, initializationFailed)
            }
            if (initializing) {
                throw IllegalStateException("Built-in library initialization loop")
            }
            initializing = true
            try {
                instance = constructor()
            }
            catch (e: Throwable) {
                initializationFailed = e
                throw IllegalStateException("Built-in library initialization failed. " + "Please ensure you have kotlin-stdlib.jar in the classpath: " + e, e)
            }
            finally {
                initializing = false
            }
        }
    }

    fun get(): T {
        if (initializing) {
            synchronized (this) {
                return instance.sure {
                    "Built-ins are not initialized (note: We are under the same lock as initializing and instance)"
                }
            }
        }
        if (instance == null) {
            initialize()
        }
        return instance!!
    }
}