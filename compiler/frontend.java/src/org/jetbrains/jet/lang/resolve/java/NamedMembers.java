/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
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
