/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KtFirDefinitelyNotNullType(
    override val coneType: ConeDefinitelyNotNullType,
    private val builder: KtSymbolByFirBuilder,
) : KtDefinitelyNotNullType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token
    override val original: KtType = withValidityAssertion { builder.typeBuilder.buildKtType(this.coneType.original) }
    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder)
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
