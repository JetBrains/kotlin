/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class OverridingUtilTypeSystemContext(
    val matchingTypeConstructors: Map<TypeConstructor, TypeConstructor>?,
    private val equalityAxioms: KotlinTypeChecker.TypeConstructorEquality,
    private val kotlinTypeRefiner: KotlinTypeRefiner,
    private val kotlinTypePreparator: KotlinTypePreparator,
    private val customSubtype: ((KotlinType, KotlinType) -> Boolean)? = null,
) : ClassicTypeSystemContext {

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor)
        require(c2 is TypeConstructor)
        return super.areEqualTypeConstructors(c1, c2) || areEqualTypeConstructorsByAxioms(c1, c2)
    }

    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        if (customSubtype == null) {
            return createClassicTypeCheckerState(
                errorTypesEqualToAnything,
                stubTypesEqualToAnything,
                typeSystemContext = this,
                kotlinTypeRefiner = kotlinTypeRefiner,
                kotlinTypePreparator = kotlinTypePreparator,
            )
        }

        return object : TypeCheckerState(
            errorTypesEqualToAnything, stubTypesEqualToAnything, allowedTypeVariable = true,
            typeSystemContext = this,
            kotlinTypePreparator, kotlinTypeRefiner,
        ) {
            override fun customIsSubtypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {
                require(subType is KotlinType)
                require(superType is KotlinType)
                return customSubtype.invoke(subType, superType)
            }
        }
    }

    private fun areEqualTypeConstructorsByAxioms(a: TypeConstructor, b: TypeConstructor): Boolean {
        if (equalityAxioms.equals(a, b)) return true
        if (matchingTypeConstructors == null) return false
        val img1 = matchingTypeConstructors[a]
        val img2 = matchingTypeConstructors[b]
        return img1 != null && img1 == b || img2 != null && img2 == a
    }
}
