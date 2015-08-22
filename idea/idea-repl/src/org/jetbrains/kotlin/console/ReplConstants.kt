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

package org.jetbrains.kotlin.console

public object ReplConstants {
    public val XML_PREFIX: String = "<?xml"
    public val XML_PREAMBLE: String = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

    public val SOURCE_CHARS: Array<String>     = arrayOf("\n", "#")
    public val XML_REPLACEMENTS: Array<String> = arrayOf("#n", "#diez")
}