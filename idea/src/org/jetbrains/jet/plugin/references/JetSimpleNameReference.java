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

package org.jetbrains.jet.plugin.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.plugin.completion.DescriptorLookupConverter;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

public class JetSimpleNameReference extends JetPsiReference {

    @NotNull
    private final JetSimpleNameExpression myExpression;

    public JetSimpleNameReference(@NotNull JetSimpleNameExpression jetSimpleNameExpression) {
        super(jetSimpleNameExpression);
        myExpression = jetSimpleNameExpression;
    }

    @NotNull
    public JetSimpleNameExpression getExpression() {
        return myExpression;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getElement().getTextLength());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) getExpression().getContainingFile());
        BindingContext bindingContext = ResolveSessionUtils.resolveToElement(resolveSession, getExpression());

        return DescriptorLookupConverter.collectLookupElements(
                resolveSession, bindingContext, TipsManager.getReferenceVariants(myExpression, bindingContext));
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        IElementType type = myExpression.getReferencedNameElementType();
        Project project = myExpression.getProject();
        PsiElement element;
        if (JetTokens.FIELD_IDENTIFIER == type) {
            element = JetPsiFactory.createFieldIdentifier(project, newElementName);
        }
        else if (JetTokens.LABEL_IDENTIFIER == type) {
            element = JetPsiFactory.createClassLabel(project, newElementName);
        }
        else {
            element = JetPsiFactory.createNameIdentifier(project, newElementName);
        }
        return myExpression.getReferencedNameElement().replace(element);
    }

    @Override
    public String toString() {
        return JetSimpleNameReference.class.getSimpleName() + ": " + myExpression.getText();
    }
}
