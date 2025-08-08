/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.typeCreation.KaFirTypeCreator
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator


internal class KaFirTypeCreatorProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaTypeCreatorProvider, KaBaseSessionComponent<KaFirSession>(), KaFirSessionComponent {
    val backingTypeCreator by lazy { KaFirTypeCreator(analysisSession) }

    override val typeCreator: KaTypeCreator
        get() = withValidityAssertion {
            backingTypeCreator
        }
}