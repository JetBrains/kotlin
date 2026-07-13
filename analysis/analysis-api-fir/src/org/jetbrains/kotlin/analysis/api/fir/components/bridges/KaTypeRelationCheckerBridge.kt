/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.components.KaTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.analysis.api.types.KaSubtypingErrorTypePolicy as KaEndpointSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.isSubtypeOf as isSubtypeOfEndpoint
import org.jetbrains.kotlin.analysis.api.types.semanticallyEquals as semanticallyEqualsEndpoint

internal class KaTypeRelationCheckerBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaTypeRelationChecker {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        context(analysisSession) { semanticallyEqualsEndpoint(other, errorTypePolicy.toEndpointPolicy()) }

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        context(analysisSession) { isSubtypeOfEndpoint(supertype, errorTypePolicy.toEndpointPolicy()) }

    override fun KaType.isSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        context(analysisSession) { isSubtypeOfEndpoint(classId, errorTypePolicy.toEndpointPolicy()) }

    override fun KaType.isSubtypeOf(symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        context(analysisSession) { isSubtypeOfEndpoint(symbol, errorTypePolicy.toEndpointPolicy()) }
}

private fun KaSubtypingErrorTypePolicy.toEndpointPolicy(): KaEndpointSubtypingErrorTypePolicy = when (this) {
    KaSubtypingErrorTypePolicy.STRICT -> KaEndpointSubtypingErrorTypePolicy.STRICT
    KaSubtypingErrorTypePolicy.LENIENT -> KaEndpointSubtypingErrorTypePolicy.LENIENT
}
