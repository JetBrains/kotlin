/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.hierarchy.calls;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.elements.KtLightMethod;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.references.ReferenceUtilKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KotlinCalleeMethodsTreeStructure extends KotlinCallTreeStructure {
    private final CalleeMethodsTreeStructure javaTreeStructure;
    private final PsiClass representativePsiClass;

    public KotlinCalleeMethodsTreeStructure(@NotNull Project project, @NotNull PsiElement element, String scopeType) {
        super(project, element, scopeType);

        PsiMethod representativePsiMethod = getRepresentativePsiMethod(element);
        assert representativePsiMethod != null;

        this.representativePsiClass = representativePsiMethod.getContainingClass();
        this.javaTreeStructure = new CalleeMethodsTreeStructure(project, representativePsiMethod, scopeType);
    }

    private static Map<PsiReference, PsiElement> getReferencesToCalleeElements(@NotNull KtElement rootElement) {
        List<KtElement> elementsToAnalyze = new ArrayList<KtElement>();
        if (rootElement instanceof KtNamedFunction) {
            elementsToAnalyze.add(((KtNamedFunction) rootElement).getBodyExpression());
        } else if (rootElement instanceof KtProperty) {
            for (KtPropertyAccessor accessor : ((KtProperty) rootElement).getAccessors()) {
                KtExpression body = accessor.getBodyExpression();
                if (body != null) {
                    elementsToAnalyze.add(body);
                }
            }
        } else {
            KtClassOrObject classOrObject = (KtClassOrObject) rootElement;
            for (KtSuperTypeListEntry specifier : classOrObject.getSuperTypeListEntries()) {
                if (specifier instanceof KtCallElement) {
                    elementsToAnalyze.add(specifier);
                }
            }

            KtClassBody body = classOrObject.getBody();
            if (body != null) {
                for (KtAnonymousInitializer initializer : body.getAnonymousInitializers()) {
                    KtExpression initializerBody = initializer.getBody();
                    if (initializerBody != null) {
                        elementsToAnalyze.add(initializerBody);
                    }
                }
                for (KtProperty property : body.getProperties()) {
                    KtExpression initializer = property.getInitializer();
                    if (initializer != null) {
                        elementsToAnalyze.add(initializer);
                    }
                }
            }
        }

        final Map<PsiReference, PsiElement> referencesToCalleeElements = new HashMap<PsiReference, PsiElement>();
        for (KtElement element : elementsToAnalyze) {
            element.accept(
                    new CalleeReferenceVisitorBase(ResolutionUtils.analyze(element, BodyResolveMode.FULL), false) {
                        @Override
                        protected void processDeclaration(KtSimpleNameExpression reference, PsiElement declaration) {
                            referencesToCalleeElements.put(ReferenceUtilKt.getMainReference(reference), declaration);
                        }
                    }
            );
        }

        return referencesToCalleeElements;
    }

    @NotNull
    @Override
    protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        PsiElement targetElement = getTargetElement(descriptor);

        // Kotlin class constructor invoked from Java code
        if (targetElement instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) targetElement;
            if (psiMethod.isConstructor()) {
                PsiClass psiClass = psiMethod.getContainingClass();
                PsiElement navigationElement = psiClass != null ? psiClass.getNavigationElement() : null;
                if (navigationElement instanceof KtClass) {
                    return buildChildrenByKotlinTarget(descriptor, (KtElement) navigationElement);
                }
            }
        }

        // Kotlin function or property invoked from Java code
        if (targetElement instanceof KtLightMethod) {
            return buildChildrenByKotlinTarget(descriptor, ((KtLightMethod) targetElement).getKotlinOrigin());
        }

        if (targetElement instanceof KtElement) {
            return buildChildrenByKotlinTarget(descriptor, (KtElement) targetElement);
        }

        CallHierarchyNodeDescriptor javaDescriptor = descriptor instanceof CallHierarchyNodeDescriptor
                                                     ? (CallHierarchyNodeDescriptor) descriptor
                                                     : ((KotlinCallHierarchyNodeDescriptor)descriptor).getJavaDelegate();
        return javaTreeStructure.getChildElements(javaDescriptor);
    }

    private Object[] buildChildrenByKotlinTarget(HierarchyNodeDescriptor descriptor, KtElement targetElement) {
        Map<PsiReference, PsiElement> referencesToCalleeElements = getReferencesToCalleeElements(targetElement);
        return collectNodeDescriptors(descriptor, referencesToCalleeElements, representativePsiClass);
    }
}
