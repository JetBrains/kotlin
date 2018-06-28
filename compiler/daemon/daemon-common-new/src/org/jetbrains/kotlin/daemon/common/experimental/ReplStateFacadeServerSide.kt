/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.cli.common.repl.ILineId
import org.jetbrains.kotlin.daemon.common.ReplStateFacadeAsync
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server

interface ReplStateFacadeServerSide: ReplStateFacadeAsync, Server<ReplStateFacadeServerSide> {

    // Query messages:
    class GetIdMessage : Server.Message<ReplStateFacadeServerSide>() {
        override suspend fun processImpl(server: ReplStateFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.getId())
    }

    class GetHistorySizeMessage : Server.Message<ReplStateFacadeServerSide>() {
        override suspend fun processImpl(server: ReplStateFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.getHistorySize())
    }

    class HistoryGetMessage(val index: Int) : Server.Message<ReplStateFacadeServerSide>() {
        override suspend fun processImpl(server: ReplStateFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.historyGet(index))
    }

    class HistoryResetMessage : Server.Message<ReplStateFacadeServerSide>() {
        override suspend fun processImpl(server: ReplStateFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.historyReset())
    }

    class HistoryResetToMessage(val id: ILineId) : Server.Message<ReplStateFacadeServerSide>() {
        override suspend fun processImpl(server: ReplStateFacadeServerSide, printObject: (Any?) -> Unit) =
            printObject(server.historyResetTo(id))
    }
}