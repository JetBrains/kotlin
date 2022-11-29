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
    override val type: ErrorType,
    override val analysisContext: Fe10AnalysisContext
) : KtTypeErrorType(), KtFe10Type {
    init {
        check(!type.kind.isUnresolved) {
            "Expected unresolved ErrorType but ${type.kind} found for $type"
        }
    }

    override fun tryRenderAsNonErrorType(): String? = withValidityAssertion {
        when (type.kind) {
            ErrorTypeKind.UNINFERRED_TYPE_VARIABLE -> type.formatParams.first()
            else -> null
        }
    }


    override fun asStringForDebugging(): String = withValidityAssertion { type.asStringForDebugging() }

    override val errorMessage: String
        get() = withValidityAssertion { type.debugMessage }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { type.ktNullability }
}