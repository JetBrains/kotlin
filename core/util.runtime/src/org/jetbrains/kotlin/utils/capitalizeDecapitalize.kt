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

package org.jetbrains.kotlin.util.capitalizeDecapitalize

/**
 * "FooBar" -> "fooBar"
 * "FOOBar" -> "fooBar"
 * "FOO" -> "foo"
 */
public fun String.decapitalizeSmart(): String {
    if (isEmpty() || !charAt(0).isUpperCase()) return this

    if (length() == 1 || !charAt(1).isUpperCase()) {
        return decapitalize()
    }

    val secondWordStart = (indices.firstOrNull { !charAt(it).isUpperCase() }
                           ?: return toLowerCase()) - 1
    return substring(0, secondWordStart).toLowerCase() + substring(secondWordStart)
}

/**
 * "fooBar" -> "FOOBar"
 * "FooBar" -> "FOOBar"
 * "foo" -> "FOO"
 */
public fun String.capitalizeFirstWord(): String {
    val secondWordStart = indices.drop(1).firstOrNull { !charAt(it).isLowerCase() }
                          ?: return toUpperCase()
    return substring(0, secondWordStart).toUpperCase() + substring(secondWordStart)
}

public fun String.capitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = charAt(0)
    return if (c in 'a'..'z')
        c.toUpperCase() + substring(1)
    else
        this
}


