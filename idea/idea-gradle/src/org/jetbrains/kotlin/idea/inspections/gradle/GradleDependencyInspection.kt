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

package org.jetbrains.kotlin.idea.inspections.gradle

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.configuration.allModules
import org.jetbrains.kotlin.idea.configuration.getWholeModuleGroup
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

data class LibInfo(
        val groupId: String,
        val name: String
)

data class OutdatedLibInfo(
        val old: LibInfo,
        val new: LibInfo,
        val outdatedAfterVersion: String,
        val message: String
)

private val outdatedLibrariesInformation = listOf(
        outdatedLib(
                oldGroupId = KotlinWithGradleConfigurator.GROUP_ID,
                oldName = PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_NAME, newName = PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_NAME,
                outdatedAfterVersion = "1.2.0-rc-39",
                message = "${PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_NAME} is deprecated since 1.2.0 and should be replaced with ${PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_NAME}"),

        outdatedLib(
                oldGroupId = KotlinWithGradleConfigurator.GROUP_ID,
                oldName = PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_NAME, newName = PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_NAME,
                outdatedAfterVersion = "1.2.0-rc-39",
                message = "${PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_NAME} is deprecated since 1.2.0 and should be replaced with ${PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_NAME}")
)

private val LibInfo.gradleMarker get() = "$groupId:$name:"

private fun outdatedLib(
        oldGroupId: String,
        oldName: String,
        newGroupId: String = oldGroupId,
        newName: String = oldName,
        outdatedAfterVersion: String,
        message: String): OutdatedLibInfo {
    return OutdatedLibInfo(
            old = LibInfo(groupId = oldGroupId, name = oldName),
            new = LibInfo(groupId = newGroupId, name = newName),
            outdatedAfterVersion = outdatedAfterVersion,
            message = message
    )
}

class GradleDependencyInspection : GradleBaseInspection() {
    override fun buildVisitor(): BaseInspectionVisitor = DependencyFinder()

    private open class DependencyFinder : KotlinGradleInspectionVisitor() {
        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            val dependencyEntries = GradleHeuristicHelper.findStatementWithPrefix(closure, "compile")
            for (dependencyStatement in dependencyEntries) {
                visitDependencyEntry(dependencyStatement)
            }
        }

        open fun visitDependencyEntry(dependencyStatement: GrCallExpression) {
            for (outdatedInfo in outdatedLibrariesInformation) {
                val dependencyText = dependencyStatement.text
                val libMarker = outdatedInfo.old.gradleMarker

                if (dependencyText.contains(libMarker)) {
                    // Should be generified for any library, not exactly Kotlin stdlib
                   val libVersion =
                            DifferentStdlibGradleVersionInspection.getResolvedKotlinStdlibVersion(
                                    dependencyStatement.containingFile, listOf(outdatedInfo.old.name)) ?:
                            libraryVersionFromOrderEntry(dependencyStatement.containingFile, outdatedInfo.old.name)


                    if (libVersion != null && VersionComparatorUtil.COMPARATOR.compare(libVersion, outdatedInfo.outdatedAfterVersion) >= 0) {
                        val reportOnElement = reportOnElement(dependencyStatement, outdatedInfo)

                        registerError(
                                reportOnElement, outdatedInfo.message,
                                arrayOf(),
                                ProblemHighlightType.LIKE_DEPRECATED)

                        break
                    }
                }
            }
        }

        private fun reportOnElement(classpathEntry: GrCallExpression, outdatedInfo: OutdatedLibInfo): PsiElement {
            val indexOf = classpathEntry.text.indexOf(outdatedInfo.old.name)
            if (indexOf < 0) return classpathEntry

            return classpathEntry.findElementAt(indexOf) ?: classpathEntry
        }

        private fun libraryVersionFromOrderEntry(file: PsiFile, libraryId: String): String? {
            val module = ProjectRootManager.getInstance(file.project).fileIndex.getModuleForFile(file.virtualFile) ?: return null
            val libMarker = ":$libraryId:"

            for (moduleInGroup in module.getWholeModuleGroup().allModules()) {
                var libVersion: String? = null
                ModuleRootManager.getInstance(moduleInGroup).orderEntries().forEachLibrary { library ->
                    if (library.name?.contains(libMarker) == true) {
                        libVersion = library.name?.substringAfterLast(":")
                    }

                    // Continue if nothing is found
                    libVersion == null
                }

                if (libVersion != null) {
                    return libVersion
                }
            }

            return null
        }
    }
}