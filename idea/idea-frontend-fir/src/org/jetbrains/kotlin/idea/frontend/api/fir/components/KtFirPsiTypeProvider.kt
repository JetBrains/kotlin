/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.idea.asJava.asPsiType
import org.jetbrains.kotlin.idea.frontend.api.components.KtPsiTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode

internal class KtFirPsiTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtPsiTypeProvider(), KtFirAnalysisSessionComponent {

    override fun asPsiType(type: KtType, context: PsiElement, mode: TypeMappingMode): PsiType = withValidityAssertion {
        type.coneType.asPsiType(rootModuleSession, analysisSession.firResolveState, mode, context)
    }
}
