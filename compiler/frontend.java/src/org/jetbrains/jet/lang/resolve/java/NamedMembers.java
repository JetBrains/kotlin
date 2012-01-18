package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
* @author Stepan Koltsov
*/
class NamedMembers {
    String name;
    List<PsiMethodWrapper> methods;

    @Nullable
    PsiFieldWrapper field;
    @Nullable
    List<PropertyAccessorData> propertyAccessors;

    @Nullable
    private PsiClass nestedClasses;
    
    Set<VariableDescriptor> propertyDescriptors;
    Set<FunctionDescriptor> functionDescriptors;

    void addMethod(PsiMethodWrapper method) {
        if (methods == null) {
            methods = new ArrayList<PsiMethodWrapper>();
        }
        methods.add(method);
    }
    
    void addPropertyAccessor(PropertyAccessorData propertyAccessorData) {
        if (propertyAccessors == null) {
            propertyAccessors = new ArrayList<PropertyAccessorData>();
        }
        propertyAccessors.add(propertyAccessorData);
    }
}
