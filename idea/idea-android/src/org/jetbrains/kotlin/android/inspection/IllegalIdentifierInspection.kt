/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.inspection

import com.android.builder.model.AndroidProject.ARTIFACT_UNIT_TEST
import com.android.tools.idea.AndroidPsiUtils
import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.android.getAndroidFacetForFile
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.RenameIdentifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.jvm.checkers.isValidDalvikIdentifier
import java.io.File

class IllegalIdentifierInspection : AbstractKotlinInspection() {
    private class JunitPaths(val paths: List<File>, val generationId: Long) {
        companion object : Key<JunitPaths>("AndroidModuleJunitPaths")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != KtTokens.IDENTIFIER) return

                val text = element.text
                // '`' can't be escaped now
                if (!text.startsWith('`') || !text.endsWith('`')) return

                val unquotedName = KtPsiUtil.unquoteIdentifier(text)
                // This is already an error
                if (unquotedName.isEmpty()) return

                if (!isValidDalvikIdentifier(unquotedName) && checkAndroidFacet(element)) {
                    if (element.isInUnitTests()) {
                        return
                    }

                    holder.registerProblem(
                        element,
                        "Identifier not allowed in Android projects",
                        ProblemHighlightType.GENERIC_ERROR,
                        RenameIdentifierFix()
                    )
                }
            }

            private fun PsiElement.isInUnitTests(): Boolean {
                val containingFile = containingFile?.virtualFile?.let { getIoFile(it) }
                val module = AndroidPsiUtils.getModuleSafely(this)

                if (module != null && containingFile != null) {
                    val currentGenerationId = ProjectRootModificationTracker.getInstance(module.project).modificationCount
                    val junitTestPaths = module.getUserData(JunitPaths)
                        ?.takeIf { it.generationId == currentGenerationId }
                        ?: JunitPaths(getJunitTestPaths(module), currentGenerationId).also { module.putUserData(JunitPaths, it) }

                    if (junitTestPaths.paths.any { containingFile.startsWith(it) }) {
                        return true
                    }
                }

                return false
            }

            private fun checkAndroidFacet(element: PsiElement): Boolean {
                return element.getAndroidFacetForFile() != null || ApplicationManager.getApplication().isUnitTestMode
            }
        }
    }

    private fun getJunitTestPaths(module: Module): List<File> {
        val androidFacet = AndroidFacet.getInstance(module) ?: return emptyList()
        val androidModuleModel = AndroidModuleModel.get(androidFacet) ?: return emptyList()

        return androidModuleModel.getTestSourceProviders(ARTIFACT_UNIT_TEST).flatMap { it.javaDirectories }
    }

    private fun getIoFile(virtualFile: VirtualFile): File? {
        var path = virtualFile.path

        // Taken from LocalFileSystemBase.convertToIOFile
        if (StringUtil.endsWithChar(path, ':') && path.length == 2 && SystemInfo.isWindows) {
            path += "/"
        }

        return File(path).takeIf { it.exists() }
    }
}