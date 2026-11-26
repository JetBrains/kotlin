/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * The naming convention for JVM accessor methods of Kotlin properties.
 */
object JvmPropertyAccessorNaming {
    private const val GET_PREFIX = "get"
    private const val IS_PREFIX = "is"
    private const val SET_PREFIX = "set"

    /**
     * Checks whether [name] starts with `get` or `is`.
     */
    @JvmStatic
    fun isGetterName(name: String): Boolean {
        return name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)
    }

    /**
     * Checks whether [name] starts with `set`.
     */
    @JvmStatic
    fun isSetterName(name: String): Boolean {
        return name.startsWith(SET_PREFIX)
    }

    /**
     * Returns the getter name for a property: `foo` gives `getFoo`, while a name with an [`is` prefix][startsWithIsPrefix] is kept as is.
     */
    @JvmStatic
    fun getterName(propertyName: String): String {
        return if (startsWithIsPrefix(propertyName)) {
            propertyName
        } else {
            GET_PREFIX + propertyName.capitalizeAsciiOnly()
        }
    }

    /**
     * Returns the setter name for a property: `foo` gives `setFoo`, and an [`is` prefix][startsWithIsPrefix] is replaced, so `isFoo`
     * also gives `setFoo`.
     */
    @JvmStatic
    fun setterName(propertyName: String): String {
        val nameBase = if (startsWithIsPrefix(propertyName)) {
            propertyName.substring(IS_PREFIX.length)
        } else {
            propertyName.capitalizeAsciiOnly()
        }

        return SET_PREFIX + nameBase
    }

    /**
     * Checks whether [name] starts with `is` followed by anything but a lowercase ASCII letter: `isFoo` and `is_foo` match, `issue` and
     * `is` do not.
     */
    @JvmStatic
    fun startsWithIsPrefix(name: String): Boolean {
        return when {
            !name.startsWith(IS_PREFIX) -> false
            name.length == IS_PREFIX.length -> false
            else -> {
                val c = name[IS_PREFIX.length]
                c !in 'a'..'z'
            }
        }
    }

    private fun String.capitalizeAsciiOnly(): String {
        if (isEmpty()) {
            return this
        }

        val c = this[0]
        if (c !in 'a'..'z') {
            return this
        }

        return buildString(length) {
            append(c.uppercaseChar())
            append(this@capitalizeAsciiOnly, 1, this@capitalizeAsciiOnly.length)
        }
    }
}
