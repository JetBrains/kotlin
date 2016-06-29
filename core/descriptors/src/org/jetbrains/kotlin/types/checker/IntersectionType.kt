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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*

fun intersectTypes(types: List<UnwrappedType>): UnwrappedType {
    when (types.size) {
        0 -> error("Expected some types")
        1 -> return types.single()
    }
    var hasFlexibleTypes = false
    var hasErrorType = false
    val lowerBounds = types.map {
        hasErrorType = hasErrorType || it.isError
        when (it) {
            is SimpleType -> it
            is FlexibleType -> {
                hasFlexibleTypes = true
                it.lowerBound
            }
        }
    }
    if (hasErrorType) {
        return ErrorUtils.createErrorType("Intersection of error types: $types")
    }

    if (!hasFlexibleTypes) {
        return intersectTypes(lowerBounds)
    }

    val upperBounds = types.map { it.upperIfFlexible() }
    /**
     * We should save this rules:
     *  - if for each type from types type is subtype of A, then intersectionType should be subtype of A
     *  - same for type B which is subtype of all types.
     *
     *  Note: when we construct intersection type of dynamic(or Raw type) & other type, we can get non-dynamic type.  // todo discuss
     */
    return KotlinTypeFactory.flexibleType(intersectTypes(lowerBounds), intersectTypes(upperBounds))
}

// types.size >= 2
// It is incorrect see to nullability here, because of KT-12684
private fun intersectTypes(types: List<SimpleType>): SimpleType {
    val constructor = IntersectionTypeConstructor(types)
    return KotlinTypeFactory.simpleType(Annotations.EMPTY, constructor, listOf(), false, constructor.createScopeForKotlinType())
}

