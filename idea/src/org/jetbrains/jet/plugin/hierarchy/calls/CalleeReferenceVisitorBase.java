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

package org.jetbrains.jet.plugin.hierarchy.calls;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;

public abstract class CalleeReferenceVisitorBase extends JetTreeVisitorVoid {
    private final BindingContext bindingContext;
    private final boolean deepTraversal;

    protected CalleeReferenceVisitorBase(BindingContext bindingContext, boolean deepTraversal) {
        this.bindingContext = bindingContext;
        this.deepTraversal = deepTraversal;
    }

    protected abstract void processDeclaration(JetReferenceExpression reference, PsiElement declaration);

    @Override
    public void visitJetElement(@NotNull JetElement element) {
        if (deepTraversal || !(element instanceof JetClassOrObject || element instanceof JetNamedFunction)) {
            super.visitJetElement(element);
        }
    }

    @Override
    public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
        if (descriptor == null) return;

        PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        if (declaration == null) return;

        if (isProperty(descriptor, declaration) || isCallable(descriptor, declaration, expression)) {
            processDeclaration(expression, declaration);
        }
    }

    // Accept callees of JetCallElement which refer to Kotlin function, Kotlin class or Java method
    private static boolean isCallable(DeclarationDescriptor descriptor, PsiElement declaration, JetSimpleNameExpression reference) {
        JetCallElement callElement = PsiTreeUtil.getParentOfType(reference, JetCallElement.class);
        if (callElement == null || !PsiTreeUtil.isAncestor(callElement.getCalleeExpression(), reference, false)) return false;

        return descriptor instanceof FunctionDescriptor
                 && (declaration instanceof JetClassOrObject
                     || declaration instanceof JetNamedFunction
                     || declaration instanceof PsiMethod);
    }

    // Accept only properties (not local variables or references to Java fields)
    private static boolean isProperty(DeclarationDescriptor descriptor, PsiElement declaration) {
        return descriptor instanceof PropertyDescriptor && declaration instanceof JetProperty;
    }
}
