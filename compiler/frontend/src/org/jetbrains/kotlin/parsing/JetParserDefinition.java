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

package org.jetbrains.kotlin.parsing;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeType;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.kdoc.parser.KDocElementType;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class JetParserDefinition implements ParserDefinition {
    public static final String STD_SCRIPT_SUFFIX = "kts";
    public static final String STD_SCRIPT_EXT = "." + STD_SCRIPT_SUFFIX;

    public JetParserDefinition() {
        //todo: ApplicationManager.getApplication() is null during JetParsingTest setting up

        /*if (!ApplicationManager.getApplication().isCommandLine()) {
        }*/
    }

    @NotNull
    public static JetParserDefinition getInstance() {
        return (JetParserDefinition)LanguageParserDefinitions.INSTANCE.forLanguage(KotlinLanguage.INSTANCE);
    }

    @Override
    @NotNull
    public Lexer createLexer(Project project) {
        return new KotlinLexer();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new JetParser(project);
    }

    @Override
    public IFileElementType getFileNodeType() {
        return KtStubElementTypes.FILE;
    }

    @Override
    @NotNull
    public TokenSet getWhitespaceTokens() {
        return KtTokens.WHITESPACES;
    }

    @Override
    @NotNull
    public TokenSet getCommentTokens() {
        return KtTokens.COMMENTS;
    }

    @Override
    @NotNull
    public TokenSet getStringLiteralElements() {
        return KtTokens.STRINGS;
    }

    @Override
    @NotNull
    public PsiElement createElement(ASTNode astNode) {
        IElementType elementType = astNode.getElementType();

        if (elementType instanceof KtStubElementType) {
            return ((KtStubElementType) elementType).createPsiFromAst(astNode);
        }
        else if (elementType == KtNodeTypes.TYPE_CODE_FRAGMENT ||
                 elementType == KtNodeTypes.EXPRESSION_CODE_FRAGMENT ||
                 elementType == KtNodeTypes.BLOCK_CODE_FRAGMENT) {
            return new ASTWrapperPsiElement(astNode);
        }
        else if (elementType instanceof KDocElementType) {
            return ((KDocElementType) elementType).createPsi(astNode);
        }
        else if (elementType == KDocTokens.MARKDOWN_LINK) {
            return new KDocLink(astNode);
        }
        else {
            return ((KtNodeType) elementType).createPsi(astNode);
        }
    }

    @Override
    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new KtFile(fileViewProvider, false);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        IElementType rightTokenType = astNode1.getElementType();
        if (rightTokenType == KtTokens.GET_KEYWORD || rightTokenType == KtTokens.SET_KEYWORD) {
            return SpaceRequirements.MUST_LINE_BREAK;
        }
        return SpaceRequirements.MAY;
    }
}
