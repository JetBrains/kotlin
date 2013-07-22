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

package org.jetbrains.jet.codegen.state;

import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;

public abstract class JetTypeToJavaTypeMapperNoMatching implements DeclarationDescriptorVisitor<Type, JetType> {
    private final DeclarationDescriptor descriptor;

    JetTypeToJavaTypeMapperNoMatching(DeclarationDescriptor declarationDescriptor) {
        descriptor = declarationDescriptor;
    }

    private Type reportError() {
        throw new UnsupportedOperationException("no mapping for " + descriptor);
    }

    public Type visitNamespaceDescriptor(NamespaceDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitVariableDescriptor(VariableDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitFunctionDescriptor(FunctionDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitModuleDeclaration(ModuleDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, JetType jetType) {
        return reportError();
    }

    public Type visitScriptDescriptor(ScriptDescriptor scriptDescriptor, JetType jetType) {
        return reportError();
    }

    public Type visitPropertyDescriptor(PropertyDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitValueParameterDescriptor(ValueParameterDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitPropertyGetterDescriptor(PropertyGetterDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitPropertySetterDescriptor(PropertySetterDescriptor descriptor, JetType jetType) {
        return reportError();
    }

    public Type visitReceiverParameterDescriptor(ReceiverParameterDescriptor descriptor, JetType jetType) {
        return reportError();
    }
}
