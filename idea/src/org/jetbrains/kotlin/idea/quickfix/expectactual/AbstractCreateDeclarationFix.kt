/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*

abstract class AbstractCreateDeclarationFix<D : KtNamedDeclaration>(
    declaration: D,
    protected val module: Module,
    protected val generateIt: KtPsiFactory.(Project, D) -> D?
) : KotlinQuickFixAction<D>(declaration) {

    override fun getFamilyName(): String = "Create expect / actual declaration"

    protected val elementType: String = element.getTypeDescription()

    override fun startInWriteAction() = false

    protected abstract fun findExistingFileToCreateDeclaration(
        originalFile: KtFile,
        originalDeclaration: KtNamedDeclaration
    ): KtFile?

    protected fun getOrCreateImplementationFile(): KtFile? {
        val declaration = element as? KtNamedDeclaration ?: return null
        val parent = declaration.parent
        return (parent as? KtFile)?.let { findExistingFileToCreateDeclaration(it, declaration) }
            ?: createFileForDeclaration(module, declaration)
    }

    protected fun doGenerate(
        project: Project,
        editor: Editor?,
        originalFile: KtFile,
        targetFile: KtFile,
        targetClass: KtClassOrObject?
    ) {
        val element = element ?: return
        val factory = KtPsiFactory(project)
        DumbService.getInstance(project).runWhenSmart {
            val generated = try {
                factory.generateIt(project, element) ?: return@runWhenSmart
            } catch (e: KotlinTypeInaccessibleException) {
                if (editor != null) {
                    showErrorHint(project, editor, "Cannot generate expected $elementType: " + e.message, e.message)
                }
                return@runWhenSmart
            }

            project.executeWriteCommand("Create expect / actual declaration") {
                if (targetFile.packageDirective?.fqName != originalFile.packageDirective?.fqName &&
                    targetFile.declarations.isEmpty()
                ) {
                    val packageDirective = originalFile.packageDirective
                    if (packageDirective != null) {
                        val oldPackageDirective = targetFile.packageDirective
                        val newPackageDirective = factory.createPackageDirective(packageDirective.fqName)
                        if (oldPackageDirective != null) {
                            oldPackageDirective.replace(newPackageDirective)
                        } else {
                            targetFile.add(newPackageDirective)
                        }
                    }
                }
                val generatedDeclaration = when {
                    targetClass != null -> targetClass.addDeclaration(generated as KtNamedDeclaration)
                    else -> targetFile.add(generated) as KtElement
                }
                val reformatted = CodeStyleManager.getInstance(project).reformat(generatedDeclaration)
                val shortened = ShortenReferences.DEFAULT.process(reformatted as KtElement)
                EditorHelper.openInEditor(shortened)?.caretModel?.moveToOffset(shortened.textRange.startOffset)
            }
        }
    }
}