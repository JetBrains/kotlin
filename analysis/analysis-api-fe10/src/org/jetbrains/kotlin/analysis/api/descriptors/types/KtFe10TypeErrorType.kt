/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtTypeErrorType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeKind

internal class KtFe10TypeErrorType(
    override val fe10Type: ErrorType,
    override val analysisContext: Fe10AnalysisContext
) : KtTypeErrorType(), KtFe10Type {
    init {
        check(!fe10Type.kind.isUnresolved) {
            "Expected unresolved ErrorType but ${fe10Type.kind} found for $fe10Type"
        }
    }

    override fun tryRenderAsNonErrorType(): String? = withValidityAssertion {
        when (fe10Type.kind) {
            ErrorTypeKind.UNINFERRED_TYPE_VARIABLE -> fe10Type.formatParams.first()
            else -> null
        }
    }


    override fun asStringForDebugging(): String = withValidityAssertion { fe10Type.asStringForDebugging() }

    override val errorMessage: String
        get() = withValidityAssertion { fe10Type.debugMessage }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }
}