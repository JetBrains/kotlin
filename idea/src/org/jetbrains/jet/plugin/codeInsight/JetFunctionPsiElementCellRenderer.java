/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.renderer.DescriptorRenderer;

public class JetFunctionPsiElementCellRenderer extends DefaultPsiElementCellRenderer {
    private final BindingContext bindingContext;

    public JetFunctionPsiElementCellRenderer(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    @Override
    public String getElementText(PsiElement element) {
        if (element instanceof JetNamedFunction) {
            JetNamedFunction function = (JetNamedFunction) element;
            SimpleFunctionDescriptor fd = bindingContext.get(BindingContext.FUNCTION, function);
            assert fd != null;
            return DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(fd);
        }
        return super.getElementText(element);
    }
}
