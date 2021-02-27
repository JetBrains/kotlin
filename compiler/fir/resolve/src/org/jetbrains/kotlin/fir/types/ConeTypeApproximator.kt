/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class ConeTypeApproximator(inferenceContext: ConeInferenceContext) : AbstractTypeApproximator(inferenceContext) {
    fun approximateToSuperType(type: ConeKotlinType, conf: TypeApproximatorConfiguration): ConeKotlinType? {
        return super.approximateToSuperType(type, conf) as ConeKotlinType?
    }

    fun approximateToSubType(type: ConeKotlinType, conf: TypeApproximatorConfiguration): ConeKotlinType? {
        return super.approximateToSubType(type, conf) as ConeKotlinType?
    }
}
