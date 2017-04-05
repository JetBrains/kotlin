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

package org.jetbrains.kotlin.javac

import com.sun.tools.javac.util.Options
import java.util.regex.Pattern

object JavacOptionsMapper {

    fun map(options: Options, arguments: List<String>) = arguments.forEach { options.putOption(it) }

    private val optionPattern by lazy { Pattern.compile("\\s+") }

    private fun Options.putOption(option: String) = option
            .split(optionPattern)
            .filter { it.isNotEmpty() }
            .let { arg ->
                when(arg.size) {
                    1 -> put(arg[0], arg[0])
                    2 -> put(arg[0], arg[1])
                    else -> null
                }
            }

}