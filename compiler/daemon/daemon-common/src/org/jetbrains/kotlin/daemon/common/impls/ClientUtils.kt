/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.impls

import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.rmi.registry.LocateRegistry
import java.util.*
import java.util.logging.Logger


internal val MAX_PORT_NUMBER = 0xffff


enum class DaemonReportCategory {
    DEBUG, INFO, EXCEPTION;
}


fun makeRunFilenameString(timestamp: String, digest: String, port: String, escapeSequence: String = ""): String =
    "$COMPILE_DAEMON_DEFAULT_FILES_PREFIX$escapeSequence.$timestamp$escapeSequence.$digest$escapeSequence.$port$escapeSequence.runServer"


fun makePortFromRunFilenameExtractor(digest: String): (String) -> Int? {
    val regex = makeRunFilenameString(
        timestamp = "[0-9TZ:\\.\\+-]+",
        digest = digest,
        port = "(\\d+)",
        escapeSequence = "\\"
    ).toRegex()
    return {
        regex.find(it)
            ?.groups?.get(1)
            ?.value?.toInt()
    }
}

private const val ORPHANED_RUN_FILE_AGE_THRESHOLD_MS = 1000000L

private val log = Logger.getLogger("ClientUtils(old)")

data class DaemonWithMetadata(val daemon: CompileService, val runFile: File, val jvmOptions: DaemonJVMOptions)

// TODO: write metadata into discovery file to speed up selection
// TODO: consider using compiler jar signature (checksum) as a CompilerID (plus java version, plus ???) instead of classpath checksum
//    would allow to use same compiler from taken different locations
//    reqs: check that plugins (or anything els) should not be part of the CP
fun walkDaemons(
    registryDir: File,
    compilerId: CompilerId,
    fileToCompareTimestamp: File,
    filter: (File, Int) -> Boolean = { _, _ -> true },
    report: (DaemonReportCategory, String) -> Unit = { _, _ -> }
): Sequence<DaemonWithMetadata> {
    val classPathDigest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString()
    val portExtractor = makePortFromRunFilenameExtractor(classPathDigest)
    return registryDir.walk()
        .map { Pair(it, portExtractor(it.name)) }
        .filter { (file, port) -> port != null && filter(file, port) }
        .mapNotNull { (file, port) ->
            assert(port!! in 1..(MAX_PORT_NUMBER - 1))
            val relativeAge = fileToCompareTimestamp.lastModified() - file.lastModified()
            report(DaemonReportCategory.DEBUG, "found daemon on socketPort $port ($relativeAge ms old), trying to connect")
            val daemon = tryConnectToDaemon(port, report)
            log.info("discovered daemon = $daemon")
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
                log.info("it.getDaemonJVMOptions()")
                daemon?.let { DaemonWithMetadata(it, file, it.getDaemonJVMOptions().get()) }
            } catch (e: Exception) {
                log.info(e.message)
                report(DaemonReportCategory.INFO, "ERROR: unable to retrieve daemon JVM options, assuming daemon is dead: ${e.message}")
                null
            }
        }
}

private inline fun tryConnectToDaemon(port: Int, report: (DaemonReportCategory, String) -> Unit): CompileService? {
    log.info("trying to connect to daemon (using port $port)")
    try {
        log.info("acquiring registry")
        val registry = LocateRegistry.getRegistry(
            LoopbackNetworkInterface.loopbackInetAddressName,
            port,
            LoopbackNetworkInterface.clientLoopbackSocketFactory
        )
        log.info("registry = $registry")
        log.info("looking up for daemon...")
        val daemon = registry?.lookup(COMPILER_SERVICE_RMI_NAME)
        log.info("connection result daemon = $daemon")
        when (daemon) {
            null -> report(DaemonReportCategory.INFO, "daemon not found")
            is CompileService -> return daemon
            else -> report(DaemonReportCategory.INFO, "Unable to cast compiler service, actual class received: ${daemon::class.java.name}")
        }
    } catch (e: Throwable) {
        report(DaemonReportCategory.INFO, "cannot connect to registry: " + (e.cause?.message ?: e.message ?: "unknown error"))
    }
    log.info("connection result daemon = NULL")
    return null
}

private const val validFlagFileKeywordChars = "abcdefghijklmnopqrstuvwxyz0123456789-_"

fun makeAutodeletingFlagFile(keyword: String = "compiler-client", baseDir: File? = null): File {
    val flagFile = File.createTempFile("kotlin-${keyword.filter { validFlagFileKeywordChars.contains(it.toLowerCase()) }}-",
                                       "-is-running",
                                       baseDir?.takeIf { it.isDirectory && it.exists() })
    flagFile.deleteOnExit()
    return flagFile
}

// Comparator for reliable choice between daemons
class FileAgeComparator : Comparator<File> {
    override fun compare(left: File, right: File): Int {
        val leftTS = left.lastModified()
        val rightTS = right.lastModified()
        return when {
            leftTS == 0L || rightTS == 0L -> 0 // cannot read any file timestamp, => undecidable
            leftTS > rightTS -> -1
            leftTS < rightTS -> 1
            else -> compareValues(left.canonicalPath, right.canonicalPath)
        }
    }
}

const val LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE = "Assuming other daemons have"
