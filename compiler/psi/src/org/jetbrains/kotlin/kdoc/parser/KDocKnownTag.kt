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

package org.jetbrains.kotlin.kdoc.parser

import com.intellij.openapi.util.text.StringUtil

enum class KDocKnownTag(val isReferenceRequired: Boolean, val isSectionStart: Boolean) {
    AUTHOR(false, false),
    THROWS(true, false),
    EXCEPTION(true, false),
    PARAM(true, false),
    RECEIVER(false, false),
    RETURN(false, false),
    SEE(true, false),
    SINCE(false, false),
    CONSTRUCTOR(false, true),
    PROPERTY(true, true),
    SAMPLE(true, false),
    SUPPRESS(false, false);


    companion object {
        fun findByTagName(tagName: CharSequence): KDocKnownTag? {
            var tagName = tagName
            if (StringUtil.startsWith(tagName, "@")) {
                tagName = tagName.subSequence(1, tagName.length)
            }
            try {
                return valueOf(tagName.toString().toUpperCase())
            } catch (ignored: IllegalArgumentException) {
            }

            return null
        }
    }
}
