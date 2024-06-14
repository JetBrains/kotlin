/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.Name
import java.util.Objects

/**
 * Name-Value pair which is used as annotation argument.
 */
public class KaNamedAnnotationValue(
    name: Name,
    expression: KaAnnotationValue,
    override val token: KaLifetimeToken
) : KaLifetimeOwner {
    public val name: Name = name
        get() = withValidityAssertion { field }

    public val expression: KaAnnotationValue = expression
        get() = withValidityAssertion { field }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KaNamedAnnotationValue

        if (name != other.name) return false
        if (expression != other.expression) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(name, expression)
    }

    override fun toString(): String {
        return "KaNamedAnnotationValue(name=$name, expression=$expression)"
    }
}

@Deprecated("Use 'KaNamedAnnotationValue' instead", ReplaceWith("KaNamedAnnotationValue"))
public typealias KtNamedAnnotationValue = KaNamedAnnotationValue