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

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class JetParser implements PsiParser {

    private final JetScriptDefinitionProvider scriptDefinitionProvider;

    public JetParser(Project project) {
        scriptDefinitionProvider = JetScriptDefinitionProvider.getInstance(project);
    }

    @Override
    @NotNull
    public ASTNode parse(IElementType iElementType, PsiBuilder psiBuilder) {
        throw new IllegalStateException("use another parse");
    }

    // we need this method because we need psiFile
    @NotNull
    public ASTNode parse(IElementType iElementType, PsiBuilder psiBuilder, PsiFile psiFile) {
        JetParsing jetParsing = JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder));
        if (scriptDefinitionProvider != null && scriptDefinitionProvider.isScript(psiFile)
            || psiFile.getName().endsWith(JetParserDefinition.STD_SCRIPT_EXT)) {
            jetParsing.parseScript();
        }
        else {
            jetParsing.parseFile();
        }
        return psiBuilder.getTreeBuilt();
    }

    @NotNull
    public static ASTNode parseTypeCodeFragment(PsiBuilder psiBuilder) {
        JetParsing jetParsing = JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder));
        jetParsing.parseTypeCodeFragment();
        return psiBuilder.getTreeBuilt();
    }

    @NotNull
    public static ASTNode parseExpressionCodeFragment(PsiBuilder psiBuilder) {
        JetParsing jetParsing = JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder));
        jetParsing.parseExpressionCodeFragment();
        return psiBuilder.getTreeBuilt();
    }

    @NotNull
    public static ASTNode parseBlockCodeFragment(PsiBuilder psiBuilder) {
        JetParsing jetParsing = JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder));
        jetParsing.parseBlockCodeFragment();
        return psiBuilder.getTreeBuilt();
    }
}
