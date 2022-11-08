/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.labelName
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValueForScript
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirScriptDeclarationsScope
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType

class TowerElementsForScript(
//    val thisReceiver: ImplicitReceiverValue<*>, // script should not have implicit 'this'?
    val implicitReceivers: List<ImplicitReceiverValueForScript>,
    val staticScope: FirScope?,
//    val companionReceiver: ImplicitReceiverValue<*>?, // script should not have companions
//    val companionStaticScope: FirScope?,
    // Ordered from inner scopes to outer scopes.
//    val superClassesStaticsAndCompanionReceivers: List<FirTowerDataElement>, // scripts should not have superclasses
    // Ordered from inner scopes to outer scopes.
//    val implicitCompanionValues: List<ImplicitReceiverValue<*>>
)

fun SessionHolder.collectTowerDataElementsForScript(owner: FirScript): TowerElementsForScript {
    val contextReceivers = owner.contextReceivers.mapIndexed { index, receiver ->
        ImplicitReceiverValueForScript(
            owner.symbol, receiver.typeRef.coneType, receiver.labelName, session, scopeSession,
            contextReceiverNumber = index,
        )
    }

    return TowerElementsForScript(
        contextReceivers,
        FirScriptDeclarationsScope(session, owner),
    )
}
