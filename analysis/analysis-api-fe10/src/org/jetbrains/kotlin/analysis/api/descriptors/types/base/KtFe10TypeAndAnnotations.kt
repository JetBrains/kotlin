/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types.base

import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.types.KotlinType

internal class KtFe10TypeAndAnnotations(
    override val type: KtType,
    private val originalType: KotlinType,
    override val token: ValidityToken
) : KtTypeAndAnnotations() {
    override val annotations: List<KtAnnotationCall>
        get() = withValidityAssertion {
            originalType.annotations.map { KtFe10DescAnnotationCall(it, token) }
        }
}