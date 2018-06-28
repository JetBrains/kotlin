/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.common.impls

import org.jetbrains.kotlin.cli.common.repl.ILineId
import java.rmi.Remote
import java.rmi.RemoteException

interface ReplStateFacade : Remote {

    @Throws(RemoteException::class)
    fun getId(): Int

    @Throws(RemoteException::class)
    fun getHistorySize(): Int

    @Throws(RemoteException::class)
    fun historyGet(index: Int): ILineId

    @Throws(RemoteException::class)
    fun historyReset(): List<ILineId>

    @Throws(RemoteException::class)
    fun historyResetTo(id: ILineId): List<ILineId>
}