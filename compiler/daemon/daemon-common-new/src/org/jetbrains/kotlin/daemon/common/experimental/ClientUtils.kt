/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental


import kotlinx.coroutines.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.runWithTimeout
import org.jetbrains.kotlin.daemon.common.DaemonReportCategory
import org.jetbrains.kotlin.daemon.common.makePortFromRunFilenameExtractor
import java.io.File
import java.rmi.registry.LocateRegistry
import java.util.logging.Logger

/*
1) walkDaemonsAsync = walkDaemons + some async calls inside (also some used classes changed *** -> ***Async)
2) tryConnectToDaemonBySockets / tryConnectToDaemonByRMI

*/

internal val MAX_PORT_NUMBER = 0xffff

private const val ORPHANED_RUN_FILE_AGE_THRESHOLD_MS = 1000000L

data class DaemonWithMetadataAsync(val daemon: CompileServiceAsync, val runFile: File, val jvmOptions: DaemonJVMOptions)

val log = Logger.getLogger("client utils")

// TODO: replace mapNotNull in walkDaemonsAsync with this method.
private suspend fun <T, R : Any> List<T>.mapNotNullAsync(transform: suspend (T) -> R?): List<R> =
    this
        .map { GlobalScope.async { transform(it) } }
        .mapNotNull { it.await() } // await for completion of the last action


// TODO: write metadata into discovery file to speed up selection
// TODO: consider using compiler jar signature (checksum) as a CompilerID (plus java version, plus ???) instead of classpath checksum
//    would allow to use same compiler from taken different locations
//    reqs: check that plugins (or anything els) should not be part of the CP
suspend fun walkDaemonsAsync(
    registryDir: File,
    compilerId: CompilerId,
    fileToCompareTimestamp: File,
    filter: (File, Int) -> Boolean = { _, _ -> true },
    report: (DaemonReportCategory, String) -> Unit = { _, _ -> },
    useRMI: Boolean = true,
    useSockets: Boolean = true
): List<DaemonWithMetadataAsync> { // TODO: replace with Deferred<List<DaemonWithMetadataAsync>> and use mapNotNullAsync to speed this up
    val classPathDigest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString()
    val portExtractor = makePortFromRunFilenameExtractor(classPathDigest)
    return registryDir.walk().toList() // list, since walk returns Sequence and Sequence.map{...} is not inline => coroutines dont work
        .map { Pair(it, portExtractor(it.name)) }
        .filter { (file, port) -> port != null && filter(file, port) }
        .mapNotNull { (file, port) ->
            // all actions process concurrently
            assert(port!! in 1..(MAX_PORT_NUMBER - 1))
            val relativeAge = fileToCompareTimestamp.lastModified() - file.lastModified()
            val daemon = tryConnectToDaemonAsync(port, report, file, useRMI, useSockets)
            // cleaning orphaned file; note: daemon should shut itself down if it detects that the runServer file is deleted
            if (daemon == null) {
                if (relativeAge - ORPHANED_RUN_FILE_AGE_THRESHOLD_MS <= 0) {
                    report(
                        DaemonReportCategory.DEBUG,
                        "found fresh runServer file '${file.absolutePath}' ($relativeAge ms old), but no daemon, ignoring it"
                    )
                } else {
                    report(
                        DaemonReportCategory.DEBUG,
                        "found seemingly orphaned runServer file '${file.absolutePath}' ($relativeAge ms old), deleting it"
                    )
                    if (!file.delete()) {
                        report(
                            DaemonReportCategory.INFO,
                            "WARNING: unable to delete seemingly orphaned file '${file.absolutePath}', cleanup recommended"
                        )
                    }
                }
            }
            try {
                daemon?.let {
                    DaemonWithMetadataAsync(it, file, it.getDaemonJVMOptions().get())
                }
            } catch (e: Exception) {
                report(
                    DaemonReportCategory.INFO,
                    "ERROR: unable to retrieve daemon JVM options, assuming daemon is dead: ${e.message}"
                )
                null
            }
        }
}

private inline fun tryConnectToDaemonByRMI(port: Int, report: (DaemonReportCategory, String) -> Unit): CompileServiceAsync? {
    try {
        log.info("tryConnectToDaemonByRMI(port = $port)")
        val daemon = runBlocking {
            runWithTimeout(2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
                LocateRegistry.getRegistry(
                    org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface.loopbackInetAddressName,
                    port,
                    org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface.clientLoopbackSocketFactory
                )?.lookup(COMPILER_SERVICE_RMI_NAME)
            }
        }
        when (daemon) {
            null -> report(DaemonReportCategory.INFO, "daemon not found")
            is CompileService -> return daemon.toClient()
            else -> report(DaemonReportCategory.INFO, "Unable to cast compiler service, actual class received: ${daemon::class.java.name}")
        }
    } catch (e: Throwable) {
        report(DaemonReportCategory.INFO, "cannot connect to registry: " + (e.cause?.message ?: e.message ?: "unknown error"))
    }
    return null
}

private suspend fun tryConnectToDaemonBySockets(
    port: Int,
    file: File,
    report: (DaemonReportCategory, String) -> Unit
): CompileServiceClientSide? {
    return CompileServiceClientSideImpl(
        port,
        LoopbackNetworkInterface.loopbackInetAddressName,
        file
    ).let { daemon ->
        try {
            log.info("tryConnectToDaemonBySockets(port = $port)")
            log.info("daemon($port) = $daemon")
            log.info("daemon($port) connecting to server...")
            daemon.connectToServer()
            log.info("OK - daemon($port) connected to server!!!")
            daemon
        } catch (e: Throwable) {
            report(DaemonReportCategory.INFO, "cannot find or connect to socket, exception:\n${e.javaClass.name}:${e.message}")
            daemon.close()
            null
        }
    }
}

private suspend fun tryConnectToDaemonAsync(
    port: Int,
    report: (DaemonReportCategory, String) -> Unit,
    file: File,
    useRMI: Boolean = true,
    useSockets: Boolean = true
): CompileServiceAsync? =
    useSockets.takeIf { it }?.let { tryConnectToDaemonBySockets(port, file, report) }
        ?: (useRMI.takeIf { it }?.let { tryConnectToDaemonByRMI(port, report) })

