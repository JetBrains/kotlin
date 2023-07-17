/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.constants.ConstantValue

internal fun ExpectActualMatchingContext<*>.areExpressionConstValuesEqual(
    a: Any?,
    b: Any?,
    collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
): Boolean {
    return when {
        a is AnnotationDescriptor && b is AnnotationDescriptor -> {
            val aArgs = a.allValueArguments
            val bArgs = b.allValueArguments
            a.fqName == b.fqName &&
                    aArgs.size == bArgs.size &&
                    areCompatibleExpectActualTypes(a.type, b.type) &&
                    aArgs.keys.all { k -> areExpressionConstValuesEqual(aArgs[k], bArgs[k], collectionArgumentsCompatibilityCheckStrategy) }
        }
        a is ConstantValue<*> && b is ConstantValue<*> -> {
            areExpressionConstValuesEqual(a.value, b.value, collectionArgumentsCompatibilityCheckStrategy)
        }
        a is Collection<*> && b is Collection<*> -> {
            collectionArgumentsCompatibilityCheckStrategy.areCompatible(a, b) { f, s ->
                areExpressionConstValuesEqual(f, s, collectionArgumentsCompatibilityCheckStrategy)
            }
        }
        a is Array<*> && b is Array<*> -> {
            collectionArgumentsCompatibilityCheckStrategy.areCompatible(a.toList(), b.toList()) { f, s ->
                areExpressionConstValuesEqual(f, s, collectionArgumentsCompatibilityCheckStrategy)
            }
        }
        else -> a == b
    }
}