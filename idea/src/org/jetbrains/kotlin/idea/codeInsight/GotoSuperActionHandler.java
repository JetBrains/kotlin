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

package org.jetbrains.kotlin.idea.codeInsight;

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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.DescriptorUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny;

public class GotoSuperActionHandler implements CodeInsightActionHandler {
    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(GotoSuperAction.FEATURE_ID);

        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        if (element == null) return;
        @SuppressWarnings("unchecked") KtDeclaration declaration =
                PsiTreeUtil.getParentOfType(element,
                                            KtNamedFunction.class,
                                            KtClass.class,
                                            KtProperty.class,
                                            KtObjectDeclaration.class);
        if (declaration == null) return;

        DeclarationDescriptor descriptor = ResolutionUtils.unsafeResolveToDescriptor(declaration, BodyResolveMode.PARTIAL);

        List<PsiElement> superDeclarations = findSuperDeclarations(descriptor);

        if (superDeclarations == null || superDeclarations.isEmpty()) return;
        if (superDeclarations.size() == 1) {
            Navigatable navigatable = EditSourceUtil.getDescriptor(superDeclarations.get(0));
            if (navigatable != null && navigatable.canNavigate()) {
                navigatable.navigate(true);
            }
        }
        else {
            String message = getTitle(descriptor);
            PsiElement[] superDeclarationsArray = PsiUtilCore.toPsiElementArray(superDeclarations);
            JBPopup popup = descriptor instanceof ClassDescriptor
                            ? NavigationUtil.getPsiElementPopup(superDeclarationsArray, message)
                            : NavigationUtil.getPsiElementPopup(superDeclarationsArray,
                                                                new KtFunctionPsiElementCellRenderer(), message);
            popup.showInBestPositionFor(editor);
        }
    }

    @Nullable
    private static String getTitle(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return KotlinBundle.message("goto.super.class.chooser.title");
        }

        if (descriptor instanceof PropertyDescriptor) {
            return KotlinBundle.message("goto.super.property.chooser.title");
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            return KotlinBundle.message("goto.super.function.chooser.title");
        }

        return null;
    }

    @Nullable
    private static List<PsiElement> findSuperDeclarations(DeclarationDescriptor descriptor) {
        Collection<? extends DeclarationDescriptor> superDescriptors;
        if (descriptor instanceof ClassDescriptor) {
            Collection<KotlinType> supertypes = ((ClassDescriptor) descriptor).getTypeConstructor().getSupertypes();
            List<ClassDescriptor> superclasses = ContainerUtil.mapNotNull(supertypes, new Function<KotlinType, ClassDescriptor>() {
                @Override
                public ClassDescriptor fun(KotlinType type) {
                    ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
                    if (descriptor instanceof ClassDescriptor) {
                        return (ClassDescriptor) descriptor;
                    }
                    return null;
                }
            });
            ContainerUtil.removeDuplicates(superclasses);
            superDescriptors = superclasses;
        }
        else if (descriptor instanceof CallableMemberDescriptor) {
            superDescriptors = DescriptorUtilsKt.getDirectlyOverriddenDeclarations((CallableMemberDescriptor) descriptor);
        }
        else {
            return null;
        }

        return ContainerUtil.mapNotNull(superDescriptors, new Function<DeclarationDescriptor, PsiElement>() {
            @Override
            public PsiElement fun(DeclarationDescriptor descriptor) {
                if (descriptor instanceof ClassDescriptor && isAny((ClassDescriptor) descriptor)) {
                    return null;
                }
                return DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
            }
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
