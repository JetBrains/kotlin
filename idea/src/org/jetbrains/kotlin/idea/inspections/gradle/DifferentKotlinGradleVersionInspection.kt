/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.BooleanFunction
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.codeInspection.BaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCommandArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import java.util.*

class DifferentKotlinGradleVersionInspection : GradleBaseInspection() {
    var testVersionMessage: String? = null
        @TestOnly set

    override fun buildVisitor(): BaseInspectionVisitor = MyVisitor()

    override fun getGroupDisplayName() = BaseInspection.PROBABLE_BUGS

    override fun buildErrorString(vararg args: Any): String {
        return "Kotlin version that is used for building with Gradle (${args[0]}) differs from the one bundled into the IDE plugin (${args[1]})"
    }

    private inner class MyVisitor : BaseInspectionVisitor() {
        private val idePluginVersion by lazy { bundledRuntimeVersion() }

        override fun visitFile(file: GroovyFileBase?) {
            if (file == null || !FileUtilRt.extensionEquals(file.name, GradleConstants.EXTENSION)) return

            val fileIndex = ProjectRootManager.getInstance(file.project).fileIndex

            if (!ApplicationManager.getApplication().isUnitTestMode) {
                val module = fileIndex.getModuleForFile(file.virtualFile) ?: return
                if (!KotlinPluginUtil.isGradleModule(module)) return
            }

            if (fileIndex.isExcluded(file.virtualFile)) return

            super.visitFile(file)
        }

        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            val buildScriptCall = dependenciesCall.getStrictParentOfType<GrMethodCall>() ?: return
            if (buildScriptCall.invokedExpression.text != "buildscript") return

            val kotlinPluginStatement = findClassPathStatements(closure).filter {
                it.text.contains("org.jetbrains.kotlin:kotlin-gradle-plugin:")
            }.firstOrNull() ?: return

            val kotlinPluginVersion =
                    getHeuristicKotlinPluginVersion(kotlinPluginStatement) ?:
                    getResolvedKotlinGradleVersion(closure.project) ?:
                    return

            if (kotlinPluginVersion != idePluginVersion) {
                registerError(kotlinPluginStatement, kotlinPluginVersion, testVersionMessage ?: idePluginVersion)
            }
        }
    }

    companion object {
        private fun getHeuristicKotlinPluginVersion(classpathStatement: GrCallExpression): String? {
            val argument = classpathStatement.getChildrenOfType<GrCommandArgumentList>().firstOrNull() ?: return null
            val grLiteral = argument.children.firstOrNull()?.let { it as? GrLiteral } ?: return null

            if (grLiteral is GrString && grLiteral.injections.size == 1) {
                val versionInjection = grLiteral.injections.first() ?: return null
                val expression = versionInjection.expression as? GrReferenceExpression ?:
                                 versionInjection.closableBlock?.getChildrenOfType<GrReferenceExpression>()?.singleOrNull() ?:
                                 return null

                return resolveVariableInBuildScript(classpathStatement, expression.text)
            }

            val literalValue = grLiteral.value ?: return null
            val versionText = literalValue.toString().substringAfterLast(':')
            if (versionText.isEmpty()) return null

            return versionText
        }

        private fun resolveVariableInBuildScript(classpathStatement: GrCallExpression, name: String): String? {
            val dependenciesClosure = classpathStatement.getStrictParentOfType<GrClosableBlock>() ?: return null
            val buildScriptClosure = dependenciesClosure.getStrictParentOfType<GrClosableBlock>() ?: return null

            for (child in buildScriptClosure.children) {
                when (child) {
                    is GrAssignmentExpression -> {
                        if (child.lValue.text == "ext.$name") {
                            val assignValue = child.rValue
                            if (assignValue is GrLiteral) {
                                return assignValue.value.toString()
                            }
                        }
                    }
                    is GrVariableDeclaration -> {
                        for (variable in child.variables) {
                            if (variable.name == name) {
                                val assignValue = variable.initializerGroovy
                                if (assignValue is GrLiteral) {
                                    return assignValue.value.toString()
                                }
                            }
                        }
                    }
                }
            }

            return null
        }

        private fun findClassPathStatements(closure: GrClosableBlock): List<GrCallExpression> {
            val applicationStatements = closure.getChildrenOfType<GrCallExpression>()

            val classPathStatements = ArrayList<GrCallExpression>()

            for (statement in applicationStatements) {
                val startExpression = statement.getChildrenOfType<GrReferenceExpression>().firstOrNull() ?: continue
                if ("classpath" == startExpression.text) {
                    classPathStatements.add(statement)
                }
            }

            return classPathStatements
        }

        private fun getResolvedKotlinGradleVersion(project: Project): String? {
            val projectPath = project.basePath ?: return null
            val projectInfo = ExternalSystemUtil.getExternalProjectInfo(project, GRADLE_SYSTEM_ID, projectPath) ?: return null
            val externalProjectStructure = projectInfo.externalProjectStructure ?: return null

            val buildScriptClasspathDataNode = ExternalSystemApiUtil.findFirstRecursively(externalProjectStructure, BooleanFunction { node ->
                BuildScriptClasspathData.KEY == node.key
            }) ?: return null

            val buildScriptClasspathData = buildScriptClasspathDataNode.getData(BuildScriptClasspathData.KEY) ?: return null
            return findKotlinPluginVersion(buildScriptClasspathData)
        }

        private fun findKotlinPluginVersion(classpathData: BuildScriptClasspathData): String? {
            for (classPathEntry in classpathData.classpathEntries) {
                for (path in classPathEntry.classesFile) {
                    val uniformedPath = path.replace('\\', '/')
                    if (uniformedPath.contains("org.jetbrains.kotlin/kotlin-gradle-plugin/")) {
                        val versionSubstring = uniformedPath.substringAfter("org.jetbrains.kotlin/kotlin-gradle-plugin/").substringBefore('/', "<error>")
                        if (versionSubstring != "<error>") {
                            return versionSubstring;
                        }
                    }
                }
            }

            return null
        }
    }
}