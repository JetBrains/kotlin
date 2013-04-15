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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.List;

public class FunctionTypesUtil {
    private static final List<ClassDescriptor> FUNCTIONS;
    private static final List<ClassDescriptor> EXTENSION_FUNCTIONS;

    static {
        int n = KotlinBuiltIns.FUNCTION_TRAIT_COUNT;
        FUNCTIONS = new ArrayList<ClassDescriptor>(n);
        EXTENSION_FUNCTIONS = new ArrayList<ClassDescriptor>(n);

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        for (int i = 0; i < n; i++) {
            Name functionImpl = Name.identifier("FunctionImpl" + i);
            FUNCTIONS.add(createFunctionImplDescriptor(functionImpl, builtIns.getFunction(i)));

            Name extensionFunctionImpl = Name.identifier("ExtensionFunctionImpl" + i);
            EXTENSION_FUNCTIONS.add(createFunctionImplDescriptor(extensionFunctionImpl, builtIns.getExtensionFunction(i)));
        }
    }

    private FunctionTypesUtil() {
    }


    @NotNull
    public static JetType getSuperTypeForClosure(@NotNull FunctionDescriptor funDescriptor, int arity) {
        if (funDescriptor.getReceiverParameter() != null) {
            return EXTENSION_FUNCTIONS.get(arity).getDefaultType();
        }
        else {
            return FUNCTIONS.get(arity).getDefaultType();
        }
    }

    @NotNull
    private static ClassDescriptor createFunctionImplDescriptor(@NotNull Name name, @NotNull ClassDescriptor functionInterface) {
        JetScope builtInsScope = KotlinBuiltIns.getInstance().getBuiltInsScope();

        MutableClassDescriptor functionImpl = new MutableClassDescriptor(
                builtInsScope.getContainingDeclaration(),
                builtInsScope,
                ClassKind.CLASS,
                false,
                name
        );
        functionImpl.setModality(Modality.FINAL);
        functionImpl.setVisibility(Visibilities.PUBLIC);
        functionImpl.setTypeParameterDescriptors(functionInterface.getDefaultType().getConstructor().getParameters());
        functionImpl.createTypeConstructor();

        return functionImpl;
    }

    @NotNull
    public static JvmClassName getFunctionTraitClassName(@NotNull FunctionDescriptor descriptor) {
        int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter() != null) {
            return JvmClassName.byInternalName("jet/ExtensionFunction" + paramCount);
        }
        else {
            return JvmClassName.byInternalName("jet/Function" + paramCount);
        }
    }

    @NotNull
    public static JvmClassName getFunctionImplClassName(@NotNull FunctionDescriptor descriptor) {
        int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter() != null) {
            return JvmClassName.byInternalName("jet/ExtensionFunctionImpl" + paramCount);
        }
        else {
            return JvmClassName.byInternalName("jet/FunctionImpl" + paramCount);
        }
    }
}
