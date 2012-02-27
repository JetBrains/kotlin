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

package org.jetbrains.jet.plugin.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author yole
 */
public class JetFormattingModelBuilder implements FormattingModelBuilder {
    @NotNull
    @Override
    public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
        final JetBlock block = new JetBlock(
            element.getNode(), null, Indent.getNoneIndent(), null, settings,
            createSpacingBuilder(settings));

        return FormattingModelProvider.createFormattingModelForPsiFile(
            element.getContainingFile(), block, settings);
    }

    private static SpacingBuilder createSpacingBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings)
                .after(NAMESPACE_HEADER).blankLines(1)

                .before(IMPORT_DIRECTIVE).lineBreakInCode()
                .between(IMPORT_DIRECTIVE, CLASS).blankLines(1)
                .between(IMPORT_DIRECTIVE, FUN).blankLines(1)
                .between(IMPORT_DIRECTIVE, PROPERTY).blankLines(1)

                .before(FUN).lineBreakInCode()
                .before(PROPERTY).lineBreakInCode()
                .between(FUN, FUN).blankLines(1)
                .between(FUN, PROPERTY).blankLines(1)

                .before(COMMA).spaceIf(settings.SPACE_BEFORE_COMMA)
                .after(COMMA).spaceIf(settings.SPACE_AFTER_COMMA)
                .around(EQ).spaceIf(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
                .beforeInside(BLOCK, FUN).spaceIf(settings.SPACE_BEFORE_METHOD_LBRACE)
                .afterInside(LBRACE, BLOCK).lineBreakInCode()
                .beforeInside(RBRACE, CLASS_BODY).lineBreakInCode()
                .beforeInside(RBRACE, BLOCK).lineBreakInCode();
    }

    @Override
    public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode) {
        return null;
    }
}
