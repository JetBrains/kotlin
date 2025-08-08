/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator

/**
 * Provides an instance of [KaTypeCreator] to create various [types][org.jetbrains.kotlin.analysis.api.types.KaType].
 */
@SubclassOptInRequired(KaImplementationDetail::class)
@KaExperimentalApi
public interface KaTypeCreatorProvider : KaSessionComponent {
    /**
     * A single entry point for the type building infrastructure.
     */
    public val typeCreator: KaTypeCreator
}
