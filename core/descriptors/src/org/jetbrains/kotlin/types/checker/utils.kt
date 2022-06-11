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

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.lowerBoundIfFlexible
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.typeConstructor
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.upperBoundIfFlexible
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

interface NewTypeVariableConstructor : TypeConstructor {
    val originalTypeParameter: TypeParameterDescriptor?
}

@Suppress("UNCHECKED_CAST")
fun findCorrespondingSupertypes(subType: KotlinTypeMarker, superType: KotlinTypeMarker): List<SimpleType> =
    AbstractTypeChecker.findCorrespondingSupertypes(
        createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true, captureArguments = false),
        subType.lowerBoundIfFlexible(),
        superType.upperBoundIfFlexible().typeConstructor()
    ) as List<SimpleType>

fun findCorrespondingSupertype(subType: KotlinTypeMarker, superType: KotlinTypeMarker): SimpleType? =
    findCorrespondingSupertypes(subType, superType).firstOrNull()
