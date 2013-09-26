package org.jetbrains.jet.plugin.hierarchy.calls;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;

import java.util.List;
import java.util.Map;

public abstract class KotlinCallTreeStructure extends HierarchyTreeStructure {
    protected final String scopeType;
    protected final JetElement localizingCodeBlock;
    protected final PsiMethod basePsiMethod;
    protected final PsiClass basePsiClass;

    public KotlinCallTreeStructure(@NotNull Project project, PsiElement element, String scopeType) {
        super(project, createNodeDescriptor(project, element, null));

        this.scopeType = scopeType;

        localizingCodeBlock = element instanceof JetNamedDeclaration
                                   ? JetPsiUtil.getLocalizingCodeBlock((JetNamedDeclaration) element)
                                   : null;

        if (localizingCodeBlock == null) {
            basePsiMethod = getPsiMethod(element);
            assert basePsiMethod != null;

            basePsiClass = basePsiMethod.getContainingClass();
            assert basePsiClass != null;
        }
        else {
            basePsiMethod = null;
            basePsiClass = null;
        }
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

    private static PsiMethod getClosestContainingClassConstructor(PsiElement element) {
        while (element != null) {
            JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(element, JetClassOrObject.class, false);
            if (classOrObject == null) return null;

            element = classOrObject.getParent();

            PsiClass psiClass = LightClassUtil.getPsiClass(classOrObject);
            if (psiClass == null) continue;

            PsiMethod[] constructors = psiClass.getConstructors();
            if (constructors.length > 0) return constructors[0];
        }

        return null;
    }

    @Nullable
    protected final PsiMethod getPsiMethod(PsiElement element) {
        assert localizingCodeBlock == null : "Can't build light method for local declaration";

        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }

        PsiMethod method = null;
        if (element instanceof JetNamedFunction) {
            method = LightClassUtil.getLightClassMethod((JetNamedFunction) element);
        }
        else if (element instanceof JetProperty) {
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    LightClassUtil.getLightClassPropertyMethods((JetProperty) element);

            if (propertyMethods.getGetter() != null) {
                method = propertyMethods.getGetter();
            }
            else {
                method = propertyMethods.getSetter();
            }
        }

        return method != null ? method : getClosestContainingClassConstructor(element);
    }

    protected static CallHierarchyNodeDescriptor getJavaNodeDescriptor(HierarchyNodeDescriptor originalDescriptor) {
        if (originalDescriptor instanceof CallHierarchyNodeDescriptor) return (CallHierarchyNodeDescriptor) originalDescriptor;

        assert originalDescriptor instanceof KotlinCallHierarchyNodeDescriptor;
        return ((KotlinCallHierarchyNodeDescriptor) originalDescriptor).getJavaDelegate();
    }

    protected final Object[] collectNodeDescriptors(HierarchyNodeDescriptor descriptor, List<? extends PsiElement> calleeElements) {
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
