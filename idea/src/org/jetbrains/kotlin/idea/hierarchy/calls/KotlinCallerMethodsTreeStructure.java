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

import com.google.common.collect.Maps;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMemberReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.FilteringProcessor;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.hierarchy.HierarchyUtils;
import org.jetbrains.kotlin.idea.references.JetReference;
import org.jetbrains.kotlin.idea.references.ReferencesPackage;
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchPackage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KotlinCallerMethodsTreeStructure extends KotlinCallTreeStructure {
    private final CallerMethodsTreeStructure javaTreeStructure;
    private final PsiClass basePsiClass;

    public KotlinCallerMethodsTreeStructure(@NotNull Project project, @NotNull PsiElement element, String scopeType) {
        super(project, element, scopeType);

        PsiMethod basePsiMethod = getRepresentativePsiMethod(element);
        assert basePsiMethod != null : "Can't generate light method: " + element.getText();

        basePsiClass = basePsiMethod.getContainingClass();
        javaTreeStructure = new CallerMethodsTreeStructure(project, basePsiMethod, scopeType);
    }

    @NotNull
    @Override
    protected Object[] buildChildren(@NotNull HierarchyNodeDescriptor descriptor) {
        final PsiElement element = getTargetElement(descriptor);

        JetElement codeBlockForLocalDeclaration = getEnclosingElementForLocalDeclaration(element);
        if (codeBlockForLocalDeclaration != null) {
            BindingContext bindingContext = ResolvePackage.analyze((JetElement) element, BodyResolveMode.FULL);

            final Map<PsiReference, PsiElement> referencesToElements = new HashMap<PsiReference, PsiElement>();
            codeBlockForLocalDeclaration.accept(new CalleeReferenceVisitorBase(bindingContext, true) {
                @Override
                protected void processDeclaration(JetSimpleNameExpression reference, PsiElement declaration) {
                    if (!declaration.equals(element)) return;

                    //noinspection unchecked
                    PsiElement container = PsiTreeUtil.getParentOfType(
                            reference,
                            JetNamedFunction.class, JetPropertyAccessor.class, JetClassOrObject.class
                    );
                    if (container instanceof JetPropertyAccessor) {
                        container = PsiTreeUtil.getParentOfType(container, JetProperty.class);
                    }

                    if (container != null) {
                        referencesToElements.put(ReferencesPackage.getMainReference(reference), container);
                    }
                }
            });
            return collectNodeDescriptors(descriptor, referencesToElements, null);
        }

        SearchScope searchScope = getSearchScope(scopeType, basePsiClass);
        Map<PsiElement, HierarchyNodeDescriptor> methodToDescriptorMap = Maps.newHashMap();

        Object[] javaCallers = null;
        if (element instanceof PsiMethod) {
            javaCallers = javaTreeStructure.getChildElements(getJavaNodeDescriptor(descriptor));
            processPsiMethodCallers(
                    Collections.singleton((PsiMethod) element), descriptor, methodToDescriptorMap, searchScope, true
            );
        }
        if (element instanceof JetNamedFunction || element instanceof JetSecondaryConstructor) {
            PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetFunction) element);
            processPsiMethodCallers(Collections.singleton(lightMethod), descriptor, methodToDescriptorMap, searchScope, false);
        }
        if (element instanceof JetProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.getLightClassPropertyMethods((JetProperty) element);
            processPsiMethodCallers(propertyMethods, descriptor, methodToDescriptorMap, searchScope, false);
        }
        if (element instanceof JetClassOrObject) {
            JetPrimaryConstructor constructor = ((JetClassOrObject) element).getPrimaryConstructor();
            if (constructor != null) {
                PsiMethod lightMethod = LightClassUtil.getLightClassMethod(constructor);
                processPsiMethodCallers(Collections.singleton(lightMethod), descriptor, methodToDescriptorMap, searchScope, false);
            }
            else {
                processJetClassOrObjectCallers((JetClassOrObject) element, descriptor, methodToDescriptorMap, searchScope);
            }
        }

        Object[] callers = methodToDescriptorMap.values().toArray(new Object[methodToDescriptorMap.size()]);
        return (javaCallers != null) ? ArrayUtil.mergeArrays(javaCallers, callers) : callers;
    }

    private void processPsiMethodCallers(
            Iterable<PsiMethod> lightMethods,
            HierarchyNodeDescriptor descriptor,
            Map<PsiElement, HierarchyNodeDescriptor> methodToDescriptorMap,
            SearchScope searchScope,
            boolean kotlinOnly
    ) {
        Set<PsiMethod> methodsToFind = new HashSet<PsiMethod>();
        for (PsiMethod lightMethod : lightMethods) {
            if (lightMethod == null) continue;

            PsiMethod[] superMethods = lightMethod.findDeepestSuperMethods();
            methodsToFind.add(lightMethod);
            ContainerUtil.addAll(methodsToFind, superMethods);
        }

        if (methodsToFind.isEmpty()) return;

        Set<PsiReference> references = ContainerUtil.newTroveSet(
                new TObjectHashingStrategy<PsiReference>() {
                    @Override
                    public int computeHashCode(PsiReference object) {
                        return object.getElement().hashCode();
                    }

                    @Override
                    public boolean equals(PsiReference o1, PsiReference o2) {
                        return o1.getElement().equals(o2.getElement());
                    }
                }
        );
        for (PsiMethod superMethod: methodsToFind) {
            ContainerUtil.addAll(references, MethodReferencesSearch.search(superMethod, searchScope, true));
        }
        ContainerUtil.process(references, defaultQueryProcessor(descriptor, methodToDescriptorMap, kotlinOnly));
    }

    private void processJetClassOrObjectCallers(
            final JetClassOrObject classOrObject,
            HierarchyNodeDescriptor descriptor,
            Map<PsiElement, HierarchyNodeDescriptor> methodToDescriptorMap,
            SearchScope searchScope
    ) {
        Processor<PsiReference> processor = new FilteringProcessor<PsiReference>(
                new Condition<PsiReference>() {
                    @Override
                    public boolean value(PsiReference reference) {
                        return UsagesSearchPackage.isConstructorUsage(reference, classOrObject);
                    }
                },
                defaultQueryProcessor(descriptor, methodToDescriptorMap, false)
        );
        ReferencesSearch.search(classOrObject, searchScope, false).forEach(processor);
    }

    private Processor<PsiReference> defaultQueryProcessor(
            final HierarchyNodeDescriptor descriptor,
            final Map<PsiElement, HierarchyNodeDescriptor> methodToDescriptorMap,
            boolean kotlinOnly
    ) {
        return new CalleeReferenceProcessor(kotlinOnly) {
            @Override
            protected void onAccept(@NotNull PsiReference ref, @NotNull PsiElement element) {
                addNodeDescriptorForElement(ref, element, methodToDescriptorMap, descriptor);
            }
        };
    }

    public static abstract class CalleeReferenceProcessor extends ReadActionProcessor<PsiReference> {
        private final boolean kotlinOnly;

        public CalleeReferenceProcessor(boolean only) {
            kotlinOnly = only;
        }

        @Override
        public boolean processInReadAction(PsiReference ref) {
            // copied from Java
            if (!(ref instanceof PsiReferenceExpression || ref instanceof JetReference)) {
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
                else if (ref instanceof LightMemberReference) {
                    PsiElement refTarget = ref.resolve();
                    // Accept implicit superclass constructor reference in Java code
                    if (!(refTarget instanceof PsiMethod && ((PsiMethod) refTarget).isConstructor())) return true;
                }
                else {
                    return true;
                }
            }

            PsiElement refElement = ref.getElement();
            if (PsiTreeUtil.getParentOfType(refElement, JetImportDirective.class, true) != null) return true;

            PsiElement element = HierarchyUtils.getCallHierarchyElement(refElement);

            if (kotlinOnly && !(element instanceof JetNamedDeclaration)) return true;

            // If reference belongs to property initializer, show enclosing declaration instead
            if (element instanceof JetProperty) {
                JetProperty property = (JetProperty) element;
                if (PsiTreeUtil.isAncestor(property.getInitializer(), refElement, false)) {
                    element = HierarchyUtils.getCallHierarchyElement(element.getParent());
                }
            }

            if (element != null) {
                onAccept(ref, element);
            }

            return true;
        }

        protected abstract void onAccept(@NotNull PsiReference ref, @NotNull PsiElement element);
    }
}
