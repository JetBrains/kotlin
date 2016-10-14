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

package org.jetbrains.kotlin.name

fun FqName.isSubpackageOf(packageName: FqName): Boolean {
    return when {
        this == packageName -> true
        packageName.isRoot -> true
        else -> isSubpackageOf(this.asString(), packageName.asString())
    }
}

fun FqName.isChildOf(packageName: FqName): Boolean = parentOrNull() == packageName

private fun isSubpackageOf(subpackageNameStr: String, packageNameStr: String): Boolean {
    return subpackageNameStr.startsWith(packageNameStr) && subpackageNameStr[packageNameStr.length] == '.'
}

fun FqName.isOneSegmentFQN(): Boolean = !isRoot && parent().isRoot

/**
 * Get the tail part of the FQ name by stripping a prefix. If FQ name does not begin with the given prefix, it will be returned as is.
 *
 * Examples:
 * "org.jetbrains.kotlin".tail("org") = "jetbrains.kotlin"
 * "org.jetbrains.kotlin".tail("") = "org.jetbrains.kotlin"
 * "org.jetbrains.kotlin".tail("org.jetbrains.kotlin") = ""
 * "org.jetbrains.kotlin".tail("org.jetbrains.gogland") = "org.jetbrains.kotlin"
 */
fun FqName.tail(prefix: FqName): FqName {
    return when {
        !isSubpackageOf(prefix) || prefix.isRoot -> this
        this == prefix -> FqName.ROOT
        else -> FqName(asString().substring(prefix.asString().length + 1))
    }
}

fun FqName.parentOrNull(): FqName? = if (this.isRoot) null else parent()

private enum class State {
    BEGINNING,
    MIDDLE,
    AFTER_DOT
}

// Check that it is javaName(\.javaName)* or an empty string
fun isValidJavaFqName(qualifiedName: String?): Boolean {
    if (qualifiedName == null) return false

    var state = State.BEGINNING

    for (c in qualifiedName) {
        when (state) {
            State.BEGINNING, State.AFTER_DOT -> {
                if (!Character.isJavaIdentifierPart(c)) return false
                state = State.MIDDLE
            }
            State.MIDDLE -> {
                if (c == '.') {
                    state = State.AFTER_DOT
                }
                else if (!Character.isJavaIdentifierPart(c)) return false
            }
        }
    }

    return state != State.AFTER_DOT
}
