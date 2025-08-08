/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator

/**
 * Provides an instance of [KaTypeCreator] to create various [types][org.jetbrains.kotlin.analysis.api.types.KaType].
 */
@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaTypeCreatorProvider : KaSessionComponent {
    /**
     * A single entry point for the type building infrastructure.
     */
    @KaExperimentalApi
    public val typeCreator: KaTypeCreator
}

/**
 * A single entry point for the type building infrastructure.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public val typeCreator: KaTypeCreator
    get() = with(s) { typeCreator }
