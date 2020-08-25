package com.jetbrains.konan.debugger

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil.getNonStrictParentOfType
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.LLValue
import com.jetbrains.cidr.execution.debugger.evaluation.CidrDebuggerTypesHelper
import com.jetbrains.cidr.execution.debugger.evaluation.CidrMemberValue
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression

class KonanDebuggerTypesHelper(process: CidrDebugProcess) : CidrDebuggerTypesHelper(process) {
    override fun computeSourcePosition(value: CidrMemberValue): XSourcePosition? = null
    override fun resolveProperty(value: CidrMemberValue, dynamicTypeName: String?): XSourcePosition? = null
    override fun createReferenceFromText(`var`: LLValue, context: PsiElement): PsiReference? = null
    override fun isImplicitContextVariable(position: XSourcePosition, `var`: LLValue): Boolean? = null

    override fun resolveToDeclaration(position: XSourcePosition?, value: LLValue): PsiElement? {
        val blockExpression = findBlockAtPosition(position ?: return null, myProcess.project) ?: return null
        val referenceText = if (value.name == "_this") "this" else value.name
        val codeFragment =
            KtPsiFactory(myProcess.project, false).createExpressionCodeFragment(referenceText, blockExpression).getContentElement()
        val referenceExpression = if (referenceText == "this") {
            (codeFragment as? KtThisExpression)?.instanceReference
        } else {
            codeFragment as? KtReferenceExpression
        } ?: return null

        val declaration = referenceExpression.mainReference.resolve() ?: return null
        // Debugger provides information for local variables even if they are not initialized.
        // Don't resolve uninitialized variables to avoid showing garbage values.
        // TODO: use control flow info to check if variable may be initialized at offset.
        return declaration.takeIf { it.textRange.startOffset <= position.offset }

    }

    companion object {
        private fun findBlockAtPosition(position: XSourcePosition, project: Project): KtBlockExpression? =
            getNonStrictParentOfType(getContextElement(position, project), KtBlockExpression::class.java)
    }
}
