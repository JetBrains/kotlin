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

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.configuration.KOTLIN_GROUP_ID
import org.jetbrains.kotlin.idea.inspections.gradle.GradleHeuristicHelper.PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

class DifferentStdlibGradleVersionInspection : GradleBaseInspection() {
    override fun buildVisitor(): BaseInspectionVisitor = MyVisitor(KOTLIN_GROUP_ID, JvmIdePlatformKind.tooling.mavenLibraryIds)

    override fun buildErrorString(vararg args: Any) =
        "Plugin version (${args[0]}) is not the same as library version (${args[1]})"

    private abstract class VersionFinder(private val groupId: String, private val libraryIds: List<String>) :
        KotlinGradleInspectionVisitor() {
        protected abstract fun onFound(stdlibVersion: String, stdlibStatement: GrCallExpression)

        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            if (dependenciesCall.parent !is PsiFile) return

            val stdlibStatement = findLibraryStatement(closure, "org.jetbrains.kotlin", libraryIds) ?: return
            val stdlibVersion = getResolvedLibVersion(closure.containingFile, groupId, libraryIds) ?: return

            onFound(stdlibVersion, stdlibStatement)
        }
    }

    private inner class MyVisitor(groupId: String, libraryIds: List<String>) : VersionFinder(groupId, libraryIds) {
        override fun onFound(stdlibVersion: String, stdlibStatement: GrCallExpression) {
            val gradlePluginVersion = getResolvedKotlinGradleVersion(stdlibStatement.containingFile)

            if (stdlibVersion != gradlePluginVersion) {
                registerError(stdlibStatement, gradlePluginVersion, stdlibVersion)
            }
        }
    }

    companion object {
        private fun findLibraryStatement(closure: GrClosableBlock, libraryGroup: String, libraryIds: List<String>): GrCallExpression? {
            return GradleHeuristicHelper.findStatementWithPrefixes(closure, PRODUCTION_DEPENDENCY_STATEMENTS).firstOrNull { statement ->
                libraryIds.any { it in statement.text } && statement.text.contains(libraryGroup)
            }
        }

        fun getResolvedLibVersion(file: PsiFile, groupId: String, libraryIds: List<String>): String? {
            val projectStructureNode = findGradleProjectStructure(file) ?: return null
            val module = ProjectRootManager.getInstance(file.project).fileIndex.getModuleForFile(file.virtualFile) ?: return null

            for (moduleData in projectStructureNode.findAll(ProjectKeys.MODULE).filter { it.data.internalName == module.name }) {
                moduleData.node.getResolvedVersionByModuleData(groupId, libraryIds)?.let {
                    return it
                }
            }

            return null
        }
    }
}