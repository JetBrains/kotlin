/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

internal class TowerInvokeResolveQuery(
    val towerLevel: SessionBasedTowerLevel,
    val callInfo: CallInfo,
    val explicitReceiverKind: ExplicitReceiverKind,
    val group: TowerGroup,
    val mode: InvokeResolveMode
) : Comparable<TowerInvokeResolveQuery> {
    override fun compareTo(other: TowerInvokeResolveQuery): Int {
        return group.compareTo(other.group)
    }
}

enum class InvokeResolveMode {
    IMPLICIT_CALL_ON_GIVEN_RECEIVER,
    RECEIVER_FOR_INVOKE_BUILTIN_EXTENSION
}