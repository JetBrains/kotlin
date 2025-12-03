/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider.CompilerPluginType
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@KaImplementationDetail
@OptIn(KtImplementationDetail::class)
abstract class KaBaseSimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleNameReference(expression) {
    override val resolvesByNames: Collection<Name>
        get() {
            val element = element
            val specialNames = when (element) {
                is KtOperationReferenceExpression -> operatorNames(element)

                // According to the KDoc, labels and `this`/`super` references cannot be properly expressed in terms of this API
                is KtNameReferenceExpression if (element.parent is KtInstanceExpressionWithLabel) -> emptyList()
                is KtLabelReferenceExpression -> emptyList()
                else -> null
            }

            if (specialNames != null) {
                return specialNames
            }

            return listOf(element.getReferencedNameAsName())
        }
}

private fun operatorNames(expression: KtOperationReferenceExpression): Collection<Name>? = buildList {
    val tokenType = expression.operationSignTokenType ?: return null

    val parent = expression.parent
    val name = OperatorConventions.getNameForOperationSymbol(
        tokenType, parent is KtUnaryExpression, parent is KtBinaryExpression
    ) ?: (parent as? KtBinaryExpression)?.let {
        runIf(it.operationToken == KtTokens.EQ && isAssignmentResolved(expression.project, it)) { ASSIGN_METHOD }
    }

    if (name != null) {
        add(name)
        val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[tokenType]
        if (counterpart != null) {
            val counterpartName = OperatorConventions.getNameForOperationSymbol(counterpart, false, true)!!
            add(counterpartName)
        }
    }

    val isArrayModification = when (parent) {
        is KtBinaryExpression if tokenType in KtTokens.ALL_ASSIGNMENTS ->
            parent.left?.unwrapParenthesesLabelsAndAnnotations()

        is KtUnaryExpression if tokenType in KtTokens.INCREMENT_AND_DECREMENT ->
            parent.baseExpression?.unwrapParenthesesLabelsAndAnnotations()

        else -> null
    } is KtArrayAccessExpression

    if (isArrayModification) {
        add(OperatorNameConventions.SET)
    }
}

private fun isAssignmentResolved(project: Project, binaryExpression: KtBinaryExpression): Boolean {
    val sourceModule = KotlinProjectStructureProvider.getModule(project, binaryExpression, useSiteModule = null)
    if (sourceModule !is KaSourceModule) {
        return false
    }

    val reference = binaryExpression.operationReference.reference ?: return false
    val compilerPluginsProvider = KotlinCompilerPluginsProvider.getInstance(project) ?: return false
    return compilerPluginsProvider.isPluginOfTypeRegistered(sourceModule, CompilerPluginType.ASSIGNMENT)
            && (reference.resolve() as? KtNamedFunction)?.nameAsName == ASSIGN_METHOD
}
