/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolIdEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.reflect.KClass

internal fun FirBasedSymbol<*>.errorInLazyResolve(name: String, actualClass: KClass<*>, expected: KClass<*>): Nothing {
    errorWithAttachment("Unexpected $name. Expected is ${expected.simpleName}, but was ${actualClass.simpleName}") {
        withFirEntry("firElement", fir)
        withFirSymbolIdEntry("firSymbol", this@errorInLazyResolve)
    }
}

internal fun FirMemberDeclaration.resolvedStatus(): FirResolvedDeclarationStatus {
    lazyResolveToPhase(FirResolvePhase.STATUS)

    val status = status
    if (status !is FirResolvedDeclarationStatus) {
        symbol.errorInLazyResolve("status", status::class, FirResolvedDeclarationStatus::class)
    }

    return status
}