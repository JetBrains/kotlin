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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.io.Serializable
import java.rmi.server.UnicastRemoteObject


open class BasicCompilerServicesWithResultsFacadeServer(
        val messageCollector: MessageCollector,
        val outputsCollector: ((File, List<File>) -> Unit)? = null,
        port: Int = SOCKET_ANY_FREE_PORT
) : CompilerServicesFacadeBase,
    UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
{
    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        messageCollector.reportFromDaemon(outputsCollector, category, severity, message, attachment)
    }
}

fun MessageCollector.reportFromDaemon(outputsCollector: ((File, List<File>) -> Unit)?, category: Int, severity: Int, message: String?, attachment: Serializable?) {
    val reportCategory = ReportCategory.fromCode(category)

    when (reportCategory) {
        ReportCategory.OUTPUT_MESSAGE -> {
            if (outputsCollector != null) {
                OutputMessageUtil.parseOutputMessage(message.orEmpty())?.let { outs ->
                    outs.outputFile?.let {
                        outputsCollector.invoke(it, outs.sourceFiles.toList())
                    }
                }
            }
            else {
                report(CompilerMessageSeverity.OUTPUT, message!!)
            }
        }
        ReportCategory.EXCEPTION -> {
            report(CompilerMessageSeverity.EXCEPTION, message.orEmpty())
        }
        ReportCategory.COMPILER_MESSAGE -> {
            val compilerSeverity = when (ReportSeverity.fromCode(severity)) {
                ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
                ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
                ReportSeverity.INFO -> CompilerMessageSeverity.INFO
                ReportSeverity.DEBUG -> CompilerMessageSeverity.LOGGING
            }
            if (message != null) {
                report(compilerSeverity, message, attachment as? CompilerMessageSourceLocation)
            }
            else {
                reportUnexpected(category, severity, message, attachment)
            }
        }
        ReportCategory.DAEMON_MESSAGE,
        ReportCategory.IC_MESSAGE -> {
            if (message != null) {
                report(CompilerMessageSeverity.LOGGING, message)
            }
            else {
                reportUnexpected(category, severity, message, attachment)
            }
        }
        else -> {
            reportUnexpected(category, severity, message, attachment)
        }
    }
}

private fun MessageCollector.reportUnexpected(category: Int, severity: Int, message: String?, attachment: Serializable?) {
    val compilerMessageSeverity = when (ReportSeverity.fromCode(severity)) {
        ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
        ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
        ReportSeverity.INFO -> CompilerMessageSeverity.INFO
        ReportSeverity.DEBUG -> CompilerMessageSeverity.LOGGING
    }

    report(compilerMessageSeverity, "Unexpected message: category=$category; severity=$severity; message='$message'; attachment=$attachment")
}
