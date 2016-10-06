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

package org.jetbrains.kotlin.cli.common.messages

import java.io.Serializable

data class CompilerMessageLocation private constructor(
        val path: String?,
        val line: Int,
        val column: Int,
        val lineContent: String?
) : Serializable {
    override fun toString(): String =
            path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        @JvmField val NO_LOCATION: CompilerMessageLocation = CompilerMessageLocation(null, -1, -1, null)

        @JvmStatic fun create(
                path: String?,
                line: Int,
                column: Int,
                lineContent: String?
        ): CompilerMessageLocation =
                if (path == null) NO_LOCATION else CompilerMessageLocation(path, line, column, lineContent)

        private val serialVersionUID: Long = 8228357578L
    }
}
