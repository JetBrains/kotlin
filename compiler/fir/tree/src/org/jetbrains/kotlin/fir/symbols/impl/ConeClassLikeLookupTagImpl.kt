/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId

class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag() {
    var boundSymbol: Pair<*, FirClassLikeSymbol<*>?>? = null

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