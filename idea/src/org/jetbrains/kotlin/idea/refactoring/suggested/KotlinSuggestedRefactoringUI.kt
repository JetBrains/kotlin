package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.psi.PsiCodeFragment
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution.NewParameterValue
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import com.intellij.refactoring.suggested.SuggestedRefactoringUI
import com.intellij.refactoring.suggested.SignaturePresentationBuilder
import org.jetbrains.kotlin.psi.KtExpressionCodeFragment
import org.jetbrains.kotlin.psi.KtPsiFactory

object KotlinSuggestedRefactoringUI : SuggestedRefactoringUI() {
    override fun createSignaturePresentationBuilder(
        signature: Signature,
        otherSignature: Signature,
        isOldSignature: Boolean
    ): SignaturePresentationBuilder {
        return KotlinSignaturePresentationBuilder(signature, otherSignature, isOldSignature)
    }

    override fun extractNewParameterData(data: SuggestedChangeSignatureData): List<NewParameterData> {
        val newParameters = mutableListOf<NewParameterData>()

        val declaration = data.declaration
        val factory = KtPsiFactory(declaration.project)

        fun createCodeFragment() = factory.createExpressionCodeFragment("", declaration)

        if (data.newSignature.receiverType != null && data.oldSignature.receiverType == null) {
            newParameters.add(NewParameterData("<receiver>", createCodeFragment(), false/*TODO*/))
        }

        for (parameter in data.newSignature.parameters) {
            if (data.oldSignature.parameterById(parameter.id) == null) {
                newParameters.add(NewParameterData(parameter.name, createCodeFragment(), false/*TODO*/))
            }
        }

        return newParameters
    }

    override fun extractValue(fragment: PsiCodeFragment): NewParameterValue.Expression? {
        return (fragment as KtExpressionCodeFragment).getContentElement()
            ?.let { NewParameterValue.Expression(it) }
    }
}
