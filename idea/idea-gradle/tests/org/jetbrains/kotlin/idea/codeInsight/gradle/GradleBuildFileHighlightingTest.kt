/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test

class GradleBuildFileHighlightingTest : GradleImportingTestCase() {

    @TargetVersions("4.8+")
    @Test
    fun testKtsInJsProject() {
        val files = configureByFiles()
        importProjectUsingSingeModulePerGradleProject()

        checkHighlighting(files)
    }

    private fun checkHighlighting(files: List<VirtualFile>) {
        for (file in files.filter { it.name.endsWith(GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION) }) {
            runReadAction {
                val psiFile = PsiManager.getInstance(myProject).findFile(file) as? KtFile
                    ?: error("Couldn't find psiFile for virtual file: ${file.canonicalPath}")

                ScriptDependenciesManager.updateScriptDependenciesSynchronously(file, myProject)

                val bindingContext = psiFile.analyze()

                val diagnostics = bindingContext.diagnostics.filter { it.severity == Severity.ERROR }
                assert(diagnostics.isEmpty()) {
                    "Diagnostic's list should be empty:\n ${diagnostics.joinToString("\n") { DefaultErrorMessages.render(it) }}"
                }
            }
        }
    }

    override fun testDataDirName() = "highlighting"
}