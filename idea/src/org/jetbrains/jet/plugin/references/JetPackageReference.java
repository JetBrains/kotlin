/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.TipsManager;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.completion.DescriptorLookupConverter;

/**
 * @author Nikolay Krasko
 */
public class JetPackageReference extends JetPsiReference {

    private JetNamespaceHeader packageExpression;

    public JetPackageReference(@NotNull JetNamespaceHeader expression) {
        super(expression);
        packageExpression = expression;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(
                (JetFile) myExpression.getContainingFile());

        return DescriptorLookupConverter.collectLookupElements(
                bindingContext, TipsManager.getReferenceVariants(packageExpression, bindingContext));
    }

    @NotNull
    @Override
    public TextRange getRangeInElement() {
        return new TextRange(0, getElement().getTextLength());
    }

    @NotNull
    public JetNamespaceHeader getExpression() {
        return packageExpression;
    }
}
