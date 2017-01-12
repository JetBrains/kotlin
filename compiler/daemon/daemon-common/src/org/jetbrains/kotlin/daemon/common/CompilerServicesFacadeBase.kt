/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

interface CompilerServicesFacadeBase : Remote {
    /**
     * Reports different kind of diagnostic messages from compile daemon to compile daemon clients (jps, gradle, ...)
     */
    @Throws(RemoteException::class)
    fun report(category: Int, severity: Int, message: String?, attachment: Serializable?)
}

enum class ReportCategory(val code: Int) {
    COMPILER_MESSAGE(0),
    DAEMON_MESSAGE(1),
    IC_MESSAGE(2),
    OUTPUT_MESSAGE(3);

    companion object {
        fun fromCode(code: Int): ReportCategory? =
                ReportCategory.values().firstOrNull { it.code == code }

    }
}

enum class ReportSeverity(val code: Int) {
    ERROR(0),
    WARNING(1),
    INFO(2),
    DEBUG(3);

    companion object {
        fun fromCode(code: Int): ReportSeverity? =
                ReportSeverity.values().firstOrNull { it.code == code }
    }
}

fun CompilerServicesFacadeBase.report(category: ReportCategory, severity: ReportSeverity, message: String? = null, attachment: Serializable? = null) {
    report(category.code, severity.code, message, attachment)
}

data class CompilerMessageAttachment(
        val severity: CompilerMessageSeverity,
        val location: CompilerMessageLocation
) : Serializable {
    companion object {
        const val serialVersionUID = 0L
    }
}
