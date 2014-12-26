/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger

import org.jetbrains.jet.lang.resolve.diagnostics.DiagnosticsWithSuppression
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.psi.codeFragmentUtil.skipVisibilityCheck

public class DiagnosticSuppressorForDebugger : DiagnosticsWithSuppression.DiagnosticSuppressor {
    override fun isSuppressed(diagnostic: Diagnostic): Boolean {
        val element = diagnostic.getPsiElement()
        val containingFile = element.getContainingFile()

        if (containingFile is JetFile && containingFile.skipVisibilityCheck) {
            val diagnosticFactory = diagnostic.getFactory()
            return diagnosticFactory == Errors.INVISIBLE_MEMBER ||
                   diagnosticFactory == Errors.INVISIBLE_REFERENCE ||
                   diagnosticFactory == Errors.INVISIBLE_SETTER
        }

        return false
    }
}