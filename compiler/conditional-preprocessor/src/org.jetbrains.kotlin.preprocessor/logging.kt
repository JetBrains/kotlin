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

package org.jetbrains.kotlin.preprocessor

public interface Logger {
    fun debug(msg: CharSequence)
    fun info(msg: CharSequence)
    fun warn(msg: CharSequence)
    fun error(msg: CharSequence)
}

public object SystemOutLogger : Logger {
    private fun out(level: String, msg: CharSequence) = println("[$level] $msg")

    public var isDebugEnabled: Boolean = false
    override fun debug(msg: CharSequence) = if (isDebugEnabled) out("DEBUG", msg)
    override fun info(msg: CharSequence) = out("INFO", msg)
    override fun warn(msg: CharSequence) = out("WARN", msg)
    override fun error(msg: CharSequence) = out("ERROR", msg)
}

public fun Logger.withPrefix(prefix: String): Logger = PrefixedLogger(prefix, this)

public class PrefixedLogger(val prefix: String, val logger: Logger) : Logger {
    private fun prefix(msg: CharSequence): CharSequence = StringBuilder {
        append(prefix)
        append(": ")
        append(msg)
    }

    override fun debug(msg: CharSequence) = logger.debug(prefix(msg))
    override fun info(msg: CharSequence) = logger.info(prefix(msg))
    override fun warn(msg: CharSequence) = logger.warn(prefix(msg))
    override fun error(msg: CharSequence) = logger.error(prefix(msg))
}