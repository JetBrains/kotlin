/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider.CompilerPluginType
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD
import org.jetbrains.kotlin.utils.addToStdlib.runIf

abstract class KaBaseSimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleNameReference(expression) {
    override val resolvesByNames: Collection<Name>
        get() {
            val element = element

            if (element is KtOperationReferenceExpression) {
                val tokenType = element.operationSignTokenType
                if (tokenType != null) {
                    val name = OperatorConventions.getNameForOperationSymbol(
                        tokenType, element.parent is KtUnaryExpression, element.parent is KtBinaryExpression
                    )
                        ?: (expression.parent as? KtBinaryExpression)?.let {
                            runIf(it.operationToken == KtTokens.EQ && isAssignmentResolved(element.project, it)) { ASSIGN_METHOD }
                        }
                        ?: return emptyList()

                    val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[tokenType]
                    return if (counterpart != null) {
                        val counterpartName = OperatorConventions.getNameForOperationSymbol(counterpart, false, true)!!
                        listOf(name, counterpartName)
                    } else {
                        listOf(name)
                    }
                }
            }

            return listOf(element.getReferencedNameAsName())
        }

    private fun isAssignmentResolved(project: Project, binaryExpression: KtBinaryExpression): Boolean {
        val sourceModule = ProjectStructureProvider.getModule(project, binaryExpression, contextualModule = null)
        if (sourceModule !is KtSourceModule) {
            return false
        }

        val reference = binaryExpression.operationReference.reference ?: return false
        val pluginPresenceService = project.getService(KotlinCompilerPluginsProvider::class.java)
            ?: error("KtAssignResolutionPresenceService is not available as a service")
        return pluginPresenceService.isPluginOfTypeRegistered(sourceModule, CompilerPluginType.ASSIGNMENT)
                && (reference.resolve() as? KtNamedFunction)?.nameAsName == ASSIGN_METHOD
    }
}
