package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.SuggestedRefactoringState
import com.intellij.refactoring.suggested.SuggestedRefactoringStateChanges
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.children

class KotlinSuggestedRefactoringStateChanges(refactoringSupport: SuggestedRefactoringSupport) :
    SuggestedRefactoringStateChanges(refactoringSupport)
{
    override fun createInitialState(declaration: PsiElement): SuggestedRefactoringState? {
        declaration as KtDeclaration
        if (declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null // currently not supported
        if (declaration.hasModifier(KtTokens.ACTUAL_KEYWORD)) return null // currently not supported
        return super.createInitialState(declaration)
    }

    override fun signature(declaration: PsiElement, prevState: SuggestedRefactoringState?): Signature? {
        declaration as KtDeclaration
        val name = declaration.name ?: return null
        if (declaration !is KtCallableDeclaration || KotlinSuggestedRefactoringSupport.isOnlyRenameSupported(declaration)) {
            return Signature.create(name, null, emptyList(), null)
        }

        val parameters = declaration.valueParameters.map { it.extractParameterData() ?: return null }
        val type = declaration.typeReference?.text
        val receiverType = declaration.receiverTypeReference?.text
        val signature = Signature.create(
            name,
            type,
            parameters,
            KotlinSignatureAdditionalData(DeclarationType.fromDeclaration(declaration), receiverType)
        ) ?: return null

        return if (prevState == null) signature else matchParametersWithPrevState(signature, declaration, prevState)
    }

    override fun parameterMarkerRanges(declaration: PsiElement): List<TextRange?> {
        if (declaration !is KtFunction) return emptyList()
        return declaration.valueParameters.map { it.colon?.textRange }
    }

    private fun KtParameter.extractParameterData(): Parameter? {
        val modifiers = modifierList?.node?.children()
            ?.filter { it.elementType is KtModifierKeywordToken && it.text in modifiersToInclude }
            ?.joinToString(separator = " ") { it.text } ?: ""
        return Parameter(
            Any(),
            name ?: return null,
            typeReference?.text ?: return null,
            KotlinParameterAdditionalData(defaultValue?.text/*TODO: strip comments etc*/, modifiers)
        )
    }

    private val modifiersToInclude = setOf(KtTokens.VARARG_KEYWORD.value)
}