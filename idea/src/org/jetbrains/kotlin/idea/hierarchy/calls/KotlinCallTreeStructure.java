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
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashMap;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import java.util.Map;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

public abstract class KotlinCallTreeStructure extends HierarchyTreeStructure {
    protected final String scopeType;

    public KotlinCallTreeStructure(@NotNull Project project, PsiElement element, String scopeType) {
        super(project, createNodeDescriptor(project, element, null, false, false));
        this.scopeType = scopeType;
    }

    protected static KtElement getEnclosingElementForLocalDeclaration(PsiElement element) {
        return element instanceof KtNamedDeclaration
                                   ? KtPsiUtil.getEnclosingElementForLocalDeclaration((KtNamedDeclaration) element)
                                   : null;
    }

    @Nullable
    private static HierarchyNodeDescriptor createNodeDescriptor(
            Project project, PsiElement element, HierarchyNodeDescriptor parent, boolean navigateToReference, boolean wrapAsLightElements
    ) {
        PsiElement nodeElement = element;
        if (wrapAsLightElements && element instanceof KtElement) {
            nodeElement = CollectionsKt.firstOrNull(LightClassUtilsKt.toLightElements((KtElement) element));
        }
        if (nodeElement == null) return null;
        boolean root = (parent == null);
        return nodeElement instanceof KtElement
               ? new KotlinCallHierarchyNodeDescriptor(project, parent, nodeElement, root, navigateToReference)
               : new CallHierarchyNodeDescriptor(project, parent, nodeElement, root, navigateToReference);
    }

    protected static PsiElement getTargetElement(HierarchyNodeDescriptor descriptor) {
        return descriptor instanceof CallHierarchyNodeDescriptor
                                       ? ((CallHierarchyNodeDescriptor) descriptor).getEnclosingElement()
                                       : ((KotlinCallHierarchyNodeDescriptor)descriptor).getTargetElement();
    }

    private static final Function1<PsiElement, Boolean> IS_NON_LOCAL_DECLARATION = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@javax.annotation.Nullable PsiElement input) {
            return input instanceof PsiMethod
                   || ((input instanceof KtNamedFunction || input instanceof KtClassOrObject || input instanceof KtProperty)
                       && !KtPsiUtil.isLocal((KtNamedDeclaration) input));
        }
    };

    @Nullable
    protected static PsiMethod getRepresentativePsiMethod(PsiElement element) {
        while (true) {
            element = PsiUtilsKt.getParentOfTypesAndPredicate(element, false, ArrayUtil.EMPTY_CLASS_ARRAY, IS_NON_LOCAL_DECLARATION);
            if (element == null) return null;

            PsiMethod method = getRepresentativePsiMethodForNonLocalDeclaration(element);
            if (method != null) return method;

            element = element.getParent();
        }
    }

    private static PsiMethod getRepresentativePsiMethodForNonLocalDeclaration(PsiElement element) {
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }

        if (element instanceof KtNamedFunction || element instanceof KtSecondaryConstructor) {
            return LightClassUtil.INSTANCE.getLightClassMethod((KtFunction) element);
        }

        if (element instanceof KtProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.INSTANCE.getLightClassPropertyMethods((KtProperty) element);
            return (propertyMethods.getGetter() != null) ? propertyMethods.getGetter() : propertyMethods.getSetter();
        }

        if (element instanceof KtClassOrObject) {
            PsiClass psiClass = toLightClass((KtClassOrObject) element);
            if (psiClass == null) return null;

            PsiMethod[] constructors = psiClass.getConstructors();
            if (constructors.length > 0) return constructors[0];
        }

        return null;
    }

    protected static CallHierarchyNodeDescriptor getJavaNodeDescriptor(HierarchyNodeDescriptor originalDescriptor) {
        if (originalDescriptor instanceof CallHierarchyNodeDescriptor) return (CallHierarchyNodeDescriptor) originalDescriptor;

        assert originalDescriptor instanceof KotlinCallHierarchyNodeDescriptor;
        return ((KotlinCallHierarchyNodeDescriptor) originalDescriptor).getJavaDelegate();
    }

    protected Object[] collectNodeDescriptors(
            HierarchyNodeDescriptor descriptor, Map<PsiReference, PsiElement> referencesToCalleeElements, PsiClass basePsiClass
    ) {
        HashMap<PsiElement, HierarchyNodeDescriptor> declarationToDescriptorMap = new HashMap<PsiElement, HierarchyNodeDescriptor>();
        for (Map.Entry<PsiReference, PsiElement> refToCallee : referencesToCalleeElements.entrySet()) {
            PsiReference ref = refToCallee.getKey();
            PsiElement callee = refToCallee.getValue();

            if (basePsiClass != null && !isInScope(basePsiClass, callee, scopeType)) continue;

            addNodeDescriptorForElement(ref, callee, declarationToDescriptorMap, descriptor, false);
        }
        return declarationToDescriptorMap.values().toArray(new Object[declarationToDescriptorMap.size()]);
    }

    protected static void addNodeDescriptorForElement(
            PsiReference reference,
            PsiElement element,
            Map<PsiElement, HierarchyNodeDescriptor> declarationToDescriptorMap,
            HierarchyNodeDescriptor descriptor,
            boolean wrapAsLightElements
    ) {
        HierarchyNodeDescriptor d = declarationToDescriptorMap.get(element);
        if (d == null) {
            d = createNodeDescriptor(element.getProject(), element, descriptor, true, wrapAsLightElements);
            if (d == null) return;
            declarationToDescriptorMap.put(element, d);
        }
        else if (d instanceof CallHierarchyNodeDescriptor) {
            ((CallHierarchyNodeDescriptor) d).incrementUsageCount();
        }

        if (d instanceof CallHierarchyNodeDescriptor) {
            ((CallHierarchyNodeDescriptor) d).addReference(reference);
        }
        else if (d instanceof KotlinCallHierarchyNodeDescriptor) {
            ((KotlinCallHierarchyNodeDescriptor) d).addReference(reference);
        }
    }

    @Override
    public boolean isAlwaysShowPlus() {
        return true;
    }
}
