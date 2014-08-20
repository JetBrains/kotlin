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

package org.jetbrains.jet.plugin.findUsages.handlers;

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
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.plugin.findUsages.*;
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinFindFunctionUsagesDialog;
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinFindPropertyUsagesDialog;
import org.jetbrains.jet.plugin.search.declarationsSearch.DeclarationsSearchPackage;
import org.jetbrains.jet.plugin.search.declarationsSearch.HierarchySearchRequest;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearch;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchHelper;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public abstract class KotlinFindMemberUsagesHandler<T extends JetNamedDeclaration> extends KotlinFindUsagesHandler<T> {
    private static class Function extends KotlinFindMemberUsagesHandler<JetNamedFunction> {
        public Function(
                @NotNull JetNamedFunction declaration,
                @NotNull Collection<? extends PsiElement> elementsToSearch,
                @NotNull KotlinFindUsagesHandlerFactory factory
        ) {
            super(declaration, elementsToSearch, factory);
        }

        @Override
        protected UsagesSearchHelper<JetNamedFunction> getSearchHelper(KotlinCallableFindUsagesOptions options) {
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
        if (element instanceof JetNamedFunction) {
            PsiMethod method = LightClassUtil.getLightClassMethod((JetNamedFunction) element);
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
                                getSearchHelper(kotlinOptions).newRequest(FindUsagesPackage.toSearchTarget(options, (T) element, true));

                        for (PsiReference ref : UsagesSearch.INSTANCE$.search(request)) {
                            processUsage(processor, ref);
                        }

                        if (kotlinOptions.getSearchOverrides()) {
                            DeclarationsSearchPackage.searchOverriders(
                                    new HierarchySearchRequest<PsiElement>(element, options.searchScope, true)
                            ).forEach(
                                    new PsiElementProcessorAdapter<PsiMethod>(
                                            new PsiElementProcessor<PsiMethod>() {
                                                @Override
                                                public boolean execute(@NotNull PsiMethod method) {
                                                    return processUsage(processor, method.getNavigationElement());
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
        return declaration instanceof JetNamedFunction
               ? new Function((JetNamedFunction) declaration, elementsToSearch, factory)
               : new Property(declaration, elementsToSearch, factory);
    }

    @NotNull
    public static KotlinFindMemberUsagesHandler<? extends JetNamedDeclaration> getInstance(
            @NotNull JetNamedDeclaration declaration,
            @NotNull KotlinFindUsagesHandlerFactory factory) {
        return getInstance(declaration, Collections.<PsiElement>emptyList(), factory);
    }
}
