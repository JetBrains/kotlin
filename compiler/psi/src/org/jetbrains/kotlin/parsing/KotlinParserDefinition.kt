/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.parsing

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements.*
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementType
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

/**
 * Creates [org.jetbrains.kotlin.psi.KtCommonFile] when java psi is not available e.g. on JB Client.
 * Otherwise, normal [KotlinParserDefinition] should be used.
 */
open class KotlinCommonParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = KotlinLexer()

    override fun createParser(project: Project): PsiParser = KotlinParser(project)

    override fun getFileNodeType(): IFileElementType = KtFileElementType.INSTANCE

    override fun getWhitespaceTokens(): TokenSet = KtTokens.WHITESPACES

    override fun getCommentTokens(): TokenSet = KtTokens.COMMENTS

    override fun getStringLiteralElements(): TokenSet = KtTokens.STRINGS

    override fun createElement(astNode: ASTNode): PsiElement {
        val elementType = astNode.elementType

        return when (elementType) {
            is KtStubElementType<*, *> -> elementType.createPsiFromAst(astNode)
            KtNodeTypes.TYPE_CODE_FRAGMENT, KtNodeTypes.EXPRESSION_CODE_FRAGMENT, KtNodeTypes.BLOCK_CODE_FRAGMENT -> ASTWrapperPsiElement(
                astNode
            )
            is KDocElementType -> elementType.createPsi(astNode)
            KDocTokens.MARKDOWN_LINK -> KDocLink(astNode)
            else -> (elementType as KtNodeType).createPsi(astNode)
        }
    }

    @Suppress("DEPRECATION")
    override fun createFile(fileViewProvider: FileViewProvider): PsiFile = org.jetbrains.kotlin.psi.KtCommonFile(fileViewProvider, false)

    @Deprecated("Deprecated in Java")
    override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
        val rightTokenType = right.elementType

        // get/set from a new line
        if (rightTokenType == KtTokens.GET_KEYWORD || rightTokenType == KtTokens.SET_KEYWORD) {
            return MUST_LINE_BREAK
        }

        val leftTokenType = left.elementType

        if (leftTokenType is KtKeywordToken && rightTokenType is KtKeywordToken) return MUST

        // When entry from a new line
        val rightWhenEntry = right.psi.getNonStrictParentOfType<KtWhenEntry>()
        if (rightWhenEntry != null) {
            val leftWhenEntry = left.psi.getNonStrictParentOfType<KtWhenEntry>()
            if (leftWhenEntry != null && leftWhenEntry != rightWhenEntry && leftTokenType != KtTokens.SEMICOLON) {
                return MUST_LINE_BREAK
            }
        }

        // Default
        return MAY
    }
}

class KotlinParserDefinition : KotlinCommonParserDefinition() {
    override fun createFile(fileViewProvider: FileViewProvider): PsiFile {
        return KtFile(fileViewProvider, false)
    }

    companion object {

        @JvmField
        val STD_SCRIPT_SUFFIX = "kts"

        @JvmField
        val STD_SCRIPT_EXT = "." + STD_SCRIPT_SUFFIX

        val instance: KotlinParserDefinition
            get() = LanguageParserDefinitions.INSTANCE.forLanguage(KotlinLanguage.INSTANCE) as KotlinParserDefinition
    }
}
