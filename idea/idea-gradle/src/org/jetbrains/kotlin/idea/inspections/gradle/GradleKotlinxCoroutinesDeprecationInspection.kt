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
import org.jetbrains.kotlin.idea.configuration.getWholeModuleGroup
import org.jetbrains.kotlin.idea.inspections.gradle.GradleHeuristicHelper.PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.versions.LibInfo
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

private val LibInfo.gradleMarker get() = "$groupId:$name:"

private data class DeprecatedForKotlinLibInfo(
    val lib: LibInfo,
    val sinceKotlinLanguageVersion: LanguageVersion,
    val message: String
)

@Suppress("SpellCheckingInspection")
private fun kotlinxCoroutinesDeprecation(name: String): org.jetbrains.kotlin.idea.inspections.gradle.DeprecatedForKotlinLibInfo {
    return DeprecatedForKotlinLibInfo(
        lib = LibInfo("org.jetbrains.kotlinx", name),
        sinceKotlinLanguageVersion = LanguageVersion.KOTLIN_1_3,
        message = "Library should be updated to be compatible with Kotlin 1.3"
    )
}

@Suppress("SpellCheckingInspection")
private val DEPRECATED_COROUTINES_LIBRARIES_INFORMATION = listOf(
    kotlinxCoroutinesDeprecation("kotlinx-coroutines"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-android"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-core"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-core-common"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-core-js"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-guava"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-io"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-javafx"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-jdk8"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-nio"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-quasar"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-reactive"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-reactor"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-rx1"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-rx2"),
    kotlinxCoroutinesDeprecation("kotlinx-coroutines-swing")
)

@Suppress("SpellCheckingInspection")
class GradleKotlinxCoroutinesDeprecationInspection : GradleBaseInspection(), CleanupLocalInspectionTool {
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

                    val reportOnElement = reportOnElement(dependencyStatement, outdatedInfo)

                    registerError(
                        reportOnElement, outdatedInfo.message,
                        emptyArray(),
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