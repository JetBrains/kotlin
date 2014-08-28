/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.jet.plugin.JetJdkAndLibraryProjectDescriptor
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.jet.plugin.PluginTestCaseBase
import kotlin.test.assertEquals
import org.jetbrains.jet.plugin.caches.resolve.getBindingContext
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.jet.JetTestUtils
import kotlin.test.assertTrue

public class NoErrorsInStdlibTest : LightCodeInsightFixtureTestCase() {
    public fun testNoErrors() {
        val root = myFixture.copyDirectoryToProject("../libraries/stdlib/src", "")

        val psiManager = getPsiManager() // workaround for KT-3974 IllegalAccessError when accessing protected method inherited by outer class

        var totalErrors = 0
        var hasAtLeastOneFile = false

        VfsUtil.processFileRecursivelyWithoutIgnored(root) { file ->
            if (!file!!.isDirectory()) {
                val psiFile = psiManager.findFile(file)
                if (psiFile is JetFile) {
                    hasAtLeastOneFile = true
                    val bindingContext = psiFile.getBindingContext()
                    val errors = bindingContext.getDiagnostics().all().filter { it.getSeverity() == Severity.ERROR }

                    if (errors.isNotEmpty()) {
                        System.err.println("${psiFile.getName()}: ${errors.size} errors")
                        AnalyzerWithCompilerReport.reportDiagnostics(
                                bindingContext.getDiagnostics(), MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR
                        )

                        totalErrors += errors.size
                    }
                }
            }

            true
        }

        assertEquals(0, totalErrors)
        assertTrue(hasAtLeastOneFile)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        val dir = JetTestUtils.tmpDir("noErrorsInStdlibTest")
        ForTestCompileRuntime.compileBuiltIns(dir)
        return object : JetJdkAndLibraryProjectDescriptor(dir) {
            override fun getSdk(): Sdk? = PluginTestCaseBase.fullJdk()
        }
    }
}
