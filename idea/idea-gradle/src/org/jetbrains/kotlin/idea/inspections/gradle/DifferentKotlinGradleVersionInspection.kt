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

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.inspections.PluginVersionDependentInspection
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
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

class DifferentKotlinGradleVersionInspection : GradleBaseInspection(), PluginVersionDependentInspection{
    override var testVersionMessage: String? = null
        @TestOnly set

    override fun buildVisitor(): BaseInspectionVisitor = MyVisitor()

    override fun getGroupDisplayName() = BaseInspection.PROBABLE_BUGS

    override fun buildErrorString(vararg args: Any): String {
        return "Kotlin version that is used for building with Gradle (${args[0]}) differs from the one bundled into the IDE plugin (${args[1]})"
    }

    private abstract class VersionFinder : KotlinGradleInspectionVisitor() {
        protected abstract fun onFound(kotlinPluginVersion: String, kotlinPluginStatement: GrCallExpression)

        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            val buildScriptCall = dependenciesCall.getStrictParentOfType<GrMethodCall>() ?: return
            if (buildScriptCall.invokedExpression.text != "buildscript") return

            val kotlinPluginStatement = findClassPathStatements(closure).firstOrNull {
                it.text.contains(KOTLIN_PLUGIN_CLASSPATH_MARKER)
            } ?: return

            val kotlinPluginVersion =
                    getHeuristicKotlinPluginVersion(kotlinPluginStatement) ?:
                    getResolvedKotlinGradleVersion(closure.containingFile) ?:
                    return

            onFound(kotlinPluginVersion, kotlinPluginStatement)
        }
    }

    private inner class MyVisitor: VersionFinder() {
        private val idePluginVersion by lazy { bundledRuntimeVersion() }

        override fun onFound(kotlinPluginVersion: String, kotlinPluginStatement: GrCallExpression) {
            if (kotlinPluginVersion != idePluginVersion) {
                registerError(kotlinPluginStatement, kotlinPluginVersion, testVersionMessage ?: idePluginVersion)
            }
        }
    }

    companion object {
        fun getKotlinPluginVersion(gradleFile: GroovyFileBase): String? {
            var version: String? = null
            val visitor = object : VersionFinder() {
                override fun visitElement(element: GroovyPsiElement) {
                    element.acceptChildren(this)
                }

                override fun onFound(kotlinPluginVersion: String, kotlinPluginStatement: GrCallExpression) {
                    version = kotlinPluginVersion
                }
            }
            gradleFile.accept(visitor)
            return version
        }

        private fun getHeuristicKotlinPluginVersion(classpathStatement: GrCallExpression): String? {
            val argumentList = when (classpathStatement) {
                is GrMethodCall -> classpathStatement.argumentList // classpath('argument')
                else -> classpathStatement.getChildrenOfType<GrCommandArgumentList>().singleOrNull() // classpath 'argument'
            } ?: return null
            val grLiteral = argumentList.children.firstOrNull() as? GrLiteral ?: return null

            if (grLiteral is GrString && grLiteral.injections.size == 1) {
                val versionInjection = grLiteral.injections.first() ?: return null
                val expression = versionInjection.expression as? GrReferenceExpression ?: // $some_variable
                                 versionInjection.closableBlock?.getChildrenOfType<GrReferenceExpression>()?.singleOrNull() ?: // ${some_variable}
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
                        if (child.lValue.text == "ext.$name") { // ext.variable = '1.0.0'
                            val assignValue = child.rValue
                            if (assignValue is GrLiteral) {
                                return assignValue.value.toString()
                            }
                        }
                    }
                    is GrVariableDeclaration -> {
                        for (variable in child.variables) { // def variable = '1.0.0'
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
    }
}