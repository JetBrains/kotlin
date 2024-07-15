/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class ConeTypeApproximator(inferenceContext: ConeInferenceContext, languageVersionSettings: LanguageVersionSettings) :
    AbstractTypeApproximator(inferenceContext, languageVersionSettings) {
    fun approximateToSuperType(type: ConeKotlinType, conf: TypeApproximatorConfiguration): ConeKotlinType? {
        if (type.fastPathSkipApproximation()) return null
        return super.approximateToSuperType(type, conf) as ConeKotlinType?
    }

    fun approximateToSubType(type: ConeKotlinType, conf: TypeApproximatorConfiguration): ConeKotlinType? {
        if (type.fastPathSkipApproximation()) return null
        return super.approximateToSubType(type, conf) as ConeKotlinType?
    }

    private fun ConeKotlinType.fastPathSkipApproximation(): Boolean {
        return this is ConeClassLikeType && this.typeArguments.isEmpty() && this.lookupTag.let { !it.isLocalClass() && !it.isAnonymousClass() }
    }
}
