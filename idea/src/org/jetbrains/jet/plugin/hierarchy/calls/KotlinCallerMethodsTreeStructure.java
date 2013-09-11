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
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.FilteringProcessor;
import com.intellij.util.Processor;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.findUsages.FindUsagesUtils;
import org.jetbrains.jet.plugin.hierarchy.HierarchyUtils;
import org.jetbrains.jet.plugin.references.JetPsiReference;

import java.util.Map;

public class KotlinCallerMethodsTreeStructure extends KotlinCallTreeStructure {
    private final String scopeType;
    private final CallerMethodsTreeStructure javaTreeStructure;

    public KotlinCallerMethodsTreeStructure(@NotNull Project project, @NotNull PsiElement element, String scopeType) {
        super(project, new KotlinCallHierarchyNodeDescriptor(project, null, element, true, false));
        this.scopeType = scopeType;

        PsiMethod psiMethod = getPsiMethod(element);
        assert psiMethod != null;

        this.javaTreeStructure = new CallerMethodsTreeStructure(project, psiMethod, scopeType);
    }

    @Override
    protected Object[] buildChildren(HierarchyNodeDescriptor descriptor) {
        PsiElement targetElement = getTargetElement(descriptor);

        PsiClass baseClass = getBasePsiClass();
        if (baseClass == null) return ArrayUtil.EMPTY_OBJECT_ARRAY;

        return processCallers(targetElement, descriptor, baseClass);
    }

    private Object[] processCallers(PsiElement element, HierarchyNodeDescriptor descriptor, PsiClass baseClass) {
        SearchScope searchScope = getSearchScope(scopeType, baseClass);
        Map<PsiElement, KotlinCallHierarchyNodeDescriptor> methodToDescriptorMap =
                new HashMap<PsiElement, KotlinCallHierarchyNodeDescriptor>();

        Object[] javaCallers = null;
        if (element instanceof PsiMethod && javaTreeStructure != null) {
            javaCallers = javaTreeStructure.getChildElements(getJavaNodeDescriptor(descriptor));
            processPsiMethodCallers((PsiMethod) element, descriptor, methodToDescriptorMap, searchScope, true);
        }
        if (element instanceof JetNamedFunction) {
            PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) element);
            processPsiMethodCallers(lightMethod, descriptor, methodToDescriptorMap, searchScope, false);
        }
        if (element instanceof JetProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.getLightClassPropertyMethods((JetProperty) element);
            processPsiMethodCallers(propertyMethods.getGetter(), descriptor, methodToDescriptorMap, searchScope, false);
            processPsiMethodCallers(propertyMethods.getSetter(), descriptor, methodToDescriptorMap, searchScope, false);
        }
        if (element instanceof JetClassOrObject) {
            processJetClassOrObjectCallers((JetClassOrObject) element, descriptor, methodToDescriptorMap, searchScope);
        }

        Object[] callers = methodToDescriptorMap.values().toArray(new Object[methodToDescriptorMap.size()]);
        return (javaCallers != null) ? ArrayUtil.mergeArrays(javaCallers, callers) : callers;
    }

    private void processPsiMethodCallers(
            @Nullable PsiMethod lightMethod,
            HierarchyNodeDescriptor descriptor,
            Map<PsiElement, KotlinCallHierarchyNodeDescriptor> methodToDescriptorMap,
            SearchScope searchScope,
            boolean kotlinOnly
    ) {
        if (lightMethod == null) return;
        MethodReferencesSearch.search(lightMethod, searchScope, true)
                .forEach(defaultQueryProcessor(descriptor, methodToDescriptorMap, kotlinOnly));
    }

    private void processJetClassOrObjectCallers(
            final JetClassOrObject classOrObject,
            HierarchyNodeDescriptor descriptor,
            Map<PsiElement, KotlinCallHierarchyNodeDescriptor> methodToDescriptorMap,
            SearchScope searchScope
    ) {
        Processor<PsiReference> processor = new FilteringProcessor<PsiReference>(
                new Condition<PsiReference>() {
                    @Override
                    public boolean value(PsiReference reference) {
                        return FindUsagesUtils.isConstructorUsage(reference.getElement(), classOrObject);
                    }
                },
                defaultQueryProcessor(descriptor, methodToDescriptorMap, false)
        );
        ReferencesSearch.search(classOrObject, searchScope, false).forEach(processor);
    }

    private Processor<PsiReference> defaultQueryProcessor(
            final HierarchyNodeDescriptor descriptor,
            final Map<PsiElement, KotlinCallHierarchyNodeDescriptor> methodToDescriptorMap,
            final boolean kotlinOnly
    ) {
        return new ReadActionProcessor<PsiReference>() {
            @Override
            public boolean processInReadAction(PsiReference ref) {
                // copied from Java
                if (!(ref instanceof PsiReferenceExpression || ref instanceof JetPsiReference)) {
                    if (!(ref instanceof PsiElement)) {
                        return true;
                    }

                    PsiElement parent = ((PsiElement) ref).getParent();
                    if (parent instanceof PsiNewExpression) {
                        if (((PsiNewExpression) parent).getClassReference() != ref) {
                            return true;
                        }
                    }
                    else if (parent instanceof PsiAnonymousClass) {
                        if (((PsiAnonymousClass) parent).getBaseClassReference() != ref) {
                            return true;
                        }
                    }
                    else {
                        return true;
                    }
                }

                PsiElement element = HierarchyUtils.getCallHierarchyElement(ref.getElement());

                if (kotlinOnly && !(element instanceof JetNamedDeclaration)) return true;

                // If reference belongs to property initializer, show enclosing declaration instead
                if (element instanceof JetProperty) {
                    JetProperty property = (JetProperty) element;
                    if (PsiTreeUtil.isAncestor(property.getInitializer(), ref.getElement(), false)) {
                        element = HierarchyUtils.getCallHierarchyElement(element.getParent());
                    }
                }

                if (element != null) {
                    KotlinCallHierarchyNodeDescriptor d = methodToDescriptorMap.get(element);
                    if (d == null) {
                        d = new KotlinCallHierarchyNodeDescriptor(myProject, descriptor, element, false, true);
                        methodToDescriptorMap.put(element, d);
                    }
                    else if (!d.hasReference(ref)) {
                        d.incrementUsageCount();
                    }
                    d.addReference(ref);
                }

                return true;
            }
        };
    }
}
