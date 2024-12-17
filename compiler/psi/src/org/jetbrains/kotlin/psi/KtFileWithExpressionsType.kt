package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParser

class KtFileWithExpressionsType : IFileElementType(NAME, KotlinLanguage.INSTANCE) {
    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? {
        val project = psi.project
        val languageForParser = getLanguageForParser(psi)
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.chars)
        return KotlinParser.parseFileWithExpressions(builder).firstChildNode
    }

    companion object {
        private const val NAME = "kotlin.FILE_WITH_EXPRESSIONS"
    }
}

