/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
                } else if (!Character.isJavaIdentifierPart(c)) return false
            }
        }
    }

    return state != State.AFTER_DOT
}

fun <V> FqName.findValueForMostSpecificFqname(values: Map<FqName, V>): V? {
    val suitableItems = values.filter { (fqName, _) -> this == fqName || this.isChildOf(fqName) }
        .takeIf { it.isNotEmpty() } ?: return null

    return suitableItems.minByOrNull { (fqName, _) -> fqName.tail(this).asString().length }?.value
}

fun ClassId.callableIdForConstructor(): CallableId {
    return if (isNestedClass) {
        CallableId(outerClassId!!, shortClassName)
    } else {
        CallableId(packageFqName, shortClassName)
    }
}
