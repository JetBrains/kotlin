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

package org.jetbrains.kotlin.idea.actions

import com.google.common.collect.Lists
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.quickfix.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile

import javax.swing.*

/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 */
public class JetAddImportAction
/**
 * @param project Project where action takes place.
 * @param editor Editor where modification should be done.
 * @param element Element with unresolved reference.
 * @param imports Variants for resolution.
 */
(private val myProject: Project, private val myEditor: Editor, private val myElement: PsiElement, imports: Iterable<FqName>) : QuestionAction {
    private val possibleImports: List<FqName>

    {
        possibleImports = Lists.newArrayList<FqName>(imports)
    }

    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()

        if (!myElement.isValid()) {
            return false
        }

        // TODO: Validate resolution variants. See AddImportAction.execute()

        if (possibleImports.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
            addImport(myElement, myProject, possibleImports.get(0))
        }
        else {
            chooseClassAndImport()
        }

        return true
    }

    protected fun getImportSelectionPopup(): BaseListPopupStep<Any> {
        return object : BaseListPopupStep<FqName>(JetBundle.message("imports.chooser.title"), possibleImports) {
            override fun isAutoSelectionEnabled(): Boolean {
                return false
            }

            override fun onChosen(selectedValue: FqName?, finalChoice: Boolean): PopupStep<Any>? {
                if (selectedValue == null) {
                    return PopupStep.FINAL_CHOICE
                }

                if (finalChoice) {
                    addImport(myElement, myProject, selectedValue)
                    return PopupStep.FINAL_CHOICE
                }

                val toExclude = AddImportAction.getAllExcludableStrings(selectedValue.asString())

                return object : BaseListPopupStep<String>(null, toExclude) {
                    override fun getTextFor(value: String): String {
                        return "Exclude '" + value + "' from auto-import"
                    }

                    override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<Any>? {
                        if (finalChoice) {
                            AddImportAction.excludeFromImport(myProject, selectedValue)
                        }

                        return super.onChosen(selectedValue, finalChoice)
                    }
                }
            }

            override fun hasSubstep(selectedValue: FqName?): Boolean {
                return true
            }

            override fun getTextFor(value: FqName): String {
                return value.asString()
            }

            override fun getIconFor(aValue: FqName): Icon? {
                // TODO: change icon
                return PlatformIcons.CLASS_ICON
            }
        }
    }

    private fun chooseClassAndImport() {
        JBPopupFactory.getInstance().createListPopup(getImportSelectionPopup()).showInBestPositionFor(myEditor)
    }

    class object {

        protected fun addImport(element: PsiElement, project: Project, selectedImport: FqName) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            CommandProcessor.getInstance().executeCommand(project, object : Runnable {
                override fun run() {
                    ApplicationManager.getApplication().runWriteAction(object : Runnable {
                        override fun run() {
                            val file = element.getContainingFile()
                            if (!(file is JetFile)) return
                            ImportInsertHelper.getInstance().addImportDirectiveIfNeeded(selectedImport, file as JetFile)
                        }
                    })
                }
            }, QuickFixBundle.message("add.import"), null)
        }
    }
}
