/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.constants.ConstantValue

internal fun ExpectActualMatchingContext<*>.areExpressionConstValuesEqual(a: Any?, b: Any?): Boolean {
    return when {
        a is AnnotationDescriptor && b is AnnotationDescriptor -> {
            val aArgs = a.allValueArguments
            val bArgs = b.allValueArguments
            a.fqName == b.fqName &&
                    aArgs.size == bArgs.size &&
                    areCompatibleExpectActualTypes(a.type, b.type) &&
                    aArgs.keys.all { k -> areExpressionConstValuesEqual(aArgs[k], bArgs[k]) }
        }
        a is ConstantValue<*> && b is ConstantValue<*> -> {
            areExpressionConstValuesEqual(a.value, b.value)
        }
        a is Collection<*> && b is Collection<*> -> {
            a.size == b.size && a.zip(b).all { (f, s) -> areExpressionConstValuesEqual(f, s) }
        }
        a is Array<*> && b is Array<*> -> {
            a.size == b.size && a.zip(b).all { (f, s) -> areExpressionConstValuesEqual(f, s) }
        }
        else -> a == b
    }
}