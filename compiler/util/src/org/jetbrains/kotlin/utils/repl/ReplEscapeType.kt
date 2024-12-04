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

package org.jetbrains.kotlin.utils.repl

enum class ReplEscapeType {
    INITIAL_PROMPT,
    HELP_PROMPT,
    USER_OUTPUT,
    REPL_RESULT,
    READLINE_START,
    READLINE_END,
    REPL_INCOMPLETE,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    INTERNAL_ERROR,
    ERRORS_REPORTED, // should be send after reporting all errors caused by the current command
                     // e.g. IDE uses it to recognize the end of command processing
    SUCCESS;

    companion object {
        fun valueOfOrNull(string: String): ReplEscapeType? {
            return try {
                valueOf(string)
            }
            catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}