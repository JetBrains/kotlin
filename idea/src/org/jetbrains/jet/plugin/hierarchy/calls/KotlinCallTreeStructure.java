package org.jetbrains.jet.plugin.hierarchy.calls;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashMap;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;

import java.util.List;
import java.util.Map;

public abstract class KotlinCallTreeStructure extends HierarchyTreeStructure {
    protected final String scopeType;

    public KotlinCallTreeStructure(@NotNull Project project, PsiElement element, String scopeType) {
        super(project, createNodeDescriptor(project, element, null));
        this.scopeType = scopeType;
    }

    protected static JetElement getEnclosingBlockForLocalDeclaration(PsiElement element) {
        return element instanceof JetNamedDeclaration
                                   ? JetPsiUtil.getEnclosingBlockForLocalDeclaration((JetNamedDeclaration) element)
                                   : null;
    }

    protected static HierarchyNodeDescriptor createNodeDescriptor(Project project, PsiElement element, HierarchyNodeDescriptor parent) {
        boolean root = (parent == null);
        return element instanceof JetElement
               ? new KotlinCallHierarchyNodeDescriptor(project, parent, element, root, false)
               : new CallHierarchyNodeDescriptor(project, parent, element, root, false);
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
                   || ((input instanceof JetNamedFunction || input instanceof JetClassOrObject || input instanceof JetProperty)
                       && !JetPsiUtil.isLocal((JetNamedDeclaration) input));
        }
    };

    @Nullable
    protected static PsiMethod getRepresentativePsiMethod(PsiElement element) {
        while (true) {
            element = PsiUtilPackage.getParentByTypesAndPredicate(element, false, ArrayUtil.EMPTY_CLASS_ARRAY, IS_NON_LOCAL_DECLARATION);
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

        if (element instanceof JetNamedFunction) {
            return LightClassUtil.getLightClassMethod((JetNamedFunction) element);
        }

        if (element instanceof JetProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.getLightClassPropertyMethods((JetProperty) element);
            return (propertyMethods.getGetter() != null) ? propertyMethods.getGetter() : propertyMethods.getSetter();
        }

        if (element instanceof JetClassOrObject) {
            PsiClass psiClass = LightClassUtil.getPsiClass((JetClassOrObject) element);
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
            HierarchyNodeDescriptor descriptor, List<? extends PsiElement> calleeElements, PsiClass basePsiClass
    ) {
        HashMap<PsiElement, HierarchyNodeDescriptor> declarationToDescriptorMap = new HashMap<PsiElement, HierarchyNodeDescriptor>();
        for (PsiElement callee : calleeElements) {
            if (basePsiClass != null && !isInScope(basePsiClass, callee, scopeType)) continue;

            addNodeDescriptorForElement(callee, declarationToDescriptorMap, descriptor);
        }
        return declarationToDescriptorMap.values().toArray(new Object[declarationToDescriptorMap.size()]);
    }

    protected final void addNodeDescriptorForElement(
            PsiElement element,
            Map<PsiElement, HierarchyNodeDescriptor> declarationToDescriptorMap,
            HierarchyNodeDescriptor descriptor
    ) {
        HierarchyNodeDescriptor d = declarationToDescriptorMap.get(element);
        if (d == null) {
            d = createNodeDescriptor(myProject, element, descriptor);
            declarationToDescriptorMap.put(element, d);
        }
        else {
            if (d instanceof CallHierarchyNodeDescriptor) {
                ((CallHierarchyNodeDescriptor) d).incrementUsageCount();
            }
            else if (d instanceof KotlinCallHierarchyNodeDescriptor) {
                ((KotlinCallHierarchyNodeDescriptor) d).incrementUsageCount();
            }
        }
    }

    @Override
    public boolean isAlwaysShowPlus() {
        return true;
    }
}
