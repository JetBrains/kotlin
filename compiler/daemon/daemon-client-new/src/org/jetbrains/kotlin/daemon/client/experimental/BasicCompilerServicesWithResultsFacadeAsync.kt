/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ServerSocketWrapper
import java.io.File
import java.io.Serializable

//interface MessageCollectorAsync {
//    fun clear()
//
//    suspend fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation? = null)
//
//    suspend fun hasErrors(): Boolean
//}

//fun MessageCollector.toAsync() = object : MessageCollectorAsync {
//
//    override fun clear() = this@toAsync.clear()
//
//    override suspend fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) =
//        this@toAsync.report(severity, message, location)
//
//    override suspend fun hasErrors(): Boolean = this@toAsync.hasErrors()
//
//}

open class BasicCompilerServicesWithResultsFacadeServerServerSide(
    val messageCollector: MessageCollector,
    val outputsCollector: ((File, List<File>) -> Unit)? = null,
    override val serverSocketWithPort: ServerSocketWrapper = findCallbackServerSocket()
) : CompilerServicesFacadeBaseServerSide {

    override val clients = hashMapOf<Socket, Server.ClientInfo>()

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        messageCollector.reportFromDaemon(outputsCollector, category, severity, message, attachment)
    }

    val clientSide: CompilerServicesFacadeBaseClientSide
        get() = CompilerServicesFacadeBaseClientSideImpl(serverSocketWithPort.port)
}

//suspend fun MessageCollectorAsync.reportFromDaemon(
//    outputsCollector: ((File, List<File>) -> Unit)?,
//    category: Int,
//    severity: Int,
//    message: String?,
//    attachment: Serializable?
//) {
//    val reportCategory = ReportCategory.fromCode(category)
//
//    when (reportCategory) {
//        ReportCategory.OUTPUT_MESSAGE -> {
//            if (outputsCollector != null) {
//                OutputMessageUtil.parseOutputMessage(message.orEmpty())?.let { outs ->
//                    outs.outputFile?.let {
//                        outputsCollector.invoke(it, outs.sourceFiles.toList())
//                    }
//                }
//            } else {
//                report(CompilerMessageSeverity.OUTPUT, message.orEmpty())
//            }
//        }
//        ReportCategory.EXCEPTION -> {
//            report(CompilerMessageSeverity.EXCEPTION, message.orEmpty())
//        }
//        ReportCategory.COMPILER_MESSAGE -> {
//            val compilerSeverity = when (ReportSeverity.fromCode(severity)) {
//                ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
//                ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
//                ReportSeverity.INFO -> CompilerMessageSeverity.INFO
//                ReportSeverity.DEBUG -> CompilerMessageSeverity.LOGGING
//                else -> throw IllegalStateException("Unexpected compiler message report severity $severity")
//            }
//            if (message != null) {
//                report(compilerSeverity, message, attachment as? CompilerMessageLocation)
//            } else {
//                reportUnexpected(category, severity, message, attachment)
//            }
//        }
//        ReportCategory.DAEMON_MESSAGE,
//        ReportCategory.IC_MESSAGE -> {
//            if (message != null) {
//                report(CompilerMessageSeverity.LOGGING, message)
//            } else {
//                reportUnexpected(category, severity, message, attachment)
//            }
//        }
//        else -> {
//            reportUnexpected(category, severity, message, attachment)
//        }
//    }
//}
//
//private suspend fun MessageCollectorAsync.reportUnexpected(category: Int, severity: Int, message: String?, attachment: Serializable?) {
//    val compilerMessageSeverity = when (ReportSeverity.fromCode(severity)) {
//        ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
//        ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
//        ReportSeverity.INFO -> CompilerMessageSeverity.INFO
//        else -> CompilerMessageSeverity.LOGGING
//    }
//
//    report(
//        compilerMessageSeverity,
//        "Unexpected message: category=$category; severity=$severity; message='$message'; attachment=$attachment"
//    )
//}