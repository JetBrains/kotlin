/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.getActualTargetList
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*

internal val KtSymbol.firSymbol: FirBasedSymbol<*>
    get() {
        require(this is KtFirSymbol<*>)
        return this.firSymbol
    }

internal val KtTypeParameterSymbol.firSymbol: FirTypeParameterSymbol get() = (this as KtFirSymbol<*>).firSymbol as FirTypeParameterSymbol
internal val KtTypeAliasSymbol.firSymbol: FirTypeAliasSymbol get() = (this as KtFirSymbol<*>).firSymbol as FirTypeAliasSymbol

internal val KtCallableSymbol.firSymbol: FirCallableSymbol<*> get() = (this as KtFirSymbol<*>).firSymbol as FirCallableSymbol<*>
internal val KtValueParameterSymbol.firSymbol: FirValueParameterSymbol get() = (this as KtFirSymbol<*>).firSymbol as FirValueParameterSymbol
internal val KtEnumEntrySymbol.firSymbol: FirEnumEntrySymbol get() = (this as KtFirSymbol<*>).firSymbol as FirEnumEntrySymbol
internal val KtConstructorSymbol.firSymbol: FirConstructorSymbol get() = (this as KtFirSymbol<*>).firSymbol as FirConstructorSymbol
internal val KtPropertyAccessorSymbol.firSymbol: FirPropertyAccessorSymbol get() = (this as KtFirSymbol<*>).firSymbol as FirPropertyAccessorSymbol
internal val KtClassInitializerSymbol.firSymbol: FirAnonymousInitializerSymbol get() = (this as KtFirSymbol<*>).firSymbol as FirAnonymousInitializerSymbol
internal val KtClassLikeSymbol.firSymbol: FirClassLikeSymbol<*> get() = (this as KtFirSymbol<*>).firSymbol as FirClassLikeSymbol<*>


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

fun KtSymbol.getContainingKtModule(firResolveSession: LLFirResolveSession): KtModule = when (this) {
    is KtFirSymbol<*> -> firSymbol.getContainingKtModule(firResolveSession)
    is KtReceiverParameterSymbol -> owningCallableSymbol.getContainingKtModule(firResolveSession)
    else -> TODO("${this::class}")
}

fun KtSymbol.getActualAnnotationTargets(): List<KotlinTarget>? {
    val firSymbol = this.firSymbol.fir as? FirAnnotationContainer ?: return null
    return getActualTargetList(firSymbol).defaultTargets
}