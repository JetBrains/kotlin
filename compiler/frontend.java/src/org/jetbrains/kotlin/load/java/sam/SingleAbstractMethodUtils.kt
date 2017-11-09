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

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains

// If type 'samType' contains no projection, then it's non-projection parametrization is 'samType' itself
// Else each projection type argument 'out/in A_i' (but star projections) is replaced with it's bound 'A_i'
// Star projections are treated specially:
// - If first upper bound of corresponding type parameter does not contain any type parameter of 'samType' class,
//   then use this upper bound instead of star projection
// - Otherwise no non-projection parametrization exists for such 'samType'
//
// See Non-wildcard parametrization in JLS 8 p.9.9 for clarification
internal fun nonProjectionParametrization(samType: SimpleType): SimpleType? {
    if (samType.arguments.none { it.projectionKind != Variance.INVARIANT }) return samType
    val parameters = samType.constructor.parameters
    val parametersSet = parameters.toSet()

    return samType.replace(
            newArguments = samType.arguments.zip(parameters).map {
                val (projection, parameter) = it
                when {
                    projection.projectionKind == Variance.INVARIANT -> projection

                    projection.isStarProjection ->
                        parameter.upperBounds.first().takeUnless {
                            t -> t.contains { it.constructor.declarationDescriptor in parametersSet }
                        }?.asTypeProjection() ?: return@nonProjectionParametrization null

                    else -> projection.type.asTypeProjection()
                }
            })
}
