/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.checker.ErrorTypesAreEqualToAnything

/**
 * This is temporary hack for type intersector.
 *
 * It is almost save, because:
 *  - it running only if general algorithm is failed
 *  - returned type is subtype of all [types].
 *
 * But it is hack, because it can give unstable result, but it better than exception.
 * See KT-11266.
 */
internal fun hackForTypeIntersector(types: Collection<KotlinType>): KotlinType? {
    if (types.size < 2) return types.firstOrNull()

    return types.firstOrNull { candidate ->
        types.all {
            ErrorTypesAreEqualToAnything.isSubtypeOf(candidate, it)
        }
    }
}

fun getEffectiveVariance(parameterVariance: Variance, projectionKind: Variance): Variance {
    if (parameterVariance === Variance.INVARIANT) {
        return projectionKind
    }
    if (projectionKind === Variance.INVARIANT) {
        return parameterVariance
    }
    if (parameterVariance === projectionKind) {
        return parameterVariance
    }

    // In<out X> = In<*>
    // Out<in X> = Out<*>
    return Variance.OUT_VARIANCE
}