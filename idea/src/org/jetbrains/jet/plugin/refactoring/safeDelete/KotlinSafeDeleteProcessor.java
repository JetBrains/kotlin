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

package org.jetbrains.jet.plugin.refactoring.safeDelete;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo;
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetObjectDeclarationName;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class KotlinSafeDeleteProcessor extends JavaSafeDeleteProcessor {
    public static boolean checkElement(PsiElement element) {
        return element instanceof JetClassOrObject
               || element instanceof JetObjectDeclarationName; 
    }

    @Override
    public boolean handlesElement(PsiElement element) {
        return checkElement(element);
    }

    protected static NonCodeUsageSearchInfo getDefaultNonCodeUsageSearchInfo(
            @NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete
    ) {
        return new NonCodeUsageSearchInfo(SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete), element);
    }

    @Nullable
    @Override
    public NonCodeUsageSearchInfo findUsages(PsiElement element, PsiElement[] allElementsToDelete, List<UsageInfo> result) {
        if (element instanceof JetClassOrObject) {
            return findClassOrObjectUsages(element, (JetClassOrObject) element, allElementsToDelete, result);
        }
        if (element instanceof JetObjectDeclarationName && element.getParent() instanceof JetObjectDeclaration) {
            return findClassOrObjectUsages(element, (JetObjectDeclaration) element.getParent(), allElementsToDelete, result);
        }
        return getDefaultNonCodeUsageSearchInfo(element, allElementsToDelete);
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    protected static boolean isInside(PsiElement place, PsiElement[] ancestors) {
        return isInside(place, Arrays.asList(ancestors));
    }

    @SuppressWarnings("MethodOverridesPrivateMethodOfSuperclass")
    protected static boolean isInside(PsiElement place, Collection<? extends PsiElement> ancestors) {
        for (PsiElement element : ancestors) {
            if (isInside(place, element)) return true;
        }
        return false;
    }

    protected static NonCodeUsageSearchInfo findClassOrObjectUsages(
            PsiElement referencedElement,
            final JetClassOrObject classOrObject,
            final PsiElement[] allElementsToDelete,
            final List<UsageInfo> result
    ) {
        ReferencesSearch.search(referencedElement).forEach(new Processor<PsiReference>() {
            @Override
            public boolean process(PsiReference reference) {
                PsiElement element = reference.getElement();

                if (!isInside(element, allElementsToDelete)) {
                    JetImportDirective importDirective = PsiTreeUtil.getParentOfType(element, JetImportDirective.class, false);
                    if (importDirective != null) {
                        result.add(new SafeDeleteImportDirectiveUsageInfo(importDirective, classOrObject));
                        return true;
                    }

                    result.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(element, classOrObject, false));
                }
                return true;
            }
        });

        return getDefaultNonCodeUsageSearchInfo(referencedElement, allElementsToDelete);
    }

    @Nullable
    @Override
    public UsageInfo[] preprocessUsages(Project project, UsageInfo[] usages) {
        return usages;
    }

    @Override
    public Collection<PsiElement> getAdditionalElementsToDelete(
            PsiElement element, Collection<PsiElement> allElementsToDelete, boolean askUser
    ) {
        if (element instanceof JetObjectDeclarationName && element.getParent() instanceof JetObjectDeclaration) {
            return Arrays.asList(element.getParent());
        }
        return super.getAdditionalElementsToDelete(element, allElementsToDelete, askUser);
    }
}
