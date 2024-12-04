/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor

open class ErasureProjectionComputer {
    open fun computeProjection(
        parameter: TypeParameterDescriptor,
        typeAttr: ErasureTypeAttributes,
        typeParameterUpperBoundEraser: TypeParameterUpperBoundEraser,
        erasedUpperBound: KotlinType = typeParameterUpperBoundEraser.getErasedUpperBound(parameter, typeAttr)
    ): TypeProjection = TypeProjectionImpl(Variance.OUT_VARIANCE, erasedUpperBound)
}
