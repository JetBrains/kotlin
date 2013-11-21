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
import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.plugin.findUsages.FindUsagesPackage;
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory;
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinTypeParameterFindUsagesDialog;
import org.jetbrains.jet.plugin.search.usagesSearch.DefaultSearchHelper;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearch;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchRequest;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchTarget;

public class KotlinTypeParameterFindUsagesHandler extends KotlinFindUsagesHandler<JetNamedDeclaration> {
    public KotlinTypeParameterFindUsagesHandler(@NotNull JetNamedDeclaration element, @NotNull KotlinFindUsagesHandlerFactory factory) {
        super(element, factory);
    }

    @NotNull
    @Override
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
        return new KotlinTypeParameterFindUsagesDialog<JetNamedDeclaration>(
                getElement(), getProject(), getFindUsagesOptions(), toShowInNewTab, mustOpenInNewTab, isSingleFile, this
        );
    }

    @Override
    protected boolean searchReferences(
            @NotNull final PsiElement element, @NotNull final Processor<UsageInfo> processor, @NotNull final FindUsagesOptions options
    ) {
        return ApplicationManager.getApplication().runReadAction(
                new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        UsagesSearchTarget<JetNamedDeclaration> target =
                                FindUsagesPackage.toSearchTarget(options, (JetNamedDeclaration) element, true);
                        UsagesSearchRequest request = new DefaultSearchHelper().newRequest(target);

                        for (PsiReference ref : UsagesSearch.instance$.search(request)) {
                            if (!processUsage(processor, ref)) return false;
                        }

                        return true;
                    }
                }
        );
    }

    @NotNull
    @Override
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
        return getFactory().getFindClassOptions();
    }
}