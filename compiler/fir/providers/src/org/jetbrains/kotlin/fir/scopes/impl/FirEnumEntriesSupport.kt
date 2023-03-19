/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.hasEnumEntries
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.StandardClassIds

open class FirEnumEntriesSupport(val session: FirSession) : FirSessionComponent {
    protected val isEnumEntriesAvailable by lazy {
        session.symbolProvider.getRegularClassSymbolByClassId(StandardClassIds.EnumEntries) != null
    }

    open fun canSynthesizeEnumEntriesFor(klass: FirClass): Boolean {
        return klass.hasEnumEntries && isEnumEntriesAvailable
    }
}

class FirJvmEnumEntriesSupport(session: FirSession) : FirEnumEntriesSupport(session) {
    // In JVM modules "entries" can be called even on enum compiled without this property.
    override fun canSynthesizeEnumEntriesFor(klass: FirClass): Boolean = isEnumEntriesAvailable
}

val FirSession.enumEntriesSupport: FirEnumEntriesSupport by FirSession.sessionComponentAccessor()
