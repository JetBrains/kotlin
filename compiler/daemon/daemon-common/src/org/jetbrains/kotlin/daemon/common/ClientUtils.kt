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

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.rmi.registry.LocateRegistry


internal val MAX_PORT_NUMBER = 0xffff


enum class DaemonReportCategory {
    DEBUG, INFO, EXCEPTION;
}


fun makeRunFilenameString(timestamp: String, digest: String, port: String, escapeSequence: String = ""): String =
        "$COMPILE_DAEMON_DEFAULT_FILES_PREFIX$escapeSequence.$timestamp$escapeSequence.$digest$escapeSequence.$port$escapeSequence.run"


fun makePortFromRunFilenameExtractor(digest: String): (String) -> Int? {
    val regex = makeRunFilenameString(timestamp = "[0-9TZ:\\.\\+-]+", digest = digest, port = "(\\d+)", escapeSequence = "\\").toRegex()
    return { regex.find(it)
             ?.groups?.get(1)
             ?.value?.toInt()
    }
}


fun walkDaemons(registryDir: File,
                compilerId: CompilerId,
                filter: (File, Int) -> Boolean = { f, p -> true },
                report: (DaemonReportCategory, String) -> Unit = { cat, msg -> }
): Sequence<CompileService> {
    val classPathDigest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString()
    val portExtractor = makePortFromRunFilenameExtractor(classPathDigest)
    return registryDir.walk()
            .map { Pair(it, portExtractor(it.name)) }
            .filter { it.second != null && filter(it.first, it.second!!) }
            .mapNotNull {
                assert(it.second!! > 0 && it.second!! < MAX_PORT_NUMBER)
                report(DaemonReportCategory.DEBUG, "found daemon on port ${it.second}, trying to connect")
                val daemon = tryConnectToDaemon(it.second!!, report)
                // cleaning orphaned file; note: daemon should shut itself down if it detects that the run file is deleted
                if (daemon == null && !it.first.delete()) {
                    report(DaemonReportCategory.INFO, "WARNING: unable to delete seemingly orphaned file '${it.first.absolutePath}', cleanup recommended")
                }
                daemon
            }
}


private inline fun tryConnectToDaemon(port: Int, report: (DaemonReportCategory, String) -> Unit): CompileService? {

    try {
        val daemon = LocateRegistry.getRegistry(LoopbackNetworkInterface.loopbackInetAddressName, port, LoopbackNetworkInterface.clientLoopbackSocketFactory)
                ?.lookup(COMPILER_SERVICE_RMI_NAME)
        when (daemon) {
            null -> report(DaemonReportCategory.EXCEPTION, "daemon not found")
            is CompileService -> return daemon
            else -> report(DaemonReportCategory.EXCEPTION, "Unable to cast compiler service, actual class received: ${daemon.javaClass.name}")
        }
    }
    catch (e: Throwable) {
        report(DaemonReportCategory.EXCEPTION, "cannot connect to registry: " + (e.cause?.message ?: e.message ?: "unknown error"))
    }
    return null
}

private const val validFlagFileKeywordChars = "abcdefghijklmnopqrstuvwxyz0123456789-_"

fun makeAutodeletingFlagFile(keyword: String = "compiler-client", baseDir: File? = null): File {
    val flagFile = File.createTempFile("kotlin-${keyword.filter { validFlagFileKeywordChars.contains(it.toLowerCase()) }}-", "-is-running", baseDir?.takeIf { it.isDirectory && it.exists() })
    flagFile.deleteOnExit()
    return flagFile
}
