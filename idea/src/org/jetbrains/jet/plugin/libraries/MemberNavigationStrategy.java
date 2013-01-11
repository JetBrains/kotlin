/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.psi.stubs.StringStubIndexExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex;

import java.util.Collections;
import java.util.List;

interface MemberNavigationStrategy<Decl extends JetNamedDeclaration, Descr extends CallableDescriptor> {
    @NotNull
    Class<Decl> getDeclarationClass();

    @NotNull
    List<JetParameter> getValueParameters(@NotNull Decl declaration);

    @NotNull
    List<ValueParameterDescriptor> getValueParameters(@NotNull Descr descriptor);

    @Nullable
    JetTypeReference getReceiverType(@NotNull Decl declaration);

    @NotNull
    StringStubIndexExtension<Decl> getIndexForTopLevelMembers();

    class FunctionStrategy implements MemberNavigationStrategy<JetNamedFunction, FunctionDescriptor> {
        @NotNull
        @Override
        public Class<JetNamedFunction> getDeclarationClass() {
            return JetNamedFunction.class;
        }

        @NotNull
        @Override
        public List<JetParameter> getValueParameters(@NotNull JetNamedFunction declaration) {
            return declaration.getValueParameters();
        }

        @NotNull
        @Override
        public List<ValueParameterDescriptor> getValueParameters(@NotNull FunctionDescriptor descriptor) {
            return descriptor.getValueParameters();
        }

        @Nullable
        @Override
        public JetTypeReference getReceiverType(@NotNull JetNamedFunction declaration) {
            return declaration.getReceiverTypeRef();
        }

        @NotNull
        @Override
        public StringStubIndexExtension<JetNamedFunction> getIndexForTopLevelMembers() {
            return JetTopLevelFunctionsFqnNameIndex.getInstance();
        }
    }

    class PropertyStrategy implements MemberNavigationStrategy<JetProperty, VariableDescriptor> {
        @NotNull
        @Override
        public Class<JetProperty> getDeclarationClass() {
            return JetProperty.class;
        }

        @NotNull
        @Override
        public List<JetParameter> getValueParameters(@NotNull JetProperty declaration) {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public List<ValueParameterDescriptor> getValueParameters(@NotNull VariableDescriptor descriptor) {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public JetTypeReference getReceiverType(@NotNull JetProperty declaration) {
            return declaration.getReceiverTypeRef();
        }

        @NotNull
        @Override
        public StringStubIndexExtension<JetProperty> getIndexForTopLevelMembers() {
            return JetTopLevelPropertiesFqnNameIndex.getInstance();
        }
    }
}
