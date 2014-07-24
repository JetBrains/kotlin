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

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.codeInsight.navigation.actions.GotoSuperAction;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.Collection;
import java.util.List;

public class GotoSuperActionHandler implements CodeInsightActionHandler {
    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(GotoSuperAction.FEATURE_ID);

        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) return;
        @SuppressWarnings("unchecked") JetDeclaration declaration =
                PsiTreeUtil.getParentOfType(element,
                                            JetNamedFunction.class,
                                            JetClass.class,
                                            JetProperty.class,
                                            JetObjectDeclaration.class);
        if (declaration == null) return;

        BindingContext bindingContext = AnalyzerFacadeWithCache.getContextForElement(declaration);

        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        Collection<? extends DeclarationDescriptor> superDescriptors;
        String message;
        if (descriptor instanceof ClassDescriptor) {
            Collection<JetType> supertypes = ((ClassDescriptor) descriptor).getTypeConstructor().getSupertypes();
            List<ClassDescriptor> superclasses = ContainerUtil.mapNotNull(supertypes, new Function<JetType, ClassDescriptor>() {
                @Override
                public ClassDescriptor fun(JetType type) {
                    ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
                    if (descriptor instanceof ClassDescriptor) {
                        return (ClassDescriptor) descriptor;
                    }
                    return null;
                }
            });
            ContainerUtil.removeDuplicates(superclasses);
            superDescriptors = superclasses;
            message = JetBundle.message("goto.super.class.chooser.title");
        }
        else if (descriptor instanceof CallableMemberDescriptor) {
            superDescriptors = OverrideResolver.getDirectlyOverriddenDeclarations((CallableMemberDescriptor) descriptor);
            if (descriptor instanceof PropertyDescriptor) {
                message = JetBundle.message("goto.super.property.chooser.title");
            }
            else if (descriptor instanceof SimpleFunctionDescriptor) {
                message = JetBundle.message("goto.super.function.chooser.title");
            }
            else
                throw new IllegalStateException("Unknown member type: " + descriptor.getClass().getName());
        }
        else
            return;

        List<PsiElement> superDeclarations = ContainerUtil.mapNotNull(superDescriptors, new Function<DeclarationDescriptor, PsiElement>() {
            @Override
            public PsiElement fun(DeclarationDescriptor descriptor) {
                if (KotlinBuiltIns.getInstance().getAny() == descriptor) {
                    return null;
                }
                return DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
            }
        });
        if (superDeclarations.isEmpty()) return;
        if (superDeclarations.size() == 1) {
            Navigatable navigatable = EditSourceUtil.getDescriptor(superDeclarations.get(0));
            if (navigatable != null && navigatable.canNavigate()) {
                navigatable.navigate(true);
            }
        }
        else {
            PsiElement[] superDeclarationsArray = PsiUtilCore.toPsiElementArray(superDeclarations);
            JBPopup popup = descriptor instanceof ClassDescriptor
                            ? NavigationUtil.getPsiElementPopup(superDeclarationsArray, message)
                            : NavigationUtil.getPsiElementPopup(superDeclarationsArray,
                                                                new JetFunctionPsiElementCellRenderer(bindingContext), message);
            popup.showInBestPositionFor(editor);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
