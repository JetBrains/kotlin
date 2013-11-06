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
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.plugin.findUsages.FindUsagesPackage;
import org.jetbrains.jet.plugin.findUsages.KotlinClassFindUsagesOptions;
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory;
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinFindClassUsagesDialog;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearch;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchRequest;
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchTarget;

public class KotlinFindClassUsagesHandler extends KotlinFindUsagesHandler<JetClass> {
    public KotlinFindClassUsagesHandler(@NotNull JetClass jetClass, @NotNull KotlinFindUsagesHandlerFactory factory) {
        super(jetClass, factory);
    }

    @NotNull
    @Override
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
        PsiClass lightClass = LightClassUtil.getPsiClass(getElement());
        if (lightClass != null) {
            return new KotlinFindClassUsagesDialog(
                    lightClass, getProject(), getFactory().getFindClassOptions(), toShowInNewTab, mustOpenInNewTab, isSingleFile, this
            );
        }
        return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab);
    }

    @Override
    protected boolean searchReferences(
            @NotNull final PsiElement element, @NotNull final Processor<UsageInfo> processor, @NotNull final FindUsagesOptions options
    ) {
        return ApplicationManager.getApplication().runReadAction(
                new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        KotlinClassFindUsagesOptions kotlinOptions = (KotlinClassFindUsagesOptions) options;
                        JetClassOrObject classOrObject = (JetClassOrObject) element;

                        UsagesSearchTarget<JetClassOrObject> target =
                                FindUsagesPackage.toSearchTarget(kotlinOptions, classOrObject, true);
                        UsagesSearchRequest classRequest =
                                FindUsagesPackage.toClassHelper(kotlinOptions).newRequest(target);
                        UsagesSearchRequest declarationsRequest =
                                FindUsagesPackage.toClassDeclarationsHelper(kotlinOptions).newRequest(target);

                        for (PsiReference ref : UsagesSearch.instance$.search(classRequest)) {
                            processUsage(processor, ref);
                        }
                        for (PsiReference ref : UsagesSearch.instance$.search(declarationsRequest)) {
                            processUsage(processor, ref);
                        }

                        PsiClass lightClass = LightClassUtil.getPsiClass(classOrObject);
                        if (lightClass == null) return true;

                        if (!processInheritors(lightClass, processor, kotlinOptions)) return false;

                        return true;
                    }
                }
        );
    }

    private static final ClassInheritorsSearch.InheritanceChecker INHERITANCE_CHECKER = new ClassInheritorsSearch.InheritanceChecker() {
        @Override
        public boolean checkInheritance(@NotNull PsiClass subClass, @NotNull PsiClass parentClass) {
            return true;
        }
    };

    private static boolean processInheritors(
            @NotNull PsiClass klass,
            @NotNull final Processor<UsageInfo> processor,
            @NotNull final KotlinClassFindUsagesOptions options
    ) {
        //noinspection unchecked
        ClassInheritorsSearch.SearchParameters searchParameters = new ClassInheritorsSearch.SearchParameters(
                klass, options.searchScope, options.isCheckDeepInheritance, true, true, Condition.TRUE, INHERITANCE_CHECKER
        );
        return ClassInheritorsSearch.search(searchParameters).forEach(
                new PsiElementProcessorAdapter<PsiClass>(
                        new PsiElementProcessor<PsiClass>() {
                            @Override
                            public boolean execute(@NotNull PsiClass element) {
                                if ((element.isInterface() && options.isDerivedInterfaces)
                                    || (!element.isInterface() && options.isDerivedClasses)) {
                                    return processUsage(processor, element);
                                }
                                return true;
                            }
                        }
                )
        );
    }

    @Override
    protected boolean isSearchForTextOccurencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
        if (isSingleFile) return false;
        return psiElement instanceof JetClass;
    }

    @NotNull
    @Override
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
        return getFactory().getFindClassOptions();
    }
}