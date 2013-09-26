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

package org.jetbrains.jet.plugin.hierarchy.calls;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetClsMethod;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.ArrayList;
import java.util.List;

public class KotlinCalleeMethodsTreeStructure extends KotlinCallTreeStructure {
    private final CalleeMethodsTreeStructure javaTreeStructure;

    public KotlinCalleeMethodsTreeStructure(@NotNull Project project, @NotNull PsiElement element, String scopeType) {
        super(project, element, scopeType);
        this.javaTreeStructure = basePsiMethod != null ? new CalleeMethodsTreeStructure(project, basePsiMethod, scopeType) : null;
    }

    private static List<? extends PsiElement> getCalleeElements(@NotNull JetElement rootElement, BindingContext bindingContext) {
        final ArrayList<PsiElement> result = new ArrayList<PsiElement>();
        JetVisitorVoid visitor = new CalleeReferenceVisitorBase(bindingContext, false) {
            @Override
            protected void processDeclaration(JetReferenceExpression reference, PsiElement declaration) {
                result.add(declaration);
            }
        };

        if (rootElement instanceof JetNamedFunction) {
            JetExpression body = ((JetNamedFunction) rootElement).getBodyExpression();
            if (body != null) {
                body.accept(visitor);
            }
        } else if (rootElement instanceof JetProperty) {
            for (JetPropertyAccessor accessor : ((JetProperty) rootElement).getAccessors()) {
                JetExpression body = accessor.getBodyExpression();
                if (body != null) {
                    body.accept(visitor);
                }
            }
        } else {
            JetClassOrObject classOrObject = (JetClassOrObject) rootElement;
            for (JetDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                if (specifier instanceof JetCallElement) {
                    specifier.accept(visitor);
                }
            }

            JetClassBody body = classOrObject.getBody();
            if (body != null) {
                for (JetClassInitializer initializer : body.getAnonymousInitializers()) {
                    initializer.getBody().accept(visitor);
                }
                for (JetProperty property : body.getProperties()) {
                    JetExpression initializer = property.getInitializer();
                    if (initializer != null) {
                        initializer.accept(visitor);
                    }
                }
            }
        }

        return result;
    }

    @Override
    protected Object[] buildChildren(HierarchyNodeDescriptor descriptor) {
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
        if (targetElement instanceof JetClsMethod) {
            return buildChildrenByKotlinTarget(descriptor, ((JetClsMethod) targetElement).getOrigin());
        }

        if (targetElement instanceof JetElement) {
            return buildChildrenByKotlinTarget(descriptor, (JetElement) targetElement);
        }

        if (javaTreeStructure != null) {
            CallHierarchyNodeDescriptor javaDescriptor = descriptor instanceof CallHierarchyNodeDescriptor
                                                         ? (CallHierarchyNodeDescriptor) descriptor
                                                         : ((KotlinCallHierarchyNodeDescriptor)descriptor).getJavaDelegate();
            return javaTreeStructure.getChildElements(javaDescriptor);
        }

        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    private Object[] buildChildrenByKotlinTarget(HierarchyNodeDescriptor descriptor, JetElement targetElement) {
        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) targetElement.getContainingFile()).getBindingContext();
        List<? extends PsiElement> calleeDescriptors = getCalleeElements((JetElement) targetElement, bindingContext);
        return collectNodeDescriptors(descriptor, calleeDescriptors);
    }
}
