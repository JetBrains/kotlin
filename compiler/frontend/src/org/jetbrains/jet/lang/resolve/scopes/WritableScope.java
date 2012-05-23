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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author abreslav
 */
public interface WritableScope extends JetScope {
    enum LockLevel {
        WRITING,
        BOTH,
        READING,
    }

    WritableScope changeLockLevel(LockLevel lockLevel);

    void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor);

    void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor);

    void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor);

    void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor);

    void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor);

    void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor);

    void addObjectDescriptor(@NotNull ClassDescriptor objectDescriptor);

    void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor);

    void addNamespaceAlias(@NotNull Name name, @NotNull NamespaceDescriptor namespaceDescriptor);
    
    void addFunctionAlias(@NotNull Name name, @NotNull FunctionDescriptor functionDescriptor);
    
    void addVariableAlias(@NotNull Name name, @NotNull VariableDescriptor variableDescriptor);

    void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor);

    @Nullable
    NamespaceDescriptor getDeclaredNamespace(@NotNull Name name);

    @NotNull Multimap<Name, DeclarationDescriptor> getDeclaredDescriptorsAccessibleBySimpleName();

    void importScope(@NotNull JetScope imported);

    void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver);

    void importClassifierAlias(@NotNull Name importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor);

    void importNamespaceAlias(@NotNull Name aliasName, @NotNull NamespaceDescriptor namespaceDescriptor);
    
    void importFunctionAlias(@NotNull Name aliasName, @NotNull FunctionDescriptor functionDescriptor);
    
    void importVariableAlias(@NotNull Name aliasName, @NotNull VariableDescriptor variableDescriptor);

    void clearImports();
}
