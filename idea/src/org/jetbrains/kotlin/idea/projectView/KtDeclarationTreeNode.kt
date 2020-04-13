/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.projectView

import com.intellij.application.options.CodeStyle
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.psi.*

internal class KtDeclarationTreeNode(
    project: Project?,
    ktDeclaration: KtDeclaration?,
    viewSettings: ViewSettings?
) : AbstractPsiBasedNode<KtDeclaration?>(project, ktDeclaration!!, viewSettings) {

    override fun extractPsiFromValue(): PsiElement? = value

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> = emptyList()

    override fun updateImpl(data: PresentationData) {
        val declaration = value ?: return
        val project = project ?: return
        data.presentableText = tryGetRepresentableText(declaration, project)
    }

    override fun isDeprecated(): Boolean = value?.let { KtPsiUtil.isDeprecated(it) } ?: false

    companion object {
        private val CLASS_INITIALIZER = "<" + KotlinBundle.message("project.view.class.initializer") + ">"

        fun tryGetRepresentableText(declaration: KtDeclaration, project: Project): String? {

            val settings = CodeStyle.getSettings(project).getCustomSettings(KotlinCodeStyleSettings::class.java)
            fun StringBuilder.appendColon() {
                if (settings.SPACE_BEFORE_TYPE_COLON) append(" ")
                append(":")
                if (settings.SPACE_AFTER_TYPE_COLON) append(" ")
            }

            fun KtProperty.presentableText() = buildString {
                append(name)
                typeReference?.text?.let { reference ->
                    appendColon()
                    append(reference)
                }
            }

            fun KtFunction.presentableText() = buildString {
                receiverTypeReference?.text?.let { receiverReference ->
                    append(receiverReference)
                    append('.')
                }
                append(name)
                append("(")
                val valueParameters = valueParameters
                valueParameters.forEachIndexed { index, parameter ->
                    parameter.name?.let { parameterName ->
                        append(parameterName)
                        appendColon()
                    }
                    parameter.typeReference?.text?.let { typeReference ->
                        append(typeReference)
                    }
                    if (index != valueParameters.size - 1) {
                        append(", ")
                    }
                }
                append(")")

                typeReference?.text?.let { returnTypeReference ->
                    appendColon()
                    append(returnTypeReference)
                }
            }

            fun KtObjectDeclaration.presentableText(): String? = buildString {

                if (isCompanion()) {
                    append("companion object")
                } else {
                    append("object")
                    if (!name.isNullOrEmpty()) {
                        append(" $name")
                    }
                }

                if (superTypeListEntries.any()) {
                    appendColon()
                    val lastIndex = superTypeListEntries.size - 1
                    superTypeListEntries.forEachIndexed { index, entry ->
                        entry.typeReference?.text?.let {
                            if (index > 0 && index != lastIndex) append(", ")
                            append(it)
                        }
                    }
                }
            }

            return when (declaration) {
                is KtProperty -> declaration.presentableText()
                is KtFunction -> declaration.presentableText()
                is KtObjectDeclaration -> declaration.presentableText()
                is KtAnonymousInitializer -> CLASS_INITIALIZER
                else -> declaration.name
            }
        }
    }
}