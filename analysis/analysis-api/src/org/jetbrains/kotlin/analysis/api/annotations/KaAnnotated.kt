/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * [KaAnnotated] represents an entity which may have annotations applied to it. All [types][org.jetbrains.kotlin.analysis.api.types.KaType]s
 * and almost all [symbols][org.jetbrains.kotlin.analysis.api.symbols.KaSymbol] can have annotations.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaAnnotated {
    /**
     * A list of annotations applied to the annotated entity.
     */
    public val annotations: KaAnnotationList

    public fun getExtraInfo(): List<String> {
        return emptyList()
    }
}
