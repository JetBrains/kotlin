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
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory;
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinFindMethodUsagesDialog;
import org.jetbrains.jet.plugin.findUsages.options.KotlinMethodFindUsagesOptions;
import org.jetbrains.jet.plugin.search.KotlinExtensionSearch;

import java.util.Collection;

public class KotlinFindFunctionUsagesHandler extends KotlinFindUsagesHandler<JetNamedFunction> {
    public KotlinFindFunctionUsagesHandler(
            @NotNull JetNamedFunction function,
            @NotNull Collection<? extends PsiElement> elementsToSearch,
            @NotNull KotlinFindUsagesHandlerFactory factory
    ) {
        super(function, elementsToSearch, factory);
    }

    public KotlinFindFunctionUsagesHandler(@NotNull JetNamedFunction function, @NotNull KotlinFindUsagesHandlerFactory factory) {
        super(function, factory);
    }

    @NotNull
    @Override
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
        PsiMethod lightMethod = LightClassUtil.getLightClassMethod(getElement());
        if (lightMethod != null) {
            return new KotlinFindMethodUsagesDialog(
                    lightMethod, getProject(), getFactory().getFindMethodOptions(), toShowInNewTab, mustOpenInNewTab, isSingleFile, this
            );
        }

        return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab);
    }

    @Override
    public boolean searchReferences(
            @NotNull PsiElement element,
            @NotNull final Processor<UsageInfo> processor,
            @NotNull FindUsagesOptions options
    ) {
        final KotlinMethodFindUsagesOptions kotlinOptions = (KotlinMethodFindUsagesOptions)options;
        SearchScope searchScope = kotlinOptions.searchScope;

        final PsiMethod lightMethod = ApplicationManager.getApplication().runReadAction(new Computable<PsiMethod>() {
            @Override
            public PsiMethod compute() {
                return LightClassUtil.getLightClassMethod(getElement());
            }
        });
        if (lightMethod == null) return true;

        if (kotlinOptions.isUsages) {
            boolean strictSignatureSearch = !kotlinOptions.isIncludeOverloadUsages;
            if (!MethodReferencesSearch
                    .search(
                            new MethodReferencesSearch.SearchParameters(
                                    lightMethod, searchScope, strictSignatureSearch, kotlinOptions.fastTrack
                            )
                    )
                    .forEach(
                            new ReadActionProcessor<PsiReference>() {
                                @Override
                                public boolean processInReadAction(PsiReference ref) {
                                    return processUsage(processor, ref, kotlinOptions);
                                }
                            }
                    )) {
                return false;
            }
        }

        boolean isAbstract = ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return lightMethod.hasModifierProperty(PsiModifier.ABSTRACT);
            }
        });

        if (isAbstract && kotlinOptions.isImplementingMethods || kotlinOptions.isOverridingMethods) {
            OverridingMethodsSearch.search(lightMethod, options.searchScope, kotlinOptions.isCheckDeepInheritance).forEach(
                    new PsiElementProcessorAdapter<PsiMethod>(
                        new PsiElementProcessor<PsiMethod>() {
                            @Override
                            public boolean execute(@NotNull PsiMethod element) {
                                return processUsage(processor, element.getNavigationElement(), kotlinOptions);
                            }
                        }
                    )
            );
        }

        if (kotlinOptions.isIncludeOverloadUsages) {
            String name = ((PsiNamedElement)element).getName();
            if (name == null) return true;

            for (PsiReference ref : KotlinExtensionSearch.search(lightMethod, searchScope).findAll()) {
                processUsage(processor, ref, kotlinOptions);
            }
        }

        return true;
    }

    @Override
    protected boolean isSearchForTextOccurencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
        if (isSingleFile) return false;
        return psiElement instanceof JetNamedFunction;
    }

    @NotNull
    @Override
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
        return getFactory().getFindMethodOptions();
    }


}
