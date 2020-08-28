/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord

object FirJavaSyntheticNamesProvider : FirSyntheticNamesProvider() {
    private const val GETTER_PREFIX = "get"
    private const val IS_PREFIX = "is"

    override fun possibleGetterNamesByPropertyName(name: Name): List<Name> {
        if (name.isSpecial) return emptyList()
        val identifier = name.identifier
        val capitalizedAsciiName = identifier.capitalizeAsciiOnly()
        val capitalizedFirstWordName = identifier.capitalizeFirstWord(asciiOnly = true)
        return listOfNotNull(
            Name.identifier(GETTER_PREFIX + capitalizedAsciiName),
            if (capitalizedFirstWordName == capitalizedAsciiName) null else Name.identifier(GETTER_PREFIX + capitalizedFirstWordName),
            name.takeIf { identifier.startsWith(IS_PREFIX) }
        ).filter {
            propertyNameByGetMethodName(it) == name
        }
    }

    override fun setterNameByGetterName(name: Name): Name {
        val identifier = name.identifier
        val prefix = when {
            identifier.startsWith("get") -> "get"
            identifier.startsWith("is") -> "is"
            else -> throw IllegalArgumentException()
        }
        return Name.identifier("set" + identifier.removePrefix(prefix))
    }
}

fun FirSession.registerJavaSyntheticNamesProvider() {
    register(FirSyntheticNamesProvider::class, FirJavaSyntheticNamesProvider)
}
