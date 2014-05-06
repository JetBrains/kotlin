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

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.reflect.ReflectionTypes;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeProjectionImpl;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionTypesUtil {
    private final List<ClassDescriptor> functions;
    private final List<ClassDescriptor> extensionFunctions;
    private final List<ClassDescriptor> kFunctions;
    private final List<ClassDescriptor> kMemberFunctions;
    private final List<ClassDescriptor> kExtensionFunctions;

    private final ImmutableMap<ClassDescriptor, ClassDescriptor> kFunctionToImpl;

    public FunctionTypesUtil(@NotNull ReflectionTypes reflectionTypes) {
        int n = KotlinBuiltIns.FUNCTION_TRAIT_COUNT;
        functions = new ArrayList<ClassDescriptor>(n);
        extensionFunctions = new ArrayList<ClassDescriptor>(n);
        kFunctions = new ArrayList<ClassDescriptor>(n);
        kMemberFunctions = new ArrayList<ClassDescriptor>(n);
        kExtensionFunctions = new ArrayList<ClassDescriptor>(n);

        ModuleDescriptor module = new ModuleDescriptorImpl(Name.special("<fake module for functions impl>"),
                                                           Collections.<ImportPath>emptyList(), JavaToKotlinClassMap.getInstance());
        MutablePackageFragmentDescriptor kotlin = new MutablePackageFragmentDescriptor(module, new FqName("kotlin"));
        MutablePackageFragmentDescriptor reflect = new MutablePackageFragmentDescriptor(module, new FqName("kotlin.reflect"));

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        for (int i = 0; i < n; i++) {
            createFunctionImpl(functions, kotlin, "FunctionImpl" + i, builtIns.getFunction(i));
            createFunctionImpl(extensionFunctions, kotlin, "ExtensionFunctionImpl" + i, builtIns.getExtensionFunction(i));
            createFunctionImpl(kFunctions, reflect, "KFunctionImpl" + i, reflectionTypes.getKFunction(i));
            createFunctionImpl(kMemberFunctions, reflect, "KMemberFunctionImpl" + i, reflectionTypes.getKMemberFunction(i));
            createFunctionImpl(kExtensionFunctions, reflect, "KExtensionFunctionImpl" + i, reflectionTypes.getKExtensionFunction(i));
        }

        ImmutableMap.Builder<ClassDescriptor, ClassDescriptor> builder = ImmutableMap.builder();
        for (int i = 0; i < n; i++) {
            builder.put(reflectionTypes.getKFunction(i), kFunctions.get(i));
            builder.put(reflectionTypes.getKMemberFunction(i), kMemberFunctions.get(i));
            builder.put(reflectionTypes.getKExtensionFunction(i), kExtensionFunctions.get(i));
        }
        kFunctionToImpl = builder.build();
    }


    @Nullable
    public ClassDescriptor kFunctionTypeToImpl(@NotNull JetType functionType) {
        //noinspection SuspiciousMethodCalls
        return kFunctionToImpl.get(functionType.getConstructor().getDeclarationDescriptor());
    }

    @NotNull
    public JetType getSuperTypeForClosure(@NotNull FunctionDescriptor descriptor, boolean kFunction) {
        int arity = descriptor.getValueParameters().size();

        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        ReceiverParameterDescriptor expectedThisObject = descriptor.getExpectedThisObject();

        List<TypeProjection> typeArguments = new ArrayList<TypeProjection>(arity + 2);
        if (receiverParameter != null) {
            typeArguments.add(new TypeProjectionImpl(receiverParameter.getType()));
        }
        else if (kFunction && expectedThisObject != null) {
            typeArguments.add(new TypeProjectionImpl(expectedThisObject.getType()));
        }

        for (ValueParameterDescriptor parameter : descriptor.getValueParameters()) {
            typeArguments.add(new TypeProjectionImpl(parameter.getType()));
        }

        //noinspection ConstantConditions
        typeArguments.add(new TypeProjectionImpl(descriptor.getReturnType()));

        ClassDescriptor classDescriptor;
        if (kFunction) {
            if (expectedThisObject != null) {
                classDescriptor = kMemberFunctions.get(arity);
            }
            else if (receiverParameter != null) {
                classDescriptor = kExtensionFunctions.get(arity);
            }
            else {
                classDescriptor = kFunctions.get(arity);
            }
        }
        else {
            if (receiverParameter != null) {
                classDescriptor = extensionFunctions.get(arity);
            }
            else {
                classDescriptor = functions.get(arity);
            }
        }

        return new JetTypeImpl(
                classDescriptor.getDefaultType().getAnnotations(),
                classDescriptor.getTypeConstructor(),
                false,
                typeArguments,
                classDescriptor.getMemberScope(typeArguments)
        );
    }

    private static void createFunctionImpl(
            @NotNull List<ClassDescriptor> result,
            @NotNull PackageFragmentDescriptor containingDeclaration,
            @NotNull String name,
            @NotNull ClassDescriptor functionInterface
    ) {
        MutableClassDescriptor functionImpl = new MutableClassDescriptor(
                containingDeclaration,
                containingDeclaration.getMemberScope(),
                ClassKind.CLASS,
                false,
                Name.identifier(name)
        );
        functionImpl.setModality(Modality.FINAL);
        functionImpl.setVisibility(Visibilities.PUBLIC);
        functionImpl.setTypeParameterDescriptors(functionInterface.getDefaultType().getConstructor().getParameters());
        functionImpl.createTypeConstructor();

        result.add(functionImpl);
    }
}
