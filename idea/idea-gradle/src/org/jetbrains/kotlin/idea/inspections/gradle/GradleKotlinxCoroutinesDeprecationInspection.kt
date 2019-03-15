/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.gradle

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.getWholeModuleGroup
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.inspections.ReplaceStringInDocumentFix
import org.jetbrains.kotlin.idea.inspections.gradle.GradleHeuristicHelper.PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.inspections.migration.DEPRECATED_COROUTINES_LIBRARIES_INFORMATION
import org.jetbrains.kotlin.idea.inspections.migration.DeprecatedForKotlinLibInfo
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.versions.LibInfo
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

private val LibInfo.gradleMarker get() = "$groupId:$name:"

@Suppress("SpellCheckingInspection")
class GradleKotlinxCoroutinesDeprecationInspection : GradleBaseInspection(), CleanupLocalInspectionTool, MigrationFix {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(LanguageVersion.KOTLIN_1_2, LanguageVersion.KOTLIN_1_3)
    }

    override fun buildVisitor(): BaseInspectionVisitor = DependencyFinder()

    private open class DependencyFinder : KotlinGradleInspectionVisitor() {
        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            val dependencyEntries = GradleHeuristicHelper.findStatementWithPrefixes(closure, PRODUCTION_DEPENDENCY_STATEMENTS)
            for (dependencyStatement in dependencyEntries) {
                for (outdatedInfo in DEPRECATED_COROUTINES_LIBRARIES_INFORMATION) {
                    val dependencyText = dependencyStatement.text
                    val libMarker = outdatedInfo.lib.gradleMarker

                    if (!dependencyText.contains(libMarker)) continue
                    if (!checkKotlinVersion(dependencyStatement.containingFile, outdatedInfo.sinceKotlinLanguageVersion)) {
                        // Same result will be for all invocations in this file, so exit
                        return
                    }

                    val libVersion =
                        DifferentStdlibGradleVersionInspection.getResolvedLibVersion(
                            dependencyStatement.containingFile, outdatedInfo.lib.groupId, listOf(outdatedInfo.lib.name)
                        ) ?: DeprecatedGradleDependencyInspection.libraryVersionFromOrderEntry(
                            dependencyStatement.containingFile,
                            outdatedInfo.lib.name
                        ) ?: continue

                    val updatedVersion = outdatedInfo.versionUpdater.updateVersion(libVersion)
                    if (libVersion == updatedVersion) {
                        continue
                    }

                    if (dependencyText.contains(updatedVersion)) {
                        continue
                    }

                    val reportOnElement = reportOnElement(dependencyStatement, outdatedInfo)

                    val fix = if (dependencyText.contains(libVersion)) {
                        ReplaceStringInDocumentFix(reportOnElement, libVersion, updatedVersion)
                    } else {
                        null
                    }

                    registerError(
                        reportOnElement, outdatedInfo.message,
                        if (fix != null) arrayOf(fix) else emptyArray(),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )

                    break
                }
            }
        }

        private fun reportOnElement(classpathEntry: GrCallExpression, deprecatedForKotlinInfo: DeprecatedForKotlinLibInfo): PsiElement {
            val indexOf = classpathEntry.text.indexOf(deprecatedForKotlinInfo.lib.name)
            if (indexOf < 0) return classpathEntry

            return classpathEntry.findElementAt(indexOf) ?: classpathEntry
        }

        private fun checkKotlinVersion(file: PsiFile, languageVersion: LanguageVersion): Boolean {
            val module = ProjectRootManager.getInstance(file.project).fileIndex.getModuleForFile(file.virtualFile) ?: return false
            val moduleGroup = module.getWholeModuleGroup()
            return moduleGroup.sourceRootModules.any { moduleInGroup ->
                moduleInGroup.languageVersionSettings.languageVersion >= languageVersion
            }
        }
    }
}