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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

//NOTE: should be moved to some "util" place

val TextRange.start: Int
    get() = startOffset

val TextRange.end: Int
    get() = endOffset

val PsiElement.range: TextRange
    get() = textRange!!

val RangeMarker.range: TextRange?
    get() = if (isValid) {
        val start = startOffset
        val end = endOffset
        if (start in 0..end) {
            TextRange(start, end)
        } else {
            // Probably a race condition had happened and range marker is invalidated
            null
        }
    } else null
