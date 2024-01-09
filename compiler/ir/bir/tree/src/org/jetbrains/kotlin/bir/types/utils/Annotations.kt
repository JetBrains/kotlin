/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.types.BirDynamicType
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.memoryOptimizedFilterNot
import org.jetbrains.kotlin.utils.memoryOptimizedPlus


fun BirType.addAnnotations(newAnnotations: List<BirConstructorCall>): BirType =
    if (newAnnotations.isEmpty())
        this
    else when (this) {
        is BirSimpleType ->
            toBuilder().apply {
                annotations = annotations memoryOptimizedPlus newAnnotations
            }.buildSimpleType()
        is BirDynamicType ->
            BirDynamicType(null, annotations memoryOptimizedPlus newAnnotations, Variance.INVARIANT)
        else ->
            this
    }

fun BirType.removeAnnotations(predicate: (BirConstructorCall) -> Boolean): BirType =
    when (this) {
        is BirSimpleType ->
            toBuilder().apply {
                annotations = annotations.memoryOptimizedFilterNot(predicate)
            }.buildSimpleType()
        is BirDynamicType ->
            BirDynamicType(null, annotations.memoryOptimizedFilterNot(predicate), Variance.INVARIANT)
        else ->
            this
    }

fun BirType.removeAnnotations(): BirType =
    when (this) {
        is BirSimpleType ->
            toBuilder().apply {
                annotations = emptyList()
            }.buildSimpleType()
        is BirDynamicType ->
            BirDynamicType(null, emptyList(), Variance.INVARIANT)
        else ->
            this
    }
