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

package org.jetbrains.kotlin.idea.refactoring.move.changePackage;

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetPackageDirective

public class ChangePackageIntention: JetSelfTargetingOffsetIndependentIntention<JetPackageDirective>(javaClass(), "Change package") {
    override fun isApplicableTo(element: JetPackageDirective) = element.getPackageNameExpression() != null

    override fun applyTo(element: JetPackageDirective, editor: Editor) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            throw UnsupportedOperationException("Do not call applyTo() in the test mode")
        }

        val file = element.getContainingJetFile()
        val project = file.getProject()

        val nameExpression = element.getPackageNameExpression()!!
        val currentName = element.getQualifiedName()

        val builder = TemplateBuilderImpl(file)
        builder.replaceElement(
                nameExpression,
                object: Expression() {
                    override fun calculateQuickResult(context: ExpressionContext?) = TextResult(currentName)
                    override fun calculateResult(context: ExpressionContext?) = TextResult(currentName)
                    override fun calculateLookupItems(context: ExpressionContext?) = arrayOf(LookupElementBuilder.create(currentName))
                }
        )
        editor.getCaretModel().moveToOffset(0)
        TemplateManager.getInstance(project).startTemplate(
                editor,
                builder.buildInlineTemplate(),
                object: TemplateEditingAdapter() {
                    override fun templateFinished(template: Template?, brokenOff: Boolean) {
                        if (!brokenOff) {
                            // Restore original name and run refactoring
                            val packageDirective = file.getPackageDirective()!!
                            val newFqName = packageDirective.getFqName()
                            packageDirective.setFqName(FqName(currentName))
                            KotlinChangePackageRefactoring(file).run(newFqName)
                        }
                    }
                }
        )
    }
}
