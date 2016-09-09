/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Parse the given FQ name with possible generic arguments to a **top-level** ClassId instance.
 * E.g. "test.A<B>.C<D, E>" is parsed to a ClassId "test/A/C" which represents a class named "C" declared in package "test.A".
 */
internal fun parseCanonicalFqNameIgnoringTypeArguments(fqName: String): ClassId {
    val nameParts = fqName.splitCanonicalFqName()
    val resultingClassFqName = FqName(nameParts.joinToString(separator = ".") { it.substringBefore('<') })
    return ClassId.topLevel(resultingClassFqName)
}

// "test.A<B.C>.D<E<F.G, H>, I.J>" -> ["test", "A<B.C>", "D<E<F.G, H>, I.J>"]
private fun String.splitCanonicalFqName(): List<String> {
    fun String.toNonEmpty(): String =
            if (this.isNotEmpty()) this else SpecialNames.SAFE_IDENTIFIER_FOR_NO_NAME.asString()

    val result = arrayListOf<String>()
    var balance = 0
    var currentNameStart = 0
    for ((index, character) in this.withIndex()) {
        when (character) {
            '.' -> if (balance == 0) {
                result.add(this.substring(currentNameStart, index).toNonEmpty())
                currentNameStart = index + 1
            }
            '<' -> balance++
            '>' -> balance--
        }
    }
    result.add(this.substring(currentNameStart).toNonEmpty())
    return result
}
