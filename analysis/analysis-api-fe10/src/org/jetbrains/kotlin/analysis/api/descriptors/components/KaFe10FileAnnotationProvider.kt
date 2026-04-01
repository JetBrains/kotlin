/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.components.KaFileAnnotationProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

internal class KaFe10FileAnnotationProvider(
    override val analysisSessionProvider: () -> KaFe10Session,
) : KaBaseSessionComponent<KaFe10Session>(), KaFileAnnotationProvider {
    override val KaDeclarationSymbol.fileAnnotations: KaAnnotationList
        get() = withValidityAssertion {
            throw NotImplementedError("Method is not implemented for FE 1.0")
        }
}
