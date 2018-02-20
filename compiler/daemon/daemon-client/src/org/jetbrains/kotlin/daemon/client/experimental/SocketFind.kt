/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_FIND_PORT_ATTEMPTS
import org.jetbrains.kotlin.daemon.common.experimental.CALLBACK_SERVER_PORTS_RANGE_END
import org.jetbrains.kotlin.daemon.common.experimental.CALLBACK_SERVER_PORTS_RANGE_START
import org.jetbrains.kotlin.daemon.common.experimental.findPortForSocket

fun findCallbackServerSocket() = findPortForSocket(
    COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
    CALLBACK_SERVER_PORTS_RANGE_START,
    CALLBACK_SERVER_PORTS_RANGE_END
)