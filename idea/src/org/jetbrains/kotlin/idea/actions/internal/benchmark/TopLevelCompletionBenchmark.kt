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

package org.jetbrains.kotlin.idea.actions.internal.benchmark

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.psi.PsiWhiteSpace
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import org.jetbrains.kotlin.idea.actions.internal.benchmark.AbstractCompletionBenchmarkAction.Companion.randomElement
import org.jetbrains.kotlin.idea.completion.CompletionBenchmarkSink
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import java.util.*
import kotlin.properties.Delegates


class TopLevelCompletionBenchmarkAction : AbstractCompletionBenchmarkAction() {

    override fun createBenchmarkScenario(project: Project, benchmarkSink: CompletionBenchmarkSink.Impl): AbstractCompletionBenchmarkScenario? {

        val settings = showSettingsDialog() ?: return null

        val random = Random(settings.seed)

        fun collectFiles(): List<KtFile>? {
            val ktFiles = collectSuitableKotlinFiles(project) {
                it.getLineCount() >= settings.lines
            }

            if (ktFiles.size < settings.files) {
                showPopup(project, "Number of attempts > then files in project, ${ktFiles.size}")
                return null
            }

            val result = mutableListOf<KtFile>()
            repeat(settings.files) {
                result += ktFiles.randomElement(random)!!.also { ktFiles.remove(it) }
            }
            return result
        }

        val ktFiles = collectFiles() ?: return null

        return TopLevelCompletionBenchmarkScenario(ktFiles, settings, project, benchmarkSink, random)
    }


    data class Settings(val seed: Long, val lines: Int, val files: Int)

    private fun showSettingsDialog(): Settings? {
        var cSeed: JBTextField by Delegates.notNull()
        var cLines: JBTextField by Delegates.notNull()
        var cFiles: JBTextField by Delegates.notNull()
        val dialogBuilder = DialogBuilder()


        val jPanel = JBPanel<JBPanel<*>>(GridLayoutManager(3, 2)).apply {
            var i = 0
            cSeed = addBoxWithLabel("Random seed", default = "0", i = i++)
            cFiles = addBoxWithLabel("Files to visit", default = "20", i = i++)
            cLines = addBoxWithLabel("File lines", default = "100", i = i)
        }
        dialogBuilder.centerPanel(jPanel)
        if (!dialogBuilder.showAndGet()) return null

        return Settings(cSeed.text.toLong(),
                        cLines.text.toInt(),
                        cFiles.text.toInt())
    }

}

internal class TopLevelCompletionBenchmarkScenario(
        val files: List<KtFile>,
        val settings: TopLevelCompletionBenchmarkAction.Settings,
        project: Project, benchmarkSink: CompletionBenchmarkSink.Impl,
        random: Random) : AbstractCompletionBenchmarkScenario(project, benchmarkSink, random) {
    suspend override fun doBenchmark() {

        val allResults = mutableListOf<Result>()
        files.forEach { file ->

            run {
                val offset = (file.importList?.nextLeafs?.firstOrNull() as? PsiWhiteSpace)?.endOffset ?: 0
                allResults += typeAtOffsetAndGetResult("fun Str", offset, file)
            }

            run {
                val classes = file.collectDescendantsOfType<KtClassOrObject> { it.getBody() != null }
                val body = classes.randomElement(random)?.getBody() ?: return@run
                val offset = body.endOffset - 1
                allResults += typeAtOffsetAndGetResult("fun Str", offset, file)
            }
        }
        saveResults(allResults)
    }
}