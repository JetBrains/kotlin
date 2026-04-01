/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseKDocProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

internal class KaFe10KDocProvider(
    override val analysisSessionProvider: () -> KaFe10Session,
) : KaBaseKDocProvider<KaFe10Session>() {
    override fun findDeserializedKdocText(symbol: KaDeclarationSymbol): String? {
        return null
    }
}