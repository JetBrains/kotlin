/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * K1: [org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration]
 */
public class KtCallsKtEffectDeclaration(
    public val valueParameterReference: KtValueParameterReference,
    public val kind: EventOccurrencesRange
) : KtEffectDeclaration

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration]
 */
public class KtConditionalEffectDeclaration(public val effect: KtEffectDeclaration) : KtEffectDeclaration

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ReturnsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration]
 */
public class KtReturnsEffectDeclaration : KtEffectDeclaration
