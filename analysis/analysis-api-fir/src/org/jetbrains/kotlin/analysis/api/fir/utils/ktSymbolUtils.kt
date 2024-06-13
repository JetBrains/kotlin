/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*

internal val KaSymbol.firSymbol: FirBasedSymbol<*>
    get() {
        // Currently, KaFirReceiverParameterSymbol is not KaFirSymbol
        requireIsInstance<KaFirSymbol<*>>(this)
        return this.firSymbol
    }

internal val KaTypeParameterSymbol.firSymbol: FirTypeParameterSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirTypeParameterSymbol
internal val KaTypeAliasSymbol.firSymbol: FirTypeAliasSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirTypeAliasSymbol

internal val KaCallableSymbol.firSymbol: FirCallableSymbol<*> get() = (this as KaFirSymbol<*>).firSymbol as FirCallableSymbol<*>
internal val KaValueParameterSymbol.firSymbol: FirValueParameterSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirValueParameterSymbol
internal val KaEnumEntrySymbol.firSymbol: FirEnumEntrySymbol get() = (this as KaFirSymbol<*>).firSymbol as FirEnumEntrySymbol
internal val KaConstructorSymbol.firSymbol: FirConstructorSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirConstructorSymbol
internal val KaPropertyAccessorSymbol.firSymbol: FirPropertyAccessorSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirPropertyAccessorSymbol
internal val KaClassInitializerSymbol.firSymbol: FirAnonymousInitializerSymbol get() = (this as KaFirSymbol<*>).firSymbol as FirAnonymousInitializerSymbol
internal val KaClassLikeSymbol.firSymbol: FirClassLikeSymbol<*> get() = (this as KaFirSymbol<*>).firSymbol as FirClassLikeSymbol<*>
internal val KaClassOrObjectSymbol.firSymbol: FirClassSymbol<*> get() = (this as KaFirSymbol<*>).firSymbol as FirClassSymbol<*>


fun FirBasedSymbol<*>.getContainingKtModule(firResolveSession: LLFirResolveSession): KtModule {
    val target = when (this) {
        is FirCallableSymbol -> {
            // callable fake overrides have use-site FirModuleData
            dispatchReceiverClassLookupTagOrNull()?.toFirRegularClassSymbol(firResolveSession.useSiteFirSession) ?: this
        }
        else -> this
    }
    return target.llFirModuleData.ktModule
}

fun KaSymbol.getContainingKtModule(firResolveSession: LLFirResolveSession): KtModule = when (this) {
    is KaFirSymbol<*> -> firSymbol.getContainingKtModule(firResolveSession)
    is KaReceiverParameterSymbol -> owningCallableSymbol.getContainingKtModule(firResolveSession)
    else -> TODO("${this::class}")
}

fun KaSymbol.getActualAnnotationTargets(): List<KotlinTarget>? {
    val firSymbol = this.firSymbol.fir as? FirAnnotationContainer ?: return null
    return getActualTargetList(firSymbol).defaultTargets
}