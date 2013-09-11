package org.jetbrains.jet.plugin.hierarchy.calls;

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;

public abstract class KotlinCallTreeStructure extends HierarchyTreeStructure {
    public KotlinCallTreeStructure(@NotNull Project project, HierarchyNodeDescriptor baseDescriptor) {
        super(project, baseDescriptor);
    }

    protected static PsiElement getTargetElement(HierarchyNodeDescriptor descriptor) {
        return descriptor instanceof CallHierarchyNodeDescriptor
                                       ? ((CallHierarchyNodeDescriptor) descriptor).getEnclosingElement()
                                       : ((KotlinCallHierarchyNodeDescriptor)descriptor).getTargetElement();
    }

    @Nullable
    protected PsiMethod getBasePsiMethod() {
        return getPsiMethod(((KotlinCallHierarchyNodeDescriptor) getBaseDescriptor()).getTargetElement());
    }

    @Nullable
    protected PsiClass getBasePsiClass() {
        PsiMethod method = getBasePsiMethod();
        return method != null ? method.getContainingClass() : null;
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
    protected static PsiMethod getPsiMethod(PsiElement element) {
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

    @Nullable
    protected static PsiClass getPsiClass(PsiElement element) {
        PsiMethod method = getPsiMethod(element);
        return method != null ? method.getContainingClass() : null;
    }

    protected static CallHierarchyNodeDescriptor getJavaNodeDescriptor(HierarchyNodeDescriptor originalDescriptor) {
        if (originalDescriptor instanceof CallHierarchyNodeDescriptor) return (CallHierarchyNodeDescriptor) originalDescriptor;

        assert originalDescriptor instanceof KotlinCallHierarchyNodeDescriptor;
        return ((KotlinCallHierarchyNodeDescriptor) originalDescriptor).getJavaDelegate();
    }

    @Override
    public boolean isAlwaysShowPlus() {
        return true;
    }
}
