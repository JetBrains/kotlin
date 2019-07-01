/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import java.util.concurrent.TimeUnit

val RMI_WRAPPER_PORTS_RANGE_START: Int = 13001
val RMI_WRAPPER_PORTS_RANGE_END: Int = 14000

val REPL_SERVER_PORTS_RANGE_START: Int = 14001
val REPL_SERVER_PORTS_RANGE_END: Int = 15000


val CALLBACK_SERVER_PORTS_RANGE_START: Int = 15001
val CALLBACK_SERVER_PORTS_RANGE_END: Int = 16000

val RESULTS_SERVER_PORTS_RANGE_START: Int = 16001
val RESULTS_SERVER_PORTS_RANGE_END: Int = 17000

val COMPILER_DAEMON_CLASS_FQN_EXPERIMENTAL: String = "org.jetbrains.kotlin.daemon.experimental.KotlinCompileDaemon"

val FIRST_HANDSHAKE_BYTE_TOKEN = byteArrayOf(1, 2, 3, 4)
val AUTH_TIMEOUT_IN_MILLISECONDS = 200L

val DAEMON_PERIODIC_CHECK_INTERVAL_MS = 1000L
val DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS = 60000L

val KEEPALIVE_PERIOD = 2000L
val KEEPALIVE_PERIOD_SERVER = 4000L