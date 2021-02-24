/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlFile
import org.jetbrains.idea.maven.dom.MavenDomUtil
import org.jetbrains.idea.maven.indices.MavenArtifactSearchDialog
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenArtifactScope
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.maven.KotlinMavenBundle
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class KotlinMavenUnresolvedReferenceQuickFixProvider : UnresolvedReferenceQuickFixProvider<KtSimpleNameReference>() {
    override fun getReferenceClass(): Class<KtSimpleNameReference> = KtSimpleNameReference::class.java

    override fun registerFixes(ref: KtSimpleNameReference, registrar: QuickFixActionRegistrar) {
        val module = ModuleUtilCore.findModuleForPsiElement(ref.expression) ?: return
        if (!MavenProjectsManager.getInstance(module.project).isMavenizedModule(module)) {
            return
        }

        val expression = ref.expression
        val importDirective = expression.getParentOfType<KtImportDirective>(true)

        val name = if (importDirective != null) {
            if (importDirective.isAllUnder) {
                null
            } else {
                importDirective.importedFqName?.asString()
            }
        } else {
            val typeReference = expression.getParentOfType<KtTypeReference>(true)
            val referenced = typeReference?.text ?: expression.getReferencedName()

            expression.containingKtFile
                .importDirectives
                .firstOrNull { !it.isAllUnder && it.aliasName == referenced || it.importedFqName?.shortName()?.asString() == referenced }
                ?.let { it.importedFqName?.asString() }
                ?: referenced
        }

        if (name != null) {
            registrar.register(AddMavenDependencyQuickFix(name, expression.createSmartPointer()))
        }
    }
}

class AddMavenDependencyQuickFix(
    val className: String,
    private val smartPsiElementPointer: SmartPsiElementPointer<KtSimpleNameExpression>
) :
    IntentionAction, LowPriorityAction {
    override fun getText() = KotlinMavenBundle.message("fix.add.maven.dependency.name")
    override fun getFamilyName() = text
    override fun startInWriteAction() = false
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) =
        smartPsiElementPointer.element.let { it != null && it.isValid } && file != null && MavenDomUtil.findContainingProject(file) != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) {
            return
        }

        val virtualFile = file.originalFile.virtualFile ?: return
        val mavenProject = MavenDomUtil.findContainingProject(file) ?: return
        val xmlFile = PsiManager.getInstance(project).findFile(mavenProject.file) as? XmlFile ?: return

        val ids = MavenArtifactSearchDialog.searchForClass(project, className)
        if (ids.isEmpty()) return

        runWriteAction {
            val isTestSource = ProjectRootManager.getInstance(project).fileIndex.isInTestSourceContentKotlinAware(virtualFile)
            val scope = if (isTestSource) MavenArtifactScope.TEST else null

            PomFile.forFileOrNull(xmlFile)?.let { pom ->
                ids.forEach {
                    pom.addDependency(it, scope)
                }
            }
        }
    }
}

