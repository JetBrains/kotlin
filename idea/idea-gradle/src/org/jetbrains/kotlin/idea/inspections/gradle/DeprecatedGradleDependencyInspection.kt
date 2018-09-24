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
import org.jetbrains.kotlin.idea.configuration.allModules
import org.jetbrains.kotlin.idea.configuration.getWholeModuleGroup
import org.jetbrains.kotlin.idea.configuration.parseExternalLibraryName
import org.jetbrains.kotlin.idea.inspections.ReplaceStringInDocumentFix
import org.jetbrains.kotlin.idea.inspections.gradle.GradleHeuristicHelper.PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.versions.DEPRECATED_LIBRARIES_INFORMATION
import org.jetbrains.kotlin.idea.versions.DeprecatedLibInfo
import org.jetbrains.kotlin.idea.versions.LibInfo
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

private val LibInfo.gradleMarker get() = "$groupId:$name"

class DeprecatedGradleDependencyInspection : GradleBaseInspection() {
    override fun buildVisitor(): BaseInspectionVisitor = DependencyFinder()

    private open class DependencyFinder : KotlinGradleInspectionVisitor() {
        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            val dependencyEntries = GradleHeuristicHelper.findStatementWithPrefixes(
                closure, PRODUCTION_DEPENDENCY_STATEMENTS)
            for (dependencyStatement in dependencyEntries) {
                visitDependencyEntry(dependencyStatement)
            }
        }

        open fun visitDependencyEntry(dependencyStatement: GrCallExpression) {
            for (outdatedInfo in DEPRECATED_LIBRARIES_INFORMATION) {
                val dependencyText = dependencyStatement.text
                val libMarker = outdatedInfo.old.gradleMarker

                if (dependencyText.contains(libMarker)) {
                    val afterMarkerChar = dependencyText.substringAfter(libMarker).getOrNull(0)
                    if (!(afterMarkerChar == '\'' || afterMarkerChar == '"' || afterMarkerChar == ':')) {
                        continue
                    }

                    val libVersion =
                        DifferentStdlibGradleVersionInspection.getResolvedLibVersion(
                            dependencyStatement.containingFile, outdatedInfo.old.groupId, listOf(outdatedInfo.old.name)
                        ) ?: libraryVersionFromOrderEntry(dependencyStatement.containingFile, outdatedInfo.old.name)


                    if (libVersion != null && VersionComparatorUtil.COMPARATOR.compare(
                            libVersion,
                            outdatedInfo.outdatedAfterVersion
                        ) >= 0
                    ) {
                        val reportOnElement = reportOnElement(dependencyStatement, outdatedInfo)

                        registerError(
                            reportOnElement, outdatedInfo.message,
                            arrayOf(ReplaceStringInDocumentFix(reportOnElement, outdatedInfo.old.name, outdatedInfo.new.name)),
                            ProblemHighlightType.LIKE_DEPRECATED
                        )

                        break
                    }
                }
            }
        }

        private fun reportOnElement(classpathEntry: GrCallExpression, deprecatedInfo: DeprecatedLibInfo): PsiElement {
            val indexOf = classpathEntry.text.indexOf(deprecatedInfo.old.name)
            if (indexOf < 0) return classpathEntry

            return classpathEntry.findElementAt(indexOf) ?: classpathEntry
        }

    }

    companion object {
        fun libraryVersionFromOrderEntry(file: PsiFile, libraryId: String): String? {
            val module = ProjectRootManager.getInstance(file.project).fileIndex.getModuleForFile(file.virtualFile) ?: return null
            val libMarker = ":$libraryId:"

            for (moduleInGroup in module.getWholeModuleGroup().allModules()) {
                var libVersion: String? = null
                ModuleRootManager.getInstance(moduleInGroup).orderEntries().forEachLibrary { library ->
                    if (library.name?.contains(libMarker) == true) {
                        libVersion = parseExternalLibraryName(library)?.version
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