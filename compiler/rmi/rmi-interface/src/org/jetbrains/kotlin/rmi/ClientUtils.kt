/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.rmi

import java.io.File
import java.rmi.ConnectException
import java.rmi.registry.LocateRegistry


internal val MAX_PORT_NUMBER = 0xffff


enum class DaemonReportCategory {
    DEBUG, INFO, EXCEPTION;
}


fun makeRunFilenameString(timestamp: String, digest: String, port: String, escapeSequence: String = ""): String = "$COMPILE_DAEMON_DEFAULT_FILES_PREFIX$escapeSequence.$timestamp$escapeSequence.$digest$escapeSequence.$port$escapeSequence.run"


fun String.extractPortFromRunFilename(digest: String): Int =
        makeRunFilenameString(timestamp = "[0-9TZ:\\.\\+-]+", digest = digest, port = "(\\d+)", escapeSequence = "\\").toRegex()
                .find(this)
                ?.groups?.get(1)
                ?.value?.toInt()
        ?: 0


fun walkDaemons(registryDir: File, compilerId: CompilerId, report: (DaemonReportCategory, String) -> Unit): Sequence<CompileService> {
    val classPathDigest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString()
    return registryDir.walk()
            .map { Pair(it, it.name.extractPortFromRunFilename(classPathDigest)) }
            .filter { it.second != 0 }
            .map {
                assert(it.second > 0 && it.second < MAX_PORT_NUMBER)
                report(DaemonReportCategory.DEBUG, "found daemon on port ${it.second}, trying to connect")
                val daemon = tryConnectToDaemon(it.second, report)
                // cleaning orphaned file; note: daemon should shut itself down if it detects that the run file is deleted
                if (daemon == null && !it.first.delete()) {
                    report(DaemonReportCategory.INFO, "WARNING: unable to delete seemingly orphaned file '${it.first.absolutePath}', cleanup recommended")
                }
                daemon
            }
            .filterNotNull()
}


private inline fun tryConnectToDaemon(port: Int, report: (DaemonReportCategory, String) -> Unit): CompileService? {
    try {
        val daemon = LocateRegistry.getRegistry(LoopbackNetworkInterface.loopbackInetAddressName, port)
                ?.lookup(COMPILER_SERVICE_RMI_NAME)
        if (daemon != null)
            return daemon as? CompileService ?:
                   throw ClassCastException("Unable to cast compiler service, actual class received: ${daemon.javaClass}")
        report(DaemonReportCategory.EXCEPTION, "daemon not found")
    }
    catch (e: ConnectException) {
        report(DaemonReportCategory.EXCEPTION, "cannot connect to registry: " + (e.cause?.message ?: e.message ?: "unknown exception"))
        // ignoring it - processing below
    }
    return null
}
