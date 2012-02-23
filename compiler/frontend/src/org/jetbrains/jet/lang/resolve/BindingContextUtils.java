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

package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableAsFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author abreslav
 * @author svtk
 */
public class BindingContextUtils {
    private BindingContextUtils() {
    }

    @Nullable
    public static PsiElement resolveToDeclarationPsiElement(@NotNull BindingContext bindingContext, @Nullable JetReferenceExpression referenceExpression) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
        if (declarationDescriptor == null) {
            return bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression);
        }
        return bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declarationDescriptor);
    }

    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull BindingContext bindingContext, @Nullable JetElement element, boolean onlyReference) {
        DeclarationDescriptor descriptor = null;
        if (!onlyReference && (element instanceof JetProperty || element instanceof JetParameter)) {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        }
        else if (element instanceof JetSimpleNameExpression) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) element);
        }
        else if (element instanceof JetQualifiedExpression) {
            descriptor = extractVariableDescriptorIfAny(bindingContext, ((JetQualifiedExpression) element).getSelectorExpression(), onlyReference);
        }
        if (descriptor instanceof VariableDescriptor) {
            return (VariableDescriptor) descriptor;
        }
        if (descriptor instanceof VariableAsFunctionDescriptor) {
            return ((VariableAsFunctionDescriptor) descriptor).getVariableDescriptor();
        }
        return null;
    }

    // TODO these helper methods are added as a workaround to some compiler bugs in Kotlin...

    @Nullable
    public static NamespaceDescriptor namespaceDescriptor(@NotNull BindingContext context, @NotNull JetFile source) {
        return context.get(BindingContext.NAMESPACE, source);
    }

    @Nullable
    public static PsiElement descriptorToDeclaration(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor) {
        return context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
    }

}
