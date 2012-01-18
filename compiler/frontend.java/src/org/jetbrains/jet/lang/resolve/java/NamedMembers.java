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
    MembersForProperty properties;
    @Nullable
    private PsiClass nestedClasses;
    
    Set<VariableDescriptor> propertyDescriptors;
    Set<FunctionDescriptor> functionDescriptors;

    MembersForProperty getForProperty() {
        if (properties == null) {
            properties = new MembersForProperty();
        }
        return properties;
    }
    
    void addMethod(PsiMethodWrapper method) {
        if (methods == null) {
            methods = new ArrayList<PsiMethodWrapper>();
        }
        methods.add(method);
    }
}
