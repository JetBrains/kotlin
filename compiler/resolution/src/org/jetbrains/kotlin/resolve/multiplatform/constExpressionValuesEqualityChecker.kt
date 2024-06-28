/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue

internal fun ClassicExpectActualMatchingContext.areExpressionConstValuesEqual(
    expectValue: Any?,
    actualValue: Any?,
    collectionArgumentsCompatibilityCheckStrategy: K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
): Boolean {
    return when {
        expectValue is AnnotationDescriptor && actualValue is AnnotationDescriptor -> {
            val aArgs = expectValue.allValueArguments
            val bArgs = actualValue.allValueArguments
            aArgs.size == bArgs.size &&
                    areCompatibleExpectActualTypes(expectValue.type, actualValue.type) &&
                    aArgs.keys.all { k -> areExpressionConstValuesEqual(aArgs[k], bArgs[k], collectionArgumentsCompatibilityCheckStrategy) }
        }
        expectValue is ConstantValue<*> && actualValue is ConstantValue<*> -> {
            areExpressionConstValuesEqual(expectValue.value, actualValue.value, collectionArgumentsCompatibilityCheckStrategy)
        }
        expectValue is Collection<*> && actualValue is Collection<*> -> {
            collectionArgumentsCompatibilityCheckStrategy.areCompatible(expectValue, actualValue) { f, s ->
                areExpressionConstValuesEqual(f, s, collectionArgumentsCompatibilityCheckStrategy)
            }
        }
        expectValue is Array<*> && actualValue is Array<*> -> {
            collectionArgumentsCompatibilityCheckStrategy.areCompatible(expectValue.toList(), actualValue.toList()) { f, s ->
                areExpressionConstValuesEqual(f, s, collectionArgumentsCompatibilityCheckStrategy)
            }
        }
        expectValue is KClassValue.Value.NormalClass && actualValue is KClassValue.Value.NormalClass -> {
            val expectClassIdOriginal = expectValue.classId
            val expectClassIdPlatform = findExpandedExpectClassInPlatformModule(expectClassIdOriginal)?.classId
            val expectValueCopy = expectValue.copy(
                value = expectValue.value.copy(
                    classId = expectClassIdPlatform ?: expectClassIdOriginal
                )
            )
            expectValueCopy == actualValue
        }
        else -> expectValue == actualValue
    }
}