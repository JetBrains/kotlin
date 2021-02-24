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

package org.jetbrains.kotlin.cli.common.messages

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils

/**
 * This class behaviour is the same as [MessageCollector.report] in [AnalyzerWithCompilerReport.reportDiagnostic].
 */
class DefaultDiagnosticReporter(override val messageCollector: MessageCollector) : MessageCollectorBasedReporter

interface MessageCollectorBasedReporter : DiagnosticMessageReporter {
    val messageCollector: MessageCollector

    override fun report(diagnostic: Diagnostic, file: PsiFile, render: String) = messageCollector.report(
        AnalyzerWithCompilerReport.convertSeverity(diagnostic.severity),
        render,
        MessageUtil.psiFileToMessageLocation(file, file.name, DiagnosticUtils.getLineAndColumnRange(diagnostic))
    )
}
