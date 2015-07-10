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
import org.jetbrains.kotlin.asJava.KotlinLightMethod;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.references.ReferencesPackage;
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

    private static Map<PsiReference, PsiElement> getReferencesToCalleeElements(@NotNull JetElement rootElement) {
        List<JetElement> elementsToAnalyze = new ArrayList<JetElement>();
        if (rootElement instanceof JetNamedFunction) {
            elementsToAnalyze.add(((JetNamedFunction) rootElement).getBodyExpression());
        } else if (rootElement instanceof JetProperty) {
            for (JetPropertyAccessor accessor : ((JetProperty) rootElement).getAccessors()) {
                JetExpression body = accessor.getBodyExpression();
                if (body != null) {
                    elementsToAnalyze.add(body);
                }
            }
        } else {
            JetClassOrObject classOrObject = (JetClassOrObject) rootElement;
            for (JetDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                if (specifier instanceof JetCallElement) {
                    elementsToAnalyze.add(specifier);
                }
            }

            JetClassBody body = classOrObject.getBody();
            if (body != null) {
                for (JetClassInitializer initializer : body.getAnonymousInitializers()) {
                    JetExpression initializerBody = initializer.getBody();
                    if (initializerBody != null) {
                        elementsToAnalyze.add(initializerBody);
                    }
                }
                for (JetProperty property : body.getProperties()) {
                    JetExpression initializer = property.getInitializer();
                    if (initializer != null) {
                        elementsToAnalyze.add(initializer);
                    }
                }
            }
        }

        final Map<PsiReference, PsiElement> referencesToCalleeElements = new HashMap<PsiReference, PsiElement>();
        for (JetElement element : elementsToAnalyze) {
            element.accept(
                    new CalleeReferenceVisitorBase(ResolvePackage.analyze(element, BodyResolveMode.FULL), false) {
                        @Override
                        protected void processDeclaration(JetSimpleNameExpression reference, PsiElement declaration) {
                            referencesToCalleeElements.put(ReferencesPackage.getMainReference(reference), declaration);
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
                if (navigationElement instanceof JetClass) {
                    return buildChildrenByKotlinTarget(descriptor, (JetElement) navigationElement);
                }
            }
        }

        // Kotlin function or property invoked from Java code
        if (targetElement instanceof KotlinLightMethod) {
            return buildChildrenByKotlinTarget(descriptor, ((KotlinLightMethod) targetElement).getOrigin());
        }

        if (targetElement instanceof JetElement) {
            return buildChildrenByKotlinTarget(descriptor, (JetElement) targetElement);
        }

        CallHierarchyNodeDescriptor javaDescriptor = descriptor instanceof CallHierarchyNodeDescriptor
                                                     ? (CallHierarchyNodeDescriptor) descriptor
                                                     : ((KotlinCallHierarchyNodeDescriptor)descriptor).getJavaDelegate();
        return javaTreeStructure.getChildElements(javaDescriptor);
    }

    private Object[] buildChildrenByKotlinTarget(HierarchyNodeDescriptor descriptor, JetElement targetElement) {
        Map<PsiReference, PsiElement> referencesToCalleeElements = getReferencesToCalleeElements(targetElement);
        return collectNodeDescriptors(descriptor, referencesToCalleeElements, representativePsiClass);
    }
}
