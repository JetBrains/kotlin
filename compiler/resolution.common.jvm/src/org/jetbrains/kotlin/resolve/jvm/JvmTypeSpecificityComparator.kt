/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext

open class JvmTypeSpecificityComparator(open val context: TypeSystemInferenceExtensionContext) : TypeSpecificityComparator {

    override fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean = with(context) {
        val simpleGeneral = general.asSimpleType()
        if (!specific.isFlexible() || simpleGeneral == null) return false

        // general is inflexible
        val flexibility = specific.asFlexibleType()!!

        // For primitive types we have to take care of the case when there are two overloaded methods like
        //    foo(int) and foo(Integer)
        // if we do not discriminate one of them, any call to foo(kotlin.Int) will result in overload resolution ambiguity
        // so, for such cases, we discriminate Integer in favour of int
        if (!simpleGeneral.isPrimitiveType() || !flexibility.lowerBound().isPrimitiveType()) {
            return false
        }

        // Int? >< Int!
        if (simpleGeneral.isMarkedNullable()) return false
        // Int! lessSpecific Int
        return true
    }
}
