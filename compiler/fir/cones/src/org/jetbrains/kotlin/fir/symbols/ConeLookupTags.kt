/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class ConeClassifierLookupTag {
    abstract val name: Name
}

abstract class ConeClassifierLookupTagWithFixedSymbol : ConeClassifierLookupTag() {
    abstract val symbol: ConeClassifierSymbol
}

data class ConeTypeParameterLookupTag(val typeParameterSymbol: ConeTypeParameterSymbol) : ConeClassifierLookupTagWithFixedSymbol() {
    override val name: Name get() = typeParameterSymbol.name
    override val symbol: ConeClassifierSymbol
        get() = typeParameterSymbol

}

abstract class ConeClassLikeLookupTag : ConeClassifierLookupTag() {
    abstract val classId: ClassId

    override val name: Name
        get() = classId.shortClassName
}

abstract class ConeTypeAliasLookupTag : ConeClassLikeLookupTag()

abstract class ConeClassLookupTag : ConeClassLikeLookupTag()

class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag() {
    var boundSymbol: Pair<*, ConeClassifierSymbol?>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeLookupTagImpl

        if (classId != other.classId) return false

        return true
    }

    override fun hashCode(): Int {
        return classId.hashCode()
    }
}
