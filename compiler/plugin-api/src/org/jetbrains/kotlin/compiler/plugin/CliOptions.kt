/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compiler.plugin

import org.jetbrains.jet.config.CompilerConfiguration
import java.util.regex.Pattern

public class CliOption(
        public val name: String,
        public val valueDescription: String,
        public val description: String,
        public val required: Boolean = true,
        public val allowMultipleOccurrences: Boolean = false
)

public class CliOptionProcessingException(message: String, cause: Throwable? = null): RuntimeException(message, cause)

public data class CliOptionValue(
        val pluginId: String,
        val optionName: String,
        val value: String
) {
    override fun toString() = "$pluginId:$optionName=$value"
}

public fun parsePluginOption(argumentValue: String): CliOptionValue? {
    val pattern = Pattern.compile("""^plugin:([^:]*):([^=]*)=(.*)$""")
    val matcher = pattern.matcher(argumentValue)
    if (matcher.matches()) {
        return CliOptionValue(matcher.group(1), matcher.group(2), matcher.group(3))
    }

    return null
}

public fun getPluginOptionString(pluginId: String, key: String, value: String): String {
    return "plugin:$pluginId:$key=$value"
}