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

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.kotlin.idea.caches.resolve.ModuleOrigin
import org.jetbrains.kotlin.idea.caches.resolve.getNullableModuleInfo
import org.jetbrains.kotlin.idea.completion.CompletionBenchmarkSink
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.idea.stubindex.KotlinExactPackagesIndex
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import java.awt.Robot
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.JFileChooser
import kotlin.properties.Delegates

class BenchmarkCompletionAction : AnAction() {

    fun showPopup(project: Project, text: String) {
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, MessageType.ERROR, null)
                .setFadeoutTime(5000)
                .createBalloon().showInCenterOf(statusBar.component)
    }

    override fun actionPerformed(e: AnActionEvent?) {
        val project = e?.project!!


        val scope = object : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            override fun isSearchOutsideRootModel(): Boolean {
                return false
            }
        }
        val ktFiles = mutableListOf<SmartPsiPointer>()
        val exactPackageIndex = KotlinExactPackagesIndex.getInstance()
        exactPackageIndex.processAllKeys(project) {
            exactPackageIndex.get(it, project, scope).forEach {
                val ptr = SmartPsiPointer(it)
                ktFiles += ptr
            }
            true
        }


        ktFiles.removeAll {
            val element = it.element as? KtFile ?: return@removeAll true
            val moduleInfo = element.getNullableModuleInfo() ?: return@removeAll true
            if (element.isCompiled || !element.isWritable || element.isScript) return@removeAll true
            return@removeAll moduleInfo.moduleOrigin != ModuleOrigin.MODULE
        }

        data class Settings(val seed: Long, val attempts: Int, val lines: Int)

        fun showSettingsDialog(): Settings? {
            var cSeed: JBTextField by Delegates.notNull()
            var cAttempts: JBTextField by Delegates.notNull()
            var cLines: JBTextField by Delegates.notNull()
            val dialogBuilder = DialogBuilder()


            val jPanel = JBPanel<JBPanel<*>>(GridLayoutManager(3, 3)).apply {
                this.add(JBLabel("Random seed: "), GridConstraints().apply { row = 0; column = 0 })
                this.add(JBTextField().apply {
                    cSeed = this
                    text = "0"
                    toolTipText = "Random seed"
                }, GridConstraints().apply { row = 0; column = 1; fill = GridConstraints.FILL_HORIZONTAL })

                this.add(JBLabel("Attempts: "), GridConstraints().apply { row = 1; column = 0 })
                this.add(JBTextField().apply {
                    cAttempts = this
                    text = "5"
                    toolTipText = "Number of files to work with"
                }, GridConstraints().apply { row = 1; column = 1; fill = GridConstraints.FILL_HORIZONTAL })

                this.add(JBLabel("File lines: "), GridConstraints().apply { row = 2; column = 0 })
                this.add(JBTextField().apply {
                    cLines = this
                    text = "100"
                    toolTipText = "File lines"
                }, GridConstraints().apply { row = 2; column = 1; fill = GridConstraints.FILL_HORIZONTAL })
            }
            dialogBuilder.centerPanel(jPanel)
            if (!dialogBuilder.showAndGet()) return null

            return Settings(cSeed.text.toLong(),
                            cAttempts.text.toInt(),
                            cLines.text.toInt())
        }

        val settings = showSettingsDialog() ?: return

        ktFiles.removeAll {
            val element = it.element as KtFile
            element.getLineCount() < settings.lines
        }

        if (ktFiles.size < settings.attempts) return showPopup(project, "Number of attempts > then files in project, ${ktFiles.size}")



        val random = Random()
        random.setSeed(settings.seed)

        fun <T> List<T>.randomElement(): T? = if (this.isNotEmpty()) this[random.nextInt(this.size)] else null
        fun <T> Array<T>.randomElement(): T? = if (this.isNotEmpty()) this[random.nextInt(this.size)] else null

        val robot = Robot()


        fun sendKey(keyCode: Int) {
            robot.keyPress(keyCode)
            robot.delay(100)
            robot.keyRelease(keyCode)
            robot.delay(100)
        }

        fun type(s: String) {
            for (c in s.toCharArray()) {
                val keyCode = KeyEvent.getExtendedKeyCodeForChar(c.toInt())
                if (KeyEvent.CHAR_UNDEFINED == keyCode.toChar()) {
                    throw RuntimeException("Key code not found for character '$c'")
                }
                if (c.isUpperCase()) {
                    robot.keyPress(KeyEvent.VK_SHIFT)
                    robot.delay(10)
                }

                sendKey(keyCode)
                if (c.isUpperCase()) {
                    robot.keyRelease(KeyEvent.VK_SHIFT)
                    robot.delay(10)
                }

            }
        }

        data class Result(val lines: Int, val filePath: String, val first: Long, val full: Long)

        val allResults = mutableListOf<Result>()

        val benchmark = CompletionBenchmarkSink.enableAndGet()

        val typeAtOffsetAndBenchmark: suspend (String, Int, KtFile) -> Unit = {
            text: String, offset: Int, file: KtFile ->
            NavigationUtil.openFileWithPsiElement(file.navigationElement, false, true)

            val document = PsiDocumentManager.getInstance(project).getDocument(file)
            val ourEditor = EditorFactory.getInstance().allEditors.find { it.document == document }

            if (document != null && ourEditor != null) {

                delay(500)

                ourEditor.moveCaret(offset, scrollType = ScrollType.CENTER)

                repeat(2) { sendKey(KeyEvent.VK_ENTER) }
                delay(500)
                val rangeMarker = document.createRangeMarker(offset, ourEditor.caretModel.offset)
                sendKey(KeyEvent.VK_UP)

                val t = text
                type(t)

                val results = benchmark.channel.receive()
                println("fsize: ${file.getLineCount()}, ${file.virtualFile.path}")
                println("first: ${results.firstFlush}, full: ${results.full}")

                allResults += Result(file.getLineCount(), "${file.virtualFile.path}:${document.getLineNumber(offset)}", results.firstFlush, results.full)

                CommandProcessor.getInstance().executeCommand(project, {
                    runWriteAction {
                        document.deleteString(rangeMarker.startOffset, rangeMarker.endOffset)
                        PsiDocumentManager.getInstance(project).commitDocument(document)
                    }
                }, "ss", "ss")
                delay(100)
            }
        }

        launch(EDT) {
            for (i in 0 until settings.attempts) {
                val file = ktFiles.randomElement()!!.apply { ktFiles.remove(this) }.element as? KtFile ?: continue

                run {
                    val offset = (file.importList?.nextLeafs?.firstOrNull() as? PsiWhiteSpace)?.endOffset ?: 0
                    typeAtOffsetAndBenchmark("fun Str", offset, file)

                }
                val ktClassOrObject = file.getChildrenOfType<KtClassOrObject>()
                                              .filter { it.getBody() != null }
                                              .randomElement() ?: continue

                run {
                    val body = ktClassOrObject.getBody()

                    val offset = body!!.lBrace!!.endOffset
                    typeAtOffsetAndBenchmark("fun Str", offset, file)
                }


            }
            CompletionBenchmarkSink.disable()
            val jfc = JFileChooser()
            val result = jfc.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = jfc.selectedFile
                file.writeText(buildString {
                    allResults.joinTo(this, separator = "\n") { (l, f, ff, lf) -> "$f, $l, $ff, $lf" }
                })
            }
            showPopup(project, "Done")
        }

    }

}
