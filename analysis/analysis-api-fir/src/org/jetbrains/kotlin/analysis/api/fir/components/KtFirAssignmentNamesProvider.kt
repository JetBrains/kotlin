/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtAssignmentNamesProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.extensions.assignAltererExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression

internal class KtFirAssignmentNamesProvider(
    override val analysisSession: KtFirAnalysisSession,
) : KtAssignmentNamesProvider(), KtFirAnalysisSessionComponent {

    override fun getOperationName(expression: KtBinaryExpression): Name? {
        val firElement = expression.getOrBuildFir(analysisSession.firResolveSession)
        val errorReference = firElement as? FirErrorNamedReference ?: return null
        val altererExtensions = rootModuleSession.extensionService.assignAltererExtensions
        return altererExtensions.firstNotNullOfOrNull { it.getOperationName(errorReference) }
    }
}
