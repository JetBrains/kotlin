/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.labelName
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValueForSnippet
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSnippetFromReplScope
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType

class TowerElementsForSnippet(
    val implicitReceivers: List<ImplicitReceiverValueForSnippet>,
    val staticScope: FirScope?,
)

// TODO: remove this abstraction
fun SessionHolder.collectTowerDataElementsForSnippet(owner: FirSnippet): TowerElementsForSnippet {
    owner.lazyResolveToPhase(FirResolvePhase.TYPES)

    val contextReceivers = owner.contextReceivers.mapIndexed { index, receiver ->
        ImplicitReceiverValueForSnippet(
            owner.symbol, receiver.typeRef.coneType, receiver.labelName, session, scopeSession,
            contextReceiverNumber = index,
        )
    }

    return TowerElementsForSnippet(
        contextReceivers,
        FirSnippetFromReplScope(session),
    )
}
