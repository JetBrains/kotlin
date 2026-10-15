/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.expressions.IrAnnotation

abstract class IrSpecialAnnotationsProvider {
    abstract fun generateEnhancedNullabilityAnnotation(): IrAnnotation

    abstract fun generateFlexibleNullabilityAnnotation(): IrAnnotation

    abstract fun generateFlexibleMutabilityAnnotation(): IrAnnotation

    abstract fun generateFlexibleArrayElementVarianceAnnotation(): IrAnnotation

    abstract fun generateRawTypeAnnotation(): IrAnnotation
}
