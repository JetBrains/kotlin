/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

@NoMutableState
object FirJavaSyntheticNamesProvider : FirSyntheticNamesProvider() {
    private const val GETTER_PREFIX = "get"
    private const val SETTER_PREFIX = "set"
    private const val IS_PREFIX = "is"

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Char.isAsciiUpperCase() = this in 'A'..'Z'

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Char.isAsciiLowerCase() = this in 'a'..'z'

    override fun possibleGetterNamesByPropertyName(name: Name): List<Name> {
        if (name.isSpecial) return emptyList()
        val identifier = name.identifier
        if (identifier.isEmpty()) return emptyList()
        val result = ArrayList<Name>(3)
        val standardName = Name.identifier(GETTER_PREFIX + identifier.capitalizeAsciiOnly())
        val length = identifier.length
        if (length == 1) {
            if (identifier[0].isAsciiLowerCase()) {
                // 'x' --> 'getX' but not 'X' --> 'getX'
                result += standardName
            }
        } else if (identifier[1].isAsciiLowerCase()) {
            if (identifier[0].isAsciiLowerCase()) {
                // 'something' --> 'getSomething' classic case
                result += standardName
            }
            var secondWordStart = 2
            while (secondWordStart < length && identifier[secondWordStart].isAsciiLowerCase()) {
                secondWordStart++
            }
            val capitalizedFirstWordName = Name.identifier(
                GETTER_PREFIX + identifier.substring(0, secondWordStart).toUpperCaseAsciiOnly() + identifier.substring(secondWordStart)
            )
            if (secondWordStart >= length || identifier[secondWordStart].isAsciiUpperCase()) {
                // 'xyz' --> 'getXYZ' or 'xyzOfSomething' --> 'getXYZOfSomething'
                result += capitalizedFirstWordName
            }
        } else if (length < 3 || !identifier[2].isAsciiUpperCase()) {
            // 'xOfSomething' --> 'getXOfSomething' but not 'xYZ' --> 'getXYZ'
            result += standardName
        }
        if (length > IS_PREFIX.length && identifier.startsWith(IS_PREFIX) && !identifier[IS_PREFIX.length].isAsciiLowerCase()) {
            // 'isSomething' (but not 'is' or 'issomething')
            result += name
        }
        return result
    }

    override fun setterNameByGetterName(name: Name): Name? {
        val identifier = name.identifier
        val prefix = when {
            identifier.startsWith(GETTER_PREFIX) -> GETTER_PREFIX
            identifier.startsWith(IS_PREFIX) -> IS_PREFIX
            else -> return null
        }
        return Name.identifier(SETTER_PREFIX + identifier.removePrefix(prefix))
    }

    override fun getterNameBySetterName(name: Name): Name? {
        val identifier = name.identifier
        val prefix = when {
            identifier.startsWith(SETTER_PREFIX) -> SETTER_PREFIX
            else -> return null
        }
        return Name.identifier(GETTER_PREFIX + identifier.removePrefix(prefix))
    }

    override fun possiblePropertyNamesByAccessorName(name: Name): List<Name> {
        if (name.isSpecial) return emptyList()
        val identifier = name.identifier
        val prefix = when {
            identifier.startsWith(GETTER_PREFIX) -> GETTER_PREFIX
            identifier.startsWith(IS_PREFIX) -> ""
            identifier.startsWith(SETTER_PREFIX) -> SETTER_PREFIX
            else -> return emptyList()
        }
        val withoutPrefix = identifier.removePrefix(prefix)
        val withoutPrefixName = Name.identifier(withoutPrefix.decapitalize())
        return if (prefix == SETTER_PREFIX) {
            listOf(withoutPrefixName, Name.identifier(IS_PREFIX + withoutPrefix))
        } else {
            listOf(withoutPrefixName)
        }
    }
}

