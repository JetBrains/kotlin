/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

public abstract class KtTypeAndAnnotations : ValidityTokenOwner {
    public abstract val type: KtType
    public abstract val annotations: List<KtAnnotationCall>
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtTypeAndAnnotations

        if (token != other.token) return false
        if (type != other.type) return false
        if (annotations != other.annotations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + annotations.hashCode()
        return result
    }
}