/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.resolution.symbols
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.resolution.KtResolvable

abstract class AbstractResolveReferenceSymbolTest : AbstractResolveReferenceTest() {
    override val resolveKind: String get() = "referencesSymbol"

    override fun KaSession.resolveReferenceToSymbols(reference: KtReference): Collection<KaSymbol> {
        return if (reference is KtResolvable) {
            reference.attemptResolveSymbol()?.symbols.orEmpty()
        } else {
            emptyList()
        }
    }
}
