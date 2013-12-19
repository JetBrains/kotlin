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

package org.jetbrains.jet.lang.descriptors;

public interface DeclarationDescriptorVisitor<R, D> {

    R visitPackageFragmentDescriptor(PackageFragmentDescriptor descriptor, D data);

    R visitPackageViewDescriptor(PackageViewDescriptor descriptor, D data);

    R visitVariableDescriptor(VariableDescriptor descriptor, D data);

    R visitFunctionDescriptor(FunctionDescriptor descriptor, D data);

    R visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data);

    R visitClassDescriptor(ClassDescriptor descriptor, D data);

    R visitModuleDeclaration(ModuleDescriptor descriptor, D data);

    R visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, D data);

    R visitScriptDescriptor(ScriptDescriptor scriptDescriptor, D data);

    R visitPropertyDescriptor(PropertyDescriptor descriptor, D data);

    R visitValueParameterDescriptor(ValueParameterDescriptor descriptor, D data);

    R visitPropertyGetterDescriptor(PropertyGetterDescriptor descriptor, D data);

    R visitPropertySetterDescriptor(PropertySetterDescriptor descriptor, D data);

    R visitReceiverParameterDescriptor(ReceiverParameterDescriptor descriptor, D data);
}
