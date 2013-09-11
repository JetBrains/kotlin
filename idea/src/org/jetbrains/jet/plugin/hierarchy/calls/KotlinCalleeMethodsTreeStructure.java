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
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetClsMethod;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.ArrayList;
import java.util.List;

public class KotlinCalleeMethodsTreeStructure extends KotlinCallTreeStructure {
    private final String scopeType;
    private final CalleeMethodsTreeStructure javaTreeStucture;

    public KotlinCalleeMethodsTreeStructure(@NotNull Project project, @NotNull PsiElement element, String scopeType) {
        super(project, new KotlinCallHierarchyNodeDescriptor(project, null, element, true, false));
        this.scopeType = scopeType;

        PsiMethod psiMethod = getPsiMethod(element);
        assert psiMethod != null;

        this.javaTreeStucture = new CalleeMethodsTreeStructure(project, psiMethod, scopeType);
    }

    private static class VisitorBase extends JetTreeVisitorVoid {
        final BindingContext bindingContext;
        final ArrayList<PsiElement> result;

        private VisitorBase(BindingContext bindingContext, ArrayList<PsiElement> result) {
            this.bindingContext = bindingContext;
            this.result = result;
        }

        @Override
        public void visitJetElement(JetElement element) {
            if (!(element instanceof JetClassOrObject || element instanceof JetNamedFunction)) {
                super.visitJetElement(element);
            }
        }

        @Override
        public void visitCallExpression(JetCallExpression expression) {
            super.visitCallExpression(expression);
            processCallElement(expression);
        }

        @Override
        public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
            if (descriptor instanceof PropertyDescriptor) {
                PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
                if (declaration instanceof JetProperty) {
                    result.add(declaration);
                }
            }
        }

        @Override
        public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
            if (specifier instanceof JetCallElement) {
                processCallElement((JetCallElement) specifier);
            }
        }

        private void processCallElement(JetCallElement element) {
            JetExpression callee = element.getCalleeExpression();

            JetReferenceExpression referenceExpression = null;
            if (callee instanceof JetReferenceExpression) {
                referenceExpression = (JetReferenceExpression) callee;
            }
            else if (callee instanceof JetConstructorCalleeExpression) {
                referenceExpression = ((JetConstructorCalleeExpression) callee).getConstructorReferenceExpression();
            }

            if (referenceExpression != null) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
                if (descriptor == null) return;

                PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
                if (declaration instanceof JetClassOrObject
                    || (declaration instanceof JetNamedFunction && !((JetNamedFunction) declaration).isLocal())
                    || declaration instanceof PsiMethod ) {
                    result.add(declaration);
                }
            }
        }
    }

    private static List<? extends PsiElement> getCalleeElements(@NotNull JetElement rootElement, BindingContext bindingContext) {
        ArrayList<PsiElement> result = new ArrayList<PsiElement>();
        JetVisitorVoid visitor = new VisitorBase(bindingContext, result);

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

    // copied from Java
    private Object[] collectNodeDescriptors(
            HierarchyNodeDescriptor descriptor,
            List<? extends PsiElement> calleeElements,
            PsiElement baseElement
    ) {
        HashMap<PsiElement, KotlinCallHierarchyNodeDescriptor> declarationToDescriptorMap =
                new HashMap<PsiElement, KotlinCallHierarchyNodeDescriptor>();

        ArrayList<KotlinCallHierarchyNodeDescriptor> result = new ArrayList<KotlinCallHierarchyNodeDescriptor>();

        for (PsiElement callee : calleeElements) {
            if (!isInScope(baseElement, callee, scopeType)) continue;

            KotlinCallHierarchyNodeDescriptor d = declarationToDescriptorMap.get(callee);
            if (d == null) {
                d = new KotlinCallHierarchyNodeDescriptor(myProject, descriptor, callee, false, false);
                declarationToDescriptorMap.put(callee, d);
                result.add(d);
            }
            else {
                d.incrementUsageCount();
            }
        }
        return ArrayUtil.toObjectArray(result);
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

        if (javaTreeStucture != null) {
            CallHierarchyNodeDescriptor javaDescriptor = descriptor instanceof CallHierarchyNodeDescriptor
                                                         ? (CallHierarchyNodeDescriptor) descriptor
                                                         : ((KotlinCallHierarchyNodeDescriptor)descriptor).getJavaDelegate();
            return javaTreeStucture.getChildElements(javaDescriptor);
        }

        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    private Object[] buildChildrenByKotlinTarget(HierarchyNodeDescriptor descriptor, JetElement targetElement) {
        PsiClass baseClass = getBasePsiClass();
        if (baseClass == null) return ArrayUtil.EMPTY_OBJECT_ARRAY;

        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) targetElement.getContainingFile()).getBindingContext();
        List<? extends PsiElement> calleeDescriptors = getCalleeElements((JetElement) targetElement, bindingContext);

        return collectNodeDescriptors(descriptor, calleeDescriptors, baseClass);
    }
}
