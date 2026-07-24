/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes
import org.jetbrains.kotlin.analysis.api.components.KaTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.dispatchReceiverType
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.analysis.api.types.allSupertypes as allSupertypesEndpoint
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSubtype as approximateToDenotableSubtypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSupertype as approximateToDenotableSupertypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.arrayElementType as arrayElementTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.augmentedByWarningLevelAnnotations as augmentedByWarningLevelAnnotationsEndpoint
import org.jetbrains.kotlin.analysis.api.types.builtinTypes as builtinTypesEndpoint
import org.jetbrains.kotlin.analysis.api.types.commonSupertype as commonSupertypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.defaultType as defaultTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.defaultTypeWithStarProjections as defaultTypeWithStarProjectionsEndpoint
import org.jetbrains.kotlin.analysis.api.types.directSupertypes as directSupertypesEndpoint
import org.jetbrains.kotlin.analysis.api.types.hasCommonSubtypeWith as hasCommonSubtypeWithEndpoint
import org.jetbrains.kotlin.analysis.api.types.receiverType as receiverTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.type as typeEndpoint
import org.jetbrains.kotlin.analysis.api.types.varargArrayType as varargArrayTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.withNullability as withNullabilityEndpoint

/**
 * Routes the legacy [KaTypeProvider] surface through the new public `context(session: KaSession)` type endpoints, which in turn reach the
 * [org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeProvider] proxy.
 *
 * The deprecated/hidden members ([approximateToSuperPublicDenotable], [withNullability] with [KaTypeNullability][org.jetbrains.kotlin.analysis.api.types.KaTypeNullability],
 * [KaNamedClassSymbol.defaultType][org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol], [dispatchReceiverType]) and
 * [collectImplicitReceiverTypes] (KT-75549) are intentionally not migrated to endpoints; they keep their original bodies here.
 */
internal class KaTypeProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaTypeProvider, KaFirSessionComponent {
    override val builtinTypes: KaBuiltinTypes
        get() = context(analysisSession) {
            // The endpoint returns the new types.KaBuiltinTypes. Its only implementation also extends the legacy
            // components.KaBuiltinTypes, so narrowing the result back to the legacy surface is safe.
            builtinTypesEndpoint as KaBuiltinTypes
        }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun KaType.approximateToSuperPublicDenotable(approximateLocalTypes: Boolean): KaType? =
        context(analysisSession) { approximateToDenotableSupertypeEndpoint(!approximateLocalTypes) }

    override fun KaType.approximateToDenotableSupertype(allowLocalDenotableTypes: Boolean): KaType? =
        context(analysisSession) { approximateToDenotableSupertypeEndpoint(allowLocalDenotableTypes) }

    override fun KaType.approximateToDenotableSubtype(): KaType? =
        context(analysisSession) { approximateToDenotableSubtypeEndpoint() }

    override fun KaType.approximateToDenotableSupertype(position: KtElement): KaType? =
        context(analysisSession) { approximateToDenotableSupertypeEndpoint(position) }

    override val KaType.augmentedByWarningLevelAnnotations: KaType
        get() = context(analysisSession) { augmentedByWarningLevelAnnotationsEndpoint }

    override val KaClassifierSymbol.defaultType: KaType
        get() = context(analysisSession) { defaultTypeEndpoint }

    override val KaClassifierSymbol.defaultTypeWithStarProjections: KaType
        get() = context(analysisSession) { defaultTypeWithStarProjectionsEndpoint }

    override val KaValueParameterSymbol.varargArrayType: KaType?
        get() = context(analysisSession) { varargArrayTypeEndpoint }

    override val Iterable<KaType>.commonSupertype: KaType
        get() = context(analysisSession) { commonSupertypeEndpoint }

    override val KtTypeReference.type: KaType
        get() = context(analysisSession) { typeEndpoint }

    override val KtDoubleColonExpression.receiverType: KaType?
        get() = context(analysisSession) { receiverTypeEndpoint }

    override fun KaType.withNullability(isMarkedNullable: Boolean): KaType =
        context(analysisSession) { withNullabilityEndpoint(isMarkedNullable) }

    override fun KaType.hasCommonSubtypeWith(that: KaType): Boolean =
        context(analysisSession) { hasCommonSubtypeWithEndpoint(that) }

    override fun KaType.directSupertypes(shouldApproximate: Boolean): Sequence<KaType> =
        context(analysisSession) { directSupertypesEndpoint(shouldApproximate) }

    override fun KaType.allSupertypes(shouldApproximate: Boolean): Sequence<KaType> =
        context(analysisSession) { allSupertypesEndpoint(shouldApproximate) }

    override val KaType.arrayElementType: KaType?
        get() = context(analysisSession) { arrayElementTypeEndpoint }

    override fun collectImplicitReceiverTypes(position: KtElement): List<KaType> =
        analysisSession.typeProvider.collectImplicitReceiverTypes(position)

    // Instantly deprecated, not migrated to an endpoint: keeps its original FIR implementation here on the legacy path.
    @Suppress("OVERRIDE_DEPRECATION")
    override val KaCallableSymbol.dispatchReceiverType: KaType?
        get() = withValidityAssertion {
            when (this) {
                is KaReceiverParameterSymbol -> null
                else -> {
                    require(this is KaFirSymbol<*>)
                    val firSymbol = firSymbol
                    check(firSymbol is FirCallableSymbol<*>) {
                        "Fir declaration should be FirCallableDeclaration; instead it was ${firSymbol::class}"
                    }
                    return firSymbol.dispatchReceiverType(analysisSession.firSymbolBuilder)
                }
            }
        }
}
