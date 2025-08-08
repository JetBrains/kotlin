/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator

@KaImplementationDetail
class KaBaseTypeCreatorProvider(
    override val analysisSessionProvider: () -> KaSession,
    private val backingTypeCreator: KaTypeCreator,
) : KaTypeCreatorProvider, KaBaseSessionComponent<KaSession>() {
    override val typeCreator: KaTypeCreator
        get() = withValidityAssertion {
            backingTypeCreator
        }
}
