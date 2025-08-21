/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*

internal val KaSymbol.firSymbol: FirBasedSymbol<*>
    get() {
        requireIsInstance<KaFirSymbol<*>>(this)
        return this.firSymbol
    }

internal val KaTypeParameterSymbol.firSymbol: FirTypeParameterSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirTypeParameterSymbol
internal val KaTypeAliasSymbol.firSymbol: FirTypeAliasSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirTypeAliasSymbol

internal val KaCallableSymbol.firSymbol: FirCallableSymbol<*> get() = (this as KaFirSymbol<*>).firSymbol as FirCallableSymbol<*>
internal val KaValueParameterSymbol.firSymbol: FirValueParameterSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirValueParameterSymbol
internal val KaContextParameterSymbol.firSymbol: FirValueParameterSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirValueParameterSymbol
internal val KaEnumEntrySymbol.firSymbol: FirEnumEntrySymbol get() = (this as KaFirSymbol<*>).firSymbol as FirEnumEntrySymbol
internal val KaConstructorSymbol.firSymbol: FirConstructorSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirConstructorSymbol
internal val KaPropertyAccessorSymbol.firSymbol: FirPropertyAccessorSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirPropertyAccessorSymbol
internal val KaClassInitializerSymbol.firSymbol: FirAnonymousInitializerSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirAnonymousInitializerSymbol
internal val KaClassLikeSymbol.firSymbol: FirClassLikeSymbol<*> get() = (this as KaFirSymbol<*>).firSymbol as FirClassLikeSymbol<*>
internal val KaClassSymbol.firSymbol: FirClassSymbol<*> get() = (this as KaFirSymbol<*>).firSymbol as FirClassSymbol<*>


internal fun FirBasedSymbol<*>.getContainingKtModule(resolutionFacade: LLResolutionFacade): KaModule {
    val target = when (this) {
        is FirCallableSymbol -> {
            // callable fake overrides have use-site FirModuleData
            dispatchReceiverClassLookupTagOrNull()?.toRegularClassSymbol(resolutionFacade.useSiteFirSession) ?: this
        }
        else -> this
    }
    return target.llFirModuleData.ktModule
}

internal fun KaSymbol.getContainingKtModule(resolutionFacade: LLResolutionFacade): KaModule = when (this) {
    is KaFirSymbol<*> -> firSymbol.getContainingKtModule(resolutionFacade)
    is KaReceiverParameterSymbol -> owningCallableSymbol.getContainingKtModule(resolutionFacade)
    else -> TODO("${this::class}")
}

@KaImplementationDetail
fun KaSymbol.getActualAnnotationTargets(): List<KotlinTarget>? {
    val firSymbol = this.firSymbol.fir as? FirAnnotationContainer ?: return null
    return getActualTargetList(firSymbol).defaultTargets
}