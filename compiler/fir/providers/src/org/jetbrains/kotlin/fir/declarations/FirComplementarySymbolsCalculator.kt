/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.isJavaNonAbstractSealed
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

fun FirClassSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirClassSymbol<*>> {
    return mutableSetOf<FirClassSymbol<*>>().apply { collectAllSubclassesTo(this, session) }
}

private fun FirClassSymbol<*>.collectAllSubclassesTo(
    destination: MutableSet<FirClassSymbol<*>>,
    session: FirSession,
    visited: MutableSet<FirRegularClassSymbol> = mutableSetOf(),
) {
    if (this !is FirRegularClassSymbol) {
        destination.add(this)
        return
    }
    if (!visited.add(this)) return
    when {
        fir.modality == Modality.SEALED -> {
            if (fir.isJavaNonAbstractSealed == true) {
                destination.add(this)
            }

            fir.getSealedClassInheritors(session).forEach {
                val symbol = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol
                symbol?.collectAllSubclassesTo(destination, session, visited)
            }
        }
        else -> destination.add(this)
    }
}
