/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtCompilerPluginsProvider.CompilerPluginType
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD
import org.jetbrains.kotlin.utils.addToStdlib.runIf

abstract class KtSimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleReference<KtSimpleNameExpression>(expression) {
    // Extension point used by deprecated android extensions.
    abstract fun isReferenceToViaExtension(element: PsiElement): Boolean

    override fun isReferenceTo(candidateTarget: PsiElement): Boolean {
        if (!canBeReferenceTo(candidateTarget)) return false
        if (isReferenceToViaExtension(candidateTarget)) return true
        return super.isReferenceTo(candidateTarget)
    }

    override fun getRangeInElement(): TextRange {
        val element = element.getReferencedNameElement()
        val startOffset = getElement().startOffset
        return element.textRange.shiftRight(-startOffset)
    }

    override fun canRename(): Boolean {
        if (expression.getParentOfTypeAndBranch<KtWhenConditionInRange>(strict = true) { operationReference } != null) return false

        val elementType = expression.getReferencedNameElementType()
        if (elementType == KtTokens.PLUSPLUS || elementType == KtTokens.MINUSMINUS) return false

        return true
    }

    enum class ShorteningMode {
        NO_SHORTENING,
        DELAYED_SHORTENING,
        FORCED_SHORTENING
    }

    fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING): PsiElement =
        getKtReferenceMutateService().bindToElement(this, element, shorteningMode)

    fun bindToFqName(
        fqName: FqName,
        shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null
    ): PsiElement =
        getKtReferenceMutateService().bindToFqName(this, fqName, shorteningMode, targetElement)

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

    abstract fun getImportAlias(): KtImportAlias?

    private fun isAssignmentResolved(project: Project, binaryExpression: KtBinaryExpression): Boolean {
        val sourceModule = ProjectStructureProvider.getModule(project, binaryExpression, contextualModule = null)
        if (sourceModule !is KtSourceModule) {
            return false
        }

        val reference = binaryExpression.operationReference.reference ?: return false
        val pluginPresenceService = project.getService(KtCompilerPluginsProvider::class.java)
            ?: error("KtAssignResolutionPresenceService is not available as a service")
        return pluginPresenceService.isPluginOfTypeRegistered(sourceModule, CompilerPluginType.ASSIGNMENT)
                && (reference.resolve() as? KtNamedFunction)?.nameAsName == ASSIGN_METHOD
    }
}