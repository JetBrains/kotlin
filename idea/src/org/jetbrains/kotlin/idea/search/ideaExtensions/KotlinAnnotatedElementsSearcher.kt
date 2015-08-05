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

package org.jetbrains.kotlin.idea.search.ideaExtensions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.impl.search.AnnotatedElementsSearcher;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.stubindex.JetAnnotationsIndex;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.ArrayList;
import java.util.Collection;

public class KotlinAnnotatedElementsSearcher extends AnnotatedElementsSearcher {
    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.search.AnnotatedMembersSearcher");

    @Override
    public boolean execute(@NotNull AnnotatedElementsSearch.Parameters p, @NotNull final Processor<PsiModifierListOwner> consumer) {
        PsiClass annClass = p.getAnnotationClass();
        assert annClass.isAnnotationType() : "Annotation type should be passed to annotated members search";

        final String annotationFQN = annClass.getQualifiedName();
        assert annotationFQN != null;

        SearchScope useScope = p.getScope();

        for (final PsiElement elt : getJetAnnotationCandidates(annClass, useScope)) {
            if (notJetAnnotationEntry(elt)) continue;

            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    JetDeclaration parentOfType = PsiTreeUtil.getParentOfType(elt, JetDeclaration.class);
                    if (parentOfType == null) return;

                    JetAnnotationEntry annotationEntry = (JetAnnotationEntry) elt;

                    BindingContext context = ResolvePackage.analyze(annotationEntry, BodyResolveMode.PARTIAL);
                    AnnotationDescriptor annotationDescriptor = context.get(BindingContext.ANNOTATION, annotationEntry);
                    if (annotationDescriptor == null) return;

                    ClassifierDescriptor descriptor = annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
                    if (descriptor == null) return;
                    if (!(DescriptorUtils.getFqName(descriptor).asString().equals(annotationFQN))) return;

                    if (parentOfType instanceof JetClass) {
                        PsiClass lightClass = LightClassUtil.getPsiClass((JetClass) parentOfType);
                        consumer.process(lightClass);
                    }
                    else if (parentOfType instanceof JetNamedFunction || parentOfType instanceof JetSecondaryConstructor) {
                        PsiMethod wrappedMethod = LightClassUtil.getLightClassMethod((JetFunction) parentOfType);
                        consumer.process(wrappedMethod);
                    }
                }
            });
        }

        return true;
    }

    /* Return all elements annotated with given annotation name. Aliases don't work now. */
    private static Collection<? extends PsiElement> getJetAnnotationCandidates(final PsiClass annClass, final SearchScope useScope) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Collection<? extends PsiElement>>() {
            @Override
            public Collection<? extends PsiElement> compute() {
                if (useScope instanceof GlobalSearchScope) {
                    Collection<JetAnnotationEntry> annotationEntries =
                            JetAnnotationsIndex.getInstance().get(annClass.getName(), annClass.getProject(), (GlobalSearchScope) useScope);

                    // Add annotations 'test' as often used alias when we search Test annotation
                    if (annClass.getName().equals("Test")) {
                        annotationEntries.addAll(JetAnnotationsIndex.getInstance().get(annClass.getName().toLowerCase(), annClass.getProject(), (GlobalSearchScope) useScope));
                    }

                    return annotationEntries;
                }

                // TODO getJetAnnotationCandidates works only with global search scope
                return new ArrayList<PsiElement>();
            }
        });
    }

    private static boolean notJetAnnotationEntry(PsiElement found) {
        if (found instanceof JetAnnotationEntry) return false;

        VirtualFile faultyContainer = PsiUtilCore.getVirtualFile(found);
        LOG.error("Non annotation in annotations list: " + faultyContainer+"; element:"+found);
        if (faultyContainer != null && faultyContainer.isValid()) {
            FileBasedIndex.getInstance().requestReindex(faultyContainer);
        }

        return true;
    }

}
