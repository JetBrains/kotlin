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

package org.jetbrains.kotlin.idea.findUsages.handlers;

import com.intellij.find.findUsages.AbstractFindUsagesDialog;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.idea.findUsages.*;
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindFunctionUsagesDialog;
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindPropertyUsagesDialog;
import org.jetbrains.kotlin.idea.search.declarationsSearch.DeclarationsSearchPackage;
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest;
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearch;
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchHelper;
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchRequest;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public abstract class KotlinFindMemberUsagesHandler<T extends JetNamedDeclaration> extends KotlinFindUsagesHandler<T> {
    private static class Function extends KotlinFindMemberUsagesHandler<JetFunction> {
        public Function(
                @NotNull JetFunction declaration,
                @NotNull Collection<? extends PsiElement> elementsToSearch,
                @NotNull KotlinFindUsagesHandlerFactory factory
        ) {
            super(declaration, elementsToSearch, factory);
        }

        @Override
        protected UsagesSearchHelper<JetFunction> getSearchHelper(KotlinCallableFindUsagesOptions options) {
            return FindUsagesPackage.toHelper((KotlinFunctionFindUsagesOptions) options);
        }

        @NotNull
        @Override
        public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
            return getFactory().getFindFunctionOptions();
        }

        @NotNull
        @Override
        public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
            KotlinFunctionFindUsagesOptions options = getFactory().getFindFunctionOptions();
            Iterator<PsiMethod> lightMethods = getLightMethods(getElement()).iterator();
            if (lightMethods.hasNext()) {
                return new KotlinFindFunctionUsagesDialog(
                        lightMethods.next(), getProject(), options, toShowInNewTab, mustOpenInNewTab, isSingleFile, this
                );
            }

            return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab);
        }
    }

    private static class Property extends KotlinFindMemberUsagesHandler<JetNamedDeclaration> {
        public Property(
                @NotNull JetNamedDeclaration declaration,
                @NotNull Collection<? extends PsiElement> elementsToSearch,
                @NotNull KotlinFindUsagesHandlerFactory factory
        ) {
            super(declaration, elementsToSearch, factory);
        }

        @Override
        protected UsagesSearchHelper<JetNamedDeclaration> getSearchHelper(KotlinCallableFindUsagesOptions options) {
            return FindUsagesPackage.toHelper((KotlinPropertyFindUsagesOptions) options);
        }

        @NotNull
        @Override
        public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
            return getFactory().getFindPropertyOptions();
        }

        @NotNull
        @Override
        public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
            return new KotlinFindPropertyUsagesDialog(
                    getElement(), getProject(), getFactory().getFindPropertyOptions(), toShowInNewTab, mustOpenInNewTab, isSingleFile, this
            );
        }
    }

    protected KotlinFindMemberUsagesHandler(
            @NotNull T declaration,
            @NotNull Collection<? extends PsiElement> elementsToSearch,
            @NotNull KotlinFindUsagesHandlerFactory factory
    ) {
        super(declaration, elementsToSearch, factory);
    }

    protected abstract UsagesSearchHelper<T> getSearchHelper(KotlinCallableFindUsagesOptions options);

    private static Iterable<PsiMethod> getLightMethods(JetNamedDeclaration element) {
        if (element instanceof JetFunction) {
            PsiMethod method = LightClassUtil.getLightClassMethod((JetFunction) element);
            return method != null ? Collections.singletonList(method) : Collections.<PsiMethod>emptyList();
        }

        if (element instanceof JetProperty) {
            return LightClassUtil.getLightClassPropertyMethods((JetProperty) element);
        }

        if (element instanceof JetParameter) {
            return LightClassUtil.getLightClassPropertyMethods((JetParameter) element);
        }

        return null;
    }

    @Override
    protected boolean searchReferences(
            @NotNull final PsiElement element, @NotNull final Processor<UsageInfo> processor, @NotNull final FindUsagesOptions options
    ) {
        return ApplicationManager.getApplication().runReadAction(
                new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        KotlinCallableFindUsagesOptions kotlinOptions = (KotlinCallableFindUsagesOptions) options;

                        @SuppressWarnings("unchecked")
                        UsagesSearchRequest request =
                                getSearchHelper(kotlinOptions).newRequest(FindUsagesPackage.<T>toSearchTarget(options, (T) element, true));

                        final CommonProcessors.UniqueProcessor<UsageInfo> uniqueProcessor =
                                new CommonProcessors.UniqueProcessor<UsageInfo>(processor);

                        for (PsiReference ref : UsagesSearch.INSTANCE$.search(request)) {
                            processUsage(uniqueProcessor, ref);
                        }

                        PsiMethod psiMethod =
                                element instanceof PsiMethod
                                ? (PsiMethod) element
                                : element instanceof JetConstructor
                                  ? LightClassUtil.getLightClassMethod((JetFunction) element)
                                  : null;
                        if (psiMethod != null) {
                            for (PsiReference ref : MethodReferencesSearch.search(psiMethod, options.searchScope, true)) {
                                processUsage(uniqueProcessor, ref);
                            }
                        }

                        if (kotlinOptions.getSearchOverrides()) {
                            DeclarationsSearchPackage.searchOverriders(
                                    new HierarchySearchRequest<PsiElement>(element, options.searchScope, true)
                            ).forEach(
                                    new PsiElementProcessorAdapter<PsiMethod>(
                                            new PsiElementProcessor<PsiMethod>() {
                                                @Override
                                                public boolean execute(@NotNull PsiMethod method) {
                                                    return processUsage(uniqueProcessor, method.getNavigationElement());
                                                }
                                            }
                                    )
                            );
                        }

                        return true;
                    }
                }
        );
    }

    @Override
    protected boolean isSearchForTextOccurencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
        return !isSingleFile;
    }

    @NotNull
    public static KotlinFindMemberUsagesHandler<? extends JetNamedDeclaration> getInstance(
            @NotNull JetNamedDeclaration declaration,
            @NotNull Collection<? extends PsiElement> elementsToSearch,
            @NotNull KotlinFindUsagesHandlerFactory factory) {
        return declaration instanceof JetFunction
               ? new Function((JetFunction) declaration, elementsToSearch, factory)
               : new Property(declaration, elementsToSearch, factory);
    }

    @NotNull
    public static KotlinFindMemberUsagesHandler<? extends JetNamedDeclaration> getInstance(
            @NotNull JetNamedDeclaration declaration,
            @NotNull KotlinFindUsagesHandlerFactory factory) {
        return getInstance(declaration, Collections.<PsiElement>emptyList(), factory);
    }
}
