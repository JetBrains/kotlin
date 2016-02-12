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

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

fun Name.render(): String {
    return if (this.shouldBeEscaped()) '`' + asString() + '`' else asString()
}

private fun Name.shouldBeEscaped(): Boolean {
    if (isSpecial) return false

    val string = asString()
    return string in KeywordStringsGenerated.KEYWORDS || string.any { !Character.isLetterOrDigit(it) && it != '_' }
}

fun FqNameUnsafe.render(): String {
    return renderFqName(pathSegments())
}

fun FqName.render(): String {
    return renderFqName(pathSegments())
}

fun renderFqName(pathSegments: List<Name>): String {
    return buildString {
        for (element in pathSegments) {
            if (length > 0) {
                append(".")
            }
            append(element.render())
        }
    }
}

