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

package org.jetbrains.jet.plugin.search;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DirectClassInheritorsSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.libraries.JetSourceNavigationHelper;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.stubindex.JetSuperClassIndex;

import java.util.Collection;

public class KotlinDirectInheritorsSearcher extends QueryExecutorBase<PsiClass, DirectClassInheritorsSearch.SearchParameters> {
    public KotlinDirectInheritorsSearcher() {
        super(true);
    }

    @Override
    public void processQuery(
            @NotNull final DirectClassInheritorsSearch.SearchParameters queryParameters,
            @NotNull final Processor<PsiClass> consumer
    ) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                PsiClass clazz = queryParameters.getClassToProcess();
                String qualifiedName = clazz.getQualifiedName();
                if (qualifiedName == null) {
                    return;
                }

                String name = clazz.getName();
                if (name == null || !(queryParameters.getScope() instanceof GlobalSearchScope)) return;
                GlobalSearchScope scope = (GlobalSearchScope) queryParameters.getScope();
                Collection<JetClassOrObject> candidates = JetSuperClassIndex.getInstance().get(name, clazz.getProject(), scope);
                for (JetClassOrObject candidate : candidates) {
                    if (!(candidate instanceof JetClass)) continue;
                    JetFile containingFile = (JetFile) candidate.getContainingFile();
                    KotlinCodeAnalyzer sessionForFile = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile(containingFile);
                    ClassDescriptor classDescriptor = (ClassDescriptor) sessionForFile.resolveToDescriptor(candidate);
                    for (JetType type : classDescriptor.getTypeConstructor().getSupertypes()) {
                        ClassifierDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
                        if (declarationDescriptor != null) {
                            String fqName = DescriptorUtils.getFQName(declarationDescriptor).getFqName();
                            if (qualifiedName.equals(fqName)) {
                                PsiClass psiClass = JetSourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(candidate);
                                if (psiClass != null) {
                                    consumer.process(psiClass);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
