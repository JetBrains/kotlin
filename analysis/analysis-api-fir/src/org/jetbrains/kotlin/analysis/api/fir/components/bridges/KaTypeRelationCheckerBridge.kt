/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.analysis.api.types.KaSubtypingErrorTypePolicy as KaEndpointSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.isSubtypeOf as isSubtypeOfEndpoint
import org.jetbrains.kotlin.analysis.api.types.semanticallyEquals as semanticallyEqualsEndpoint

internal class KaTypeRelationCheckerBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaTypeRelationChecker {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        semanticallyEqualsEndpoint(other, errorTypePolicy.toEndpointPolicy(), session = analysisSession)

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        isSubtypeOfEndpoint(supertype, errorTypePolicy.toEndpointPolicy(), session = analysisSession)

    override fun KaType.isSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        isSubtypeOfEndpoint(classId, errorTypePolicy.toEndpointPolicy(), session = analysisSession)

    override fun KaType.isSubtypeOf(symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        isSubtypeOfEndpoint(symbol, errorTypePolicy.toEndpointPolicy(), session = analysisSession)
}

private fun KaSubtypingErrorTypePolicy.toEndpointPolicy(): KaEndpointSubtypingErrorTypePolicy = when (this) {
    KaSubtypingErrorTypePolicy.STRICT -> KaEndpointSubtypingErrorTypePolicy.STRICT
    KaSubtypingErrorTypePolicy.LENIENT -> KaEndpointSubtypingErrorTypePolicy.LENIENT
}
