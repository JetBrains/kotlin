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

import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
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

object GradleHeuristicHelper {
    fun getHeuristicVersionInBuildScriptDependency(classpathStatement: GrCallExpression): String? {
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

    fun findStatementWithPrefix(closure: GrClosableBlock, prefix: String): List<GrCallExpression> {
        val applicationStatements = closure.getChildrenOfType<GrCallExpression>()

        val classPathStatements = ArrayList<GrCallExpression>()

        for (statement in applicationStatements) {
            val startExpression = statement.getChildrenOfType<GrReferenceExpression>().firstOrNull() ?: continue
            if (prefix == startExpression.text) {
                classPathStatements.add(statement)
            }
        }

        return classPathStatements
    }
}