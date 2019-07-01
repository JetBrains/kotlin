/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IErrorCounterReparseableElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.KotlinParser;
import org.jetbrains.kotlin.psi.KtFunctionLiteral;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtParameterList;

class LambdaExpressionElementType extends IErrorCounterReparseableElementType {
    public LambdaExpressionElementType() {
        super("LAMBDA_EXPRESSION", KotlinLanguage.INSTANCE);
    }

    @Override
    public ASTNode parseContents(ASTNode chameleon) {
        Project project = chameleon.getPsi().getProject();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
                project, chameleon, null, KotlinLanguage.INSTANCE, chameleon.getChars());
        return KotlinParser.parseLambdaExpression(builder).getFirstChildNode();
    }

    @Override
    public ASTNode createNode(CharSequence text) {
        return new KtLambdaExpression(text);
    }

    @Override
    public boolean isParsable(@Nullable ASTNode parent, CharSequence buffer, Language fileLanguage, Project project) {
        return super.isParsable(parent, buffer, fileLanguage, project) &&
               !wasArrowMovedOrDeleted(parent, buffer) && !wasParameterCommaMovedOrDeleted(parent, buffer);
    }

    private static boolean wasArrowMovedOrDeleted(@Nullable ASTNode parent, CharSequence buffer) {
        KtLambdaExpression lambdaExpression = findLambdaExpression(parent);
        if (lambdaExpression == null) {
            return false;
        }

        KtFunctionLiteral literal = lambdaExpression.getFunctionLiteral();
        PsiElement arrow = literal.getArrow();

        // No arrow in original node
        if (arrow == null) return false;

        int arrowOffset = arrow.getStartOffsetInParent() + literal.getStartOffsetInParent();

        return hasTokenMoved(lambdaExpression.getText(), buffer, arrowOffset, KtTokens.ARROW);
    }

    private static boolean wasParameterCommaMovedOrDeleted(@Nullable ASTNode parent, CharSequence buffer) {
        KtLambdaExpression lambdaExpression = findLambdaExpression(parent);
        if (lambdaExpression == null) {
            return false;
        }

        KtFunctionLiteral literal = lambdaExpression.getFunctionLiteral();
        KtParameterList valueParameterList = literal.getValueParameterList();
        if (valueParameterList == null || valueParameterList.getParameters().size() <= 1) {
            return false;
        }

        PsiElement comma = valueParameterList.getFirstComma();
        if (comma == null) {
            return false;
        }

        int commaOffset = comma.getTextOffset() - lambdaExpression.getTextOffset();
        return hasTokenMoved(lambdaExpression.getText(), buffer, commaOffset, KtTokens.COMMA);
    }

    private static KtLambdaExpression findLambdaExpression(@Nullable ASTNode parent) {
        if (parent == null) return null;

        PsiElement parentPsi = parent.getPsi();
        KtLambdaExpression[] lambdaExpressions = PsiTreeUtil.getChildrenOfType(parentPsi, KtLambdaExpression.class);
        if (lambdaExpressions == null || lambdaExpressions.length != 1) return null;

        // Now works only when actual node can be spotted ambiguously. Need change in API.
        return lambdaExpressions[0];
    }

    private static boolean hasTokenMoved(String oldText, CharSequence buffer, int oldOffset, IElementType tokenType) {
        Lexer oldLexer = new KotlinLexer();
        oldLexer.start(oldText);

        Lexer newLexer = new KotlinLexer();
        newLexer.start(buffer);

        while (true) {
            IElementType oldType = oldLexer.getTokenType();
            if (oldType == null) break; // Didn't find an expected token. Consider it as no token was present.

            IElementType newType = newLexer.getTokenType();
            if (newType == null) return true; // New text was finished before reaching expected token in old text

            if (newType != oldType) {
                if (newType == KtTokens.WHITE_SPACE) {
                    newLexer.advance();
                    continue;
                }
                else if (oldType == KtTokens.WHITE_SPACE) {
                    oldLexer.advance();
                    continue;
                }

                return true; // Expected token was moved or deleted
            }

            if (oldType == tokenType && oldLexer.getCurrentPosition().getOffset() == oldOffset) {
                break;
            }

            oldLexer.advance();
            newLexer.advance();
        }

        return false;
    }

    @Override
    public int getErrorsCount(CharSequence seq, Language fileLanguage, Project project){
        return ElementTypeUtils.getKotlinBlockImbalanceCount(seq);
    }
}
