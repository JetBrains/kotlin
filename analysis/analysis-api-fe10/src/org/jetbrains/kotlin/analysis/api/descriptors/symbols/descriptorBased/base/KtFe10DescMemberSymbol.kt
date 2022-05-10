/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base

import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

internal interface KtFe10DescMemberSymbol<T : MemberDescriptor> :
    KtFe10DescSymbol<T>, KtSymbolWithVisibility, KtSymbolWithModality, KtSymbolWithKind {
    override val modality: Modality
        get() = withValidityAssertion { descriptor.ktModality }

    override val visibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }
}