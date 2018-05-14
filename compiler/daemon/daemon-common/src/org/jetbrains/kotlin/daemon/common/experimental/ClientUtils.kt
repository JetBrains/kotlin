/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental


import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.runWithTimeout
import java.io.File
import java.rmi.registry.LocateRegistry
import java.util.logging.Logger

/*
1) walkDaemonsAsync = walkDaemons + some async calls inside (also some used classes changed *** -> ***Async)
2) tryConnectToDaemonBySockets / tryConnectToDaemonByRMI

*/

internal val MAX_PORT_NUMBER = 0xffff

private const val ORPHANED_RUN_FILE_AGE_THRESHOLD_MS = 1000000L

data class DaemonWithMetadataAsync(val daemon: CompileServiceClientSide, val runFile: File, val jvmOptions: DaemonJVMOptions)

val log = Logger.getLogger("client utils")
internal fun String.info(msg: String) = {}()//log.info("[$this] : $msg")

private suspend fun <T, R : Any> List<T>.mapNotNullAsync(transform: suspend (T) -> R?): List<R> =
    this
        .map { async { transform(it) } }
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
): List<DaemonWithMetadataAsync> {//Deferred<List<DaemonWithMetadataAsync>> {
    // : Sequence<DaemonWithMetadataAsync>
    val classPathDigest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString()
    val portExtractor = makePortFromRunFilenameExtractor(classPathDigest)
    log.info("\nssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss\n")
    return registryDir.walk().toList() // list, since walk returns Sequence and Sequence.map{...} is not inline => coroutines dont work
            .map { Pair(it, portExtractor(it.name)) }
            .filter { (file, port) -> port != null && filter(file, port) }
            .mapNotNull { (file, port) ->
                //.mapNotNull { (file, port) ->
                // all actions process concurrently
                log.info("\n<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>\n")
                log.info("(port = $port, file = $file)")
                log.info("(port = $port, file = $file)")
                log.info("fileToCompareTimestamp = $fileToCompareTimestamp")
                log.info("port != null : ${port != null}")
                log.info("port in RANGE : ${(port ?: -1) in 1..(MAX_PORT_NUMBER - 1)}")
                assert(port!! in 1..(MAX_PORT_NUMBER - 1))
                log.info("lastModified()")
                log.info("file.lastModified() : ${file.lastModified()}")
                log.info("fileToCompareTimestamp.lastModified() : ${fileToCompareTimestamp.lastModified()}")
                val relativeAge = fileToCompareTimestamp.lastModified() - file.lastModified()
                log.info("after ASSERT - relativeAge : $relativeAge")
//            report(
//                DaemonReportCategory.DEBUG,
//                "found daemon on socketPort $port ($relativeAge ms old), trying to connect"
//            )
                log.info("found daemon on socketPort $port ($relativeAge ms old), trying to connect")
                val daemon = tryConnectToDaemonAsync(port, report, file, useRMI, useSockets)
                log.info("daemon = $daemon (port= $port)")
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
                    log.info("try daemon = ...   ($daemon(port=$port)), daemon != null : ${daemon != null}")
                    daemon
                        ?.let {
                            DaemonWithMetadataAsync(it, file, it.getDaemonJVMOptions().get())
                        }
                        .also {
                            log.info("($port)DaemonWithMetadataAsync == $it)")
                        }
                } catch (e: Exception) {
                    log.info("($port)<error_in_client_utils> : " + e.message)
                    report(
                        org.jetbrains.kotlin.daemon.common.DaemonReportCategory.INFO,
                        "ERROR: unable to retrieve daemon JVM options, assuming daemon is dead: ${e.message}"
                    )
                    null
                }.also {
                    log.info("\n\n_______________________________________________________________________________________________________\n\n")
                }
            }
    }
//}

private inline fun tryConnectToDaemonByRMI(port: Int, report: (DaemonReportCategory, String) -> Unit): CompileServiceClientSide? {
    try {
        log.info("tryConnectToDaemonByRMI(port = $port)")
        val daemon = runBlocking {
            runWithTimeout(2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
                LocateRegistry.getRegistry(
                    LoopbackNetworkInterface.loopbackInetAddressName,
                    port,
                    LoopbackNetworkInterface.clientLoopbackSocketFactoryRMI
                )?.lookup(COMPILER_SERVICE_RMI_NAME)
            }
        }
        when (daemon) {
            null -> report(DaemonReportCategory.INFO, "daemon not found")
            is CompileService -> return daemon.toClient(port)
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
            report(DaemonReportCategory.INFO, "kcannot find or connect to socket")
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
): CompileServiceClientSide? =
    useSockets.takeIf { it }?.let { tryConnectToDaemonBySockets(port, file, report) }
            ?: (useRMI.takeIf { it }?.let { tryConnectToDaemonByRMI(port, report) })

