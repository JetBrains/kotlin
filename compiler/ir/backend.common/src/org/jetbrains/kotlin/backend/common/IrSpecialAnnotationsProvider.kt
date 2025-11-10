/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.expressions.IrAnnotation

abstract class IrSpecialAnnotationsProvider {
    abstract fun generateEnhancedNullabilityAnnotationCall(): IrAnnotation

    abstract fun generateFlexibleNullabilityAnnotationCall(): IrAnnotation

    abstract fun generateFlexibleMutabilityAnnotationCall(): IrAnnotation

    abstract fun generateFlexibleArrayElementVarianceAnnotationCall(): IrAnnotation

    abstract fun generateRawTypeAnnotationCall(): IrAnnotation
}
