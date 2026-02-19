/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.typeCreation.KaFe10TypeCreator
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator


internal class KaFe10TypeCreatorProvider(
    override val analysisSessionProvider: () -> KaFe10Session,
) : KaTypeCreatorProvider, KaBaseSessionComponent<KaFe10Session>(), KaFe10SessionComponent {
    val backingTypeCreator by lazy { KaFe10TypeCreator(analysisSession) }

    override val typeCreator: KaTypeCreator
        get() = withValidityAssertion {
            backingTypeCreator
        }
}