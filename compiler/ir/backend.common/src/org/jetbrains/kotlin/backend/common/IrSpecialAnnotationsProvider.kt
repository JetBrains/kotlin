/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

abstract class IrSpecialAnnotationsProvider {
    abstract fun generateEnhancedNullabilityAnnotationCall(): IrConstructorCall

    abstract fun generateFlexibleNullabilityAnnotationCall(): IrConstructorCall

    abstract fun generateFlexibleMutabilityAnnotationCall(): IrConstructorCall

    abstract fun generateFlexibleArrayElementVarianceAnnotationCall(): IrConstructorCall

    abstract fun generateRawTypeAnnotationCall(): IrConstructorCall
}
