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

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

public class AddHatsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
                          ?: return

        val virtualFiles = VfsUtil.collectChildrenRecursively(virtualFile)

        val project = e.project!!

        val jetFiles = virtualFiles.map {
            PsiManager.getInstance(project).findFile(it) as? JetFile
        }.filterNotNull().toTypedArray()


        object : WriteCommandAction<Any?>(project, *jetFiles) {
            override fun run(result: Result<Any?>) {
                for (jetFile in jetFiles) {
                    val bindingContext = jetFile.analyzeFully()

                    jetFile.accept(
                            object : JetTreeVisitorVoid() {
                                override fun visitBlockExpression(expression: JetBlockExpression) {
                                    val parent = expression.parent
                                    if (parent !is JetDeclaration) {
                                        addHats(expression.statements)
                                    }
                                    super.visitBlockExpression(expression)
                                }

                                override fun visitFunctionLiteralExpression(expression: JetFunctionLiteralExpression) {
                                    addHats(expression.bodyExpression?.statements ?: emptyList())
                                    super.visitFunctionLiteralExpression(expression)
                                }

                                private fun addHats(statements: List<JetExpression>) {
                                    if (statements.size() <= 1) return

                                    val lastExpression = statements.last()
                                    if (!lastExpression.isUsedAsExpression(bindingContext)) {
                                        println("Not used as expression: $lastExpression")
                                        return
                                    }

                                    val type = bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, lastExpression)?.type
                                    if (type == null) {
                                        println("Type is null: $lastExpression")
                                    }
                                    if (type != null && (KotlinBuiltIns.isUnit(type) || KotlinBuiltIns.isNothing(type))) {
                                        println("Type is Unit/Nothing: $lastExpression")
                                        return
                                    }
                                    val expectedType = bindingContext.get(BindingContext.EXPECTED_EXPRESSION_TYPE, lastExpression)
                                    if (expectedType != null && (KotlinBuiltIns.isUnit(expectedType) || KotlinBuiltIns.isNothing(expectedType))) {
                                        println("Expected Type is Unit/Nothing: $lastExpression")
                                        return
                                    }

                                    val node = lastExpression.node

                                    val space = node.treePrev
                                    val oldText = space.text
                                    val newText = if (oldText.endsWith("^")) oldText else oldText.removeSuffix(" ") + "^"
                                    node.treeParent.replaceChild(
                                            space,
                                            LeafPsiElement(JetTokens.BLOCK_COMMENT, newText)
                                    )
                                }
                            }
                    )
                }
            }
        }.execute()

    }


    override fun update(e: AnActionEvent?) {
        e!!.presentation.isVisible = ApplicationManager.getApplication().isInternal

        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = virtualFile != null
    }

}
