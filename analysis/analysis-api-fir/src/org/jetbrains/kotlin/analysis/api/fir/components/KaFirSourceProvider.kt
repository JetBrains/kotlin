/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSourceProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.klibFileAnnotations
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile
import org.jetbrains.kotlin.name.ClassId

internal class KaFirSourceProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaSourceProvider {
    override val KaDeclarationSymbol.klibSourceFileName: String?
        get() = withValidityAssertion {
            firSymbol.klibSourceFile?.name
        }

    override val KaDeclarationSymbol.klibFileAnnotationClassIds: List<ClassId>?
        get() = withValidityAssertion {
            firSymbol.klibFileAnnotations?.mapNotNull { it.toAnnotationClassId(analysisSession.firSession) }
        }
}
