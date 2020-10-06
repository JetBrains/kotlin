/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext

object ConeNullabilityChecker {
    fun isSubtypeOfAny(context: ConeTypeContext, type: ConeKotlinType): Boolean {
        val actualType = with(context) { type.lowerBoundIfFlexible() }
        return with(AbstractNullabilityChecker) {
            context.newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
                .hasNotNullSupertype(actualType, AbstractTypeCheckerContext.SupertypesPolicy.LowerIfFlexible)
        }
    }
}
