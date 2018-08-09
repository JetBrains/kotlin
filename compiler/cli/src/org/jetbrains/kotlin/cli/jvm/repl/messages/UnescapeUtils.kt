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

package org.jetbrains.kotlin.cli.jvm.repl.messages

import com.intellij.openapi.util.text.StringUtil

// using '#' to avoid collisions with xml escaping
internal val SOURCE_CHARS: Array<String> = arrayOf("\n", "#")
internal val XML_REPLACEMENTS: Array<String> = arrayOf("#n", "#diez")

fun unescapeLineBreaks(s: String) = StringUtil.replace(s, XML_REPLACEMENTS, SOURCE_CHARS)
