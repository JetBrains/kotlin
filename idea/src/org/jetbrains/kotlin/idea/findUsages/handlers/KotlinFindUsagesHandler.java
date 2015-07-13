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

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory;
import org.jetbrains.kotlin.idea.findUsages.KotlinReferenceUsageInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class KotlinFindUsagesHandler<T extends PsiElement> extends FindUsagesHandler {
    private final KotlinFindUsagesHandlerFactory factory;
    private final Collection<? extends PsiElement> elementsToSearch;

    public KotlinFindUsagesHandler(
            @NotNull T psiElement,
            @NotNull Collection<? extends PsiElement> elementsToSearch,
            @NotNull KotlinFindUsagesHandlerFactory factory
    ) {
        super(psiElement);
        this.factory = factory;
        this.elementsToSearch = elementsToSearch;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public T getElement() {
        return (T)getPsiElement();
    }

    public KotlinFindUsagesHandler(@NotNull T psiElement, @NotNull KotlinFindUsagesHandlerFactory factory) {
        this(psiElement, Collections.<PsiElement>emptyList(), factory);
    }

    @NotNull
    public final KotlinFindUsagesHandlerFactory getFactory() {
        return factory;
    }

    protected static boolean processUsage(@NotNull Processor<UsageInfo> processor, @NotNull PsiReference ref) {
        return processor.process(new KotlinReferenceUsageInfo(ref));
    }

    protected static boolean processUsage(@NotNull Processor<UsageInfo> processor, @NotNull PsiElement element) {
        return processor.process(new UsageInfo(element));
    }

    @NotNull
    @Override
    public PsiElement[] getPrimaryElements() {
        return elementsToSearch.isEmpty()
               ? new PsiElement[] {getPsiElement()}
               : elementsToSearch.toArray(new PsiElement[elementsToSearch.size()]);
    }

    protected boolean searchTextOccurrences(
            @NotNull final PsiElement element,
            @NotNull final Processor<UsageInfo> processor,
            @NotNull FindUsagesOptions options
    ) {
        final SearchScope scope = options.searchScope;

        boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

        if (searchText) {
            if (options.fastTrack != null) {
                options.fastTrack.searchCustom(new Processor<Processor<PsiReference>>() {
                    @Override
                    public boolean process(Processor<PsiReference> consumer) {
                        return processUsagesInText(element, processor, (GlobalSearchScope)scope);
                    }
                });
            }
            else {
                return processUsagesInText(element, processor, (GlobalSearchScope)scope);
            }
        }
        return true;
    }

    @Override
    public boolean processElementUsages(
            @NotNull PsiElement element,
            @NotNull Processor<UsageInfo> processor,
            @NotNull FindUsagesOptions options
    ) {
        return searchReferences(element, processor, options) && searchTextOccurrences(element, processor, options);
    }

    protected abstract boolean searchReferences(
            @NotNull PsiElement element, @NotNull Processor<UsageInfo> processor, @NotNull FindUsagesOptions options
    );

    @NotNull
    @Override
    public Collection<PsiReference> findReferencesToHighlight(@NotNull PsiElement target, @NotNull SearchScope searchScope
    ) {
        final List<PsiReference> results = new ArrayList<PsiReference>();
        FindUsagesOptions options = getFindUsagesOptions();
        options.searchScope = searchScope;
        searchReferences(target,
                         new Processor<UsageInfo>() {
                             @Override
                             public boolean process(UsageInfo info) {
                                 PsiReference reference = info.getReference();
                                 if (reference != null) {
                                     results.add(reference);
                                 }
                                 return true;
                             }
                         },
                         options);
        return results;
    }
}
