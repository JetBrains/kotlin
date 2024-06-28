/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.Name
import java.util.Objects

class KaBaseNamedAnnotationValue(
    private val backingName: Name,
    private val backingExpression: KaAnnotationValue,
) : KaNamedAnnotationValue {
    override val token: KaLifetimeToken get() = backingExpression.token

    override val name: Name get() = withValidityAssertion { backingName }

    override val expression: KaAnnotationValue get() = withValidityAssertion { backingExpression }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KaBaseNamedAnnotationValue

        if (backingName != other.backingName) return false
        if (backingExpression != other.backingExpression) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(backingName, backingExpression)
    }

    override fun toString(): String {
        return "KaNamedAnnotationValue(name=$backingName, expression=$backingExpression)"
    }
}
