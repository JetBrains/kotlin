/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.KotlinTypeIM
import org.jetbrains.kotlin.types.model.SimpleTypeIM
import org.jetbrains.kotlin.types.model.TypeSystemContext

object AbstractStrictEqualityTypeChecker {
    fun strictEqualTypes(context: TypeSystemContext, a: KotlinTypeIM, b: KotlinTypeIM) = context.strictEqualTypesInternal(a, b)

    /**
     * String! != String & A<String!> != A<String>, also A<in Nothing> != A<out Any?>
     * also A<*> != A<out Any?>
     * different error types non-equals even errorTypeEqualToAnything
     */
    private fun TypeSystemContext.strictEqualTypesInternal(a: KotlinTypeIM, b: KotlinTypeIM): Boolean {
        if (a === b) return true

        val simpleA = a.asSimpleType()
        val simpleB = b.asSimpleType()
        if (simpleA != null && simpleB != null) return strictEqualTypesInternal(simpleA, simpleB)

        val flexibleA = a.asFlexibleType()
        val flexibleB = b.asFlexibleType()
        if (flexibleA != null && flexibleB != null) {
            return strictEqualTypesInternal(flexibleA.lowerBound(), flexibleB.lowerBound()) &&
                    strictEqualTypesInternal(flexibleA.upperBound(), flexibleB.upperBound())
        }
        return false
    }

    private fun TypeSystemContext.strictEqualTypesInternal(a: SimpleTypeIM, b: SimpleTypeIM): Boolean {
        if (a.isMarkedNullable() != b.isMarkedNullable()
            || (a.asDefinitelyNotNullType() == null) != (b.asDefinitelyNotNullType() == null)
            || !isEqualTypeConstructors(a.typeConstructor(), b.typeConstructor())
            || a.argumentsCount() != b.argumentsCount()
        ) {
            return false
        }

//        if (a.arguments === b.arguments) return true

        for (i in 0..(a.argumentsCount() - 1)) {
            val aArg = a.getArgument(i)
            val bArg = b.getArgument(i)
            if (aArg.isStarProjection() != bArg.isStarProjection()) return false

            // both non-star
            if (!aArg.isStarProjection()) {
                if (aArg.getVariance() != bArg.getVariance()) return false
                if (!strictEqualTypesInternal(aArg.getType(), bArg.getType())) return false
            }
        }
        return true
    }

}
