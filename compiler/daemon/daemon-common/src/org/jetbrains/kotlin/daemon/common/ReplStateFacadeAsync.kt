/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.cli.common.repl.ILineId

interface ReplStateFacadeAsync {
    suspend fun getId(): Int

    suspend fun getHistorySize(): Int

    suspend fun historyGet(index: Int): ILineId

    suspend fun historyReset(): List<ILineId>

    suspend fun historyResetTo(id: ILineId): List<ILineId>
}
