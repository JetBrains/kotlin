/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementType;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

public class JetParserDefinition implements ParserDefinition {

    public JetParserDefinition() {
        //todo: ApplicationManager.getApplication() is null during JetParsingTest setting up

        /*if (!ApplicationManager.getApplication().isCommandLine()) {
        }*/
    }

    @NotNull
    public Lexer createLexer(Project project) {
        return new JetLexer();
    }

    public PsiParser createParser(Project project) {
        return new JetParser();
    }

    public IFileElementType getFileNodeType() {
        return JetStubElementTypes.FILE;
    }

    @NotNull
    public TokenSet getWhitespaceTokens() {
        return JetTokens.WHITESPACES;
    }

    @NotNull
    public TokenSet getCommentTokens() {
        return JetTokens.COMMENTS;
    }

    @NotNull
    public TokenSet getStringLiteralElements() {
        return JetTokens.STRINGS;
    }

    @NotNull
    public PsiElement createElement(ASTNode astNode) {
        if (astNode.getElementType() instanceof JetStubElementType) {
            return ((JetStubElementType) astNode.getElementType()).createPsiFromAst(astNode);
        }

        return ((JetNodeType) astNode.getElementType()).createPsi(astNode);
    }

    public PsiFile createFile(FileViewProvider fileViewProvider) {
        return new JetFile(fileViewProvider);
    }

    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
        return SpaceRequirements.MAY;
    }
}
