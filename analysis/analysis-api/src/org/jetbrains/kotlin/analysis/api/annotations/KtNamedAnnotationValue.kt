/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.name.Name
import java.util.Objects

/**
 * Name-Value pair which is used as annotation argument.
 */
public class KtNamedAnnotationValue(
    public val name: Name,
    public val expression: KtAnnotationValue
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtNamedAnnotationValue

        if (name != other.name) return false
        if (expression != other.expression) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(name, expression)
    }

    override fun toString(): String {
        return "KtNamedAnnotationValue(name=$name, expression=$expression)"
    }
}