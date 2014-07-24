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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class ChangeToConstructorInvocationFix extends JetIntentionAction<JetDelegatorToSuperClass> {

    public ChangeToConstructorInvocationFix(@NotNull JetDelegatorToSuperClass element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.constructor.invocation");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.constructor.invocation");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        JetTypeReference typeReference = element.getTypeReference();
        BindingContext context = ResolvePackage.getBindingContext(typeReference);
        JetType supertype = context.get(BindingContext.TYPE, typeReference);
        if (supertype == null) return false;
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
        if (classDescriptor == null) return false;
        for (ConstructorDescriptor constructor : classDescriptor.getConstructors()) {
            boolean needsParametersPassed = false;
            for (ValueParameterDescriptor parameter : constructor.getValueParameters()) {
                needsParametersPassed |= !parameter.hasDefaultValue() && parameter.getVarargElementType() == null;
            }
            if (!needsParametersPassed) return true;
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetDelegatorToSuperClass delegator = (JetDelegatorToSuperClass) element.copy();
        JetClass aClass = JetPsiFactory(file).createClass("class A : " + delegator.getText() + "()");
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        assert delegationSpecifiers.size() == 1;
        JetDelegationSpecifier specifier = delegationSpecifiers.iterator().next();
        element.replace(specifier);
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetDelegatorToSuperClass> createAction(Diagnostic diagnostic) {
                if (diagnostic.getPsiElement() instanceof JetDelegatorToSuperClass) {
                    return new ChangeToConstructorInvocationFix((JetDelegatorToSuperClass) diagnostic.getPsiElement());
                }
                return null;
            }
        };
    }
}
