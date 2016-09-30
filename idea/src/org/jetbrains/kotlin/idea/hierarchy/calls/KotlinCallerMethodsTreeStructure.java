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
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.hierarchy.HierarchyUtilsKt;
import org.jetbrains.kotlin.idea.references.KtReference;
import org.jetbrains.kotlin.idea.references.ReferenceUtilKt;
import org.jetbrains.kotlin.idea.search.usagesSearch.UtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.*;

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

        KtElement codeBlockForLocalDeclaration = getEnclosingElementForLocalDeclaration(element);
        if (codeBlockForLocalDeclaration != null) {
            BindingContext bindingContext = ResolutionUtils.analyze((KtElement) element, BodyResolveMode.FULL);

            final Map<PsiReference, PsiElement> referencesToElements = new HashMap<PsiReference, PsiElement>();
            codeBlockForLocalDeclaration.accept(new CalleeReferenceVisitorBase(bindingContext, true) {
                @Override
                protected void processDeclaration(KtSimpleNameExpression reference, PsiElement declaration) {
                    if (!declaration.equals(element)) return;

                    //noinspection unchecked
                    PsiElement container = PsiTreeUtil.getParentOfType(
                            reference,
                            KtNamedFunction.class, KtPropertyAccessor.class, KtClassOrObject.class
                    );
                    if (container instanceof KtPropertyAccessor) {
                        container = PsiTreeUtil.getParentOfType(container, KtProperty.class);
                    }

                    if (container != null) {
                        referencesToElements.put(ReferenceUtilKt.getMainReference(reference), container);
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
        if (element instanceof KtNamedFunction || element instanceof KtSecondaryConstructor) {
            Collection<PsiMethod> lightMethods = LightClassUtil.INSTANCE.getLightClassMethods((KtFunction) element);
            processPsiMethodCallers(lightMethods, descriptor, methodToDescriptorMap, searchScope, false);
        }
        if (element instanceof KtProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.INSTANCE.getLightClassPropertyMethods((KtProperty) element);
            processPsiMethodCallers(propertyMethods, descriptor, methodToDescriptorMap, searchScope, false);
        }
        if (element instanceof KtClassOrObject) {
            KtPrimaryConstructor constructor = ((KtClassOrObject) element).getPrimaryConstructor();
            if (constructor != null) {
                PsiMethod lightMethod = LightClassUtil.INSTANCE.getLightClassMethod(constructor);
                processPsiMethodCallers(Collections.singleton(lightMethod), descriptor, methodToDescriptorMap, searchScope, false);
            }
            else {
                processKtClassOrObjectCallers((KtClassOrObject) element, descriptor, methodToDescriptorMap, searchScope);
            }
        }

        Object[] callers = methodToDescriptorMap.values().toArray(new Object[methodToDescriptorMap.size()]);
        return (javaCallers != null) ? ArrayUtil.mergeArrays(javaCallers, callers) : callers;
    }

    private static void processPsiMethodCallers(
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
        ContainerUtil.process(references, defaultQueryProcessor(descriptor, methodToDescriptorMap, kotlinOnly, false));
    }

    private static void processKtClassOrObjectCallers(
            final KtClassOrObject classOrObject,
            HierarchyNodeDescriptor descriptor,
            Map<PsiElement, HierarchyNodeDescriptor> methodToDescriptorMap,
            SearchScope searchScope
    ) {
        Processor<PsiReference> processor = new FilteringProcessor<PsiReference>(
                new Condition<PsiReference>() {
                    @Override
                    public boolean value(PsiReference reference) {
                        return UtilsKt.isConstructorUsage(reference, classOrObject);
                    }
                },
                defaultQueryProcessor(descriptor, methodToDescriptorMap, false, false)
        );
        ReferencesSearch.search(classOrObject, searchScope, false).forEach(processor);
    }

    static Processor<PsiReference> defaultQueryProcessor(
            final HierarchyNodeDescriptor descriptor,
            final Map<PsiElement, HierarchyNodeDescriptor> methodToDescriptorMap,
            boolean kotlinOnly,
            final boolean wrapAsLightElements
    ) {
        return new CalleeReferenceProcessor(kotlinOnly) {
            @Override
            protected void onAccept(@NotNull PsiReference ref, @NotNull PsiElement element) {
                addNodeDescriptorForElement(ref, element, methodToDescriptorMap, descriptor, wrapAsLightElements);
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
            if (!(ref instanceof PsiReferenceExpression || ref instanceof KtReference)) {
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
            if (PsiTreeUtil.getParentOfType(refElement, KtImportDirective.class, true) != null) return true;

            PsiElement element = HierarchyUtilsKt.getCallHierarchyElement(refElement);

            if (kotlinOnly && !(element instanceof KtNamedDeclaration)) return true;

            // If reference belongs to property initializer, show enclosing declaration instead
            if (element instanceof KtProperty) {
                KtProperty property = (KtProperty) element;
                if (PsiTreeUtil.isAncestor(property.getInitializer(), refElement, false)) {
                    element = HierarchyUtilsKt.getCallHierarchyElement(element.getParent());
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
