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

package org.jetbrains.kotlin.config

enum class JVMConstructorCallNormalizationMode(
    val description: String,
    val isEnabled: Boolean,
    val shouldPreserveClassInitialization: Boolean
) {
    DISABLE("disable", false, false),
    ENABLE("enable", true, false),
    PRESERVE_CLASS_INITIALIZATION("preserve-class-initialization", true, true)
    ;

    companion object {
        @JvmStatic
        fun isSupportedValue(string: String?) =
            string == null || values().any { it.description == string }

        @JvmStatic
        fun fromStringOrNull(string: String?) =
            values().find { it.description == string }
    }
}