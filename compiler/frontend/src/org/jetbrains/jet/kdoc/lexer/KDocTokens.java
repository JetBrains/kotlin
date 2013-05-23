/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.kdoc.lexer;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.kdoc.parser.KDocParser;
import org.jetbrains.jet.kdoc.psi.impl.KDocImpl;
import org.jetbrains.jet.plugin.JetLanguage;

public interface KDocTokens {
    ILazyParseableElementType KDOC = new ILazyParseableElementType("KDoc", JetLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            PsiElement  parentElement = chameleon.getTreeParent().getPsi();
            Project     project = parentElement.getProject();
            PsiBuilder  builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, new KDocLexer(), getLanguage(),
                                                                                chameleon.getText());
            PsiParser   parser = new KDocParser();

            return parser.parse(this, builder).getFirstChildNode();
        }

        @Nullable
        @Override
        public ASTNode createNode(CharSequence text) {
            return new KDocImpl(text);
        }
    };

    KDocToken START                 = new KDocToken("KDOC_START");
    KDocToken END                   = new KDocToken("KDOC_END");
    KDocToken LEADING_ASTERISK      = new KDocToken("KDOC_LEADING_ASTERISK");

    KDocToken TEXT      = new KDocToken("KDOC_TEXT");
}
