/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

/**
 * The main purpose of the lookup tag is to provide a reference to concrete classifier that
 * allows obtaining FirClassifierSymbol within a given use-site session.
 *
 * During the deserialization we use lookup tags to avoid loading the entire class-type hierarchy.
 *
 * Use-site lookup of classifiers that are referenced by lookup tags allows type refinement that is needed for the expect/actual support.
 *
 * See `/docs/fir/k2_kmp.md`
 */
abstract class ConeClassifierLookupTag : TypeConstructorMarker {
    abstract val name: Name

    override fun toString(): String {
        return name.asString()
    }
}

/**
 * @see ConeClassifierLookupTag
 */
abstract class ConeClassLikeLookupTag : ConeClassifierLookupTag() {
    abstract val classId: ClassId

    override val name: Name
        get() = classId.shortClassName
}

