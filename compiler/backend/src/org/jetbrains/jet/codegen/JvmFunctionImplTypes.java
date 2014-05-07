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

import com.google.common.collect.ImmutableList;
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
import java.util.Map;

public class JvmFunctionImplTypes {
    private final ReflectionTypes reflectionTypes;
    private final ModuleDescriptor fakeModule;

    private volatile List<Functions> functionsList;
    private volatile List<KFunctions> kFunctionsList;
    private volatile Map<ClassDescriptor, ClassDescriptor> kFunctionToImplMap;

    private static class Functions {
        public final ClassDescriptor functionImpl;
        public final ClassDescriptor extensionFunctionImpl;

        public Functions(
                @NotNull ClassDescriptor functionImpl,
                @NotNull ClassDescriptor extensionFunctionImpl
        ) {
            this.functionImpl = functionImpl;
            this.extensionFunctionImpl = extensionFunctionImpl;
        }
    }

    private static class KFunctions {
        public final ClassDescriptor kFunctionImpl;
        public final ClassDescriptor kMemberFunctionImpl;
        public final ClassDescriptor kExtensionFunctionImpl;

        public KFunctions(
                @NotNull ClassDescriptor kFunctionImpl,
                @NotNull ClassDescriptor kMemberFunctionImpl,
                @NotNull ClassDescriptor kExtensionFunctionImpl
        ) {
            this.kFunctionImpl = kFunctionImpl;
            this.kMemberFunctionImpl = kMemberFunctionImpl;
            this.kExtensionFunctionImpl = kExtensionFunctionImpl;
        }
    }

    public JvmFunctionImplTypes(@NotNull ReflectionTypes reflectionTypes) {
        this.reflectionTypes = reflectionTypes;
        this.fakeModule = new ModuleDescriptorImpl(Name.special("<fake module for functions impl>"), Collections.<ImportPath>emptyList(),
                                                   JavaToKotlinClassMap.getInstance());
    }

    @NotNull
    private List<Functions> getFunctionsImplList() {
        if (functionsList == null) {
            MutablePackageFragmentDescriptor kotlin = new MutablePackageFragmentDescriptor(fakeModule, new FqName("kotlin"));
            KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

            ImmutableList.Builder<Functions> builder = ImmutableList.builder();
            for (int i = 0; i < KotlinBuiltIns.FUNCTION_TRAIT_COUNT; i++) {
                builder.add(new Functions(
                        createFunctionImpl(kotlin, "FunctionImpl" + i, builtIns.getFunction(i)),
                        createFunctionImpl(kotlin, "ExtensionFunctionImpl" + i, builtIns.getExtensionFunction(i))
                ));
            }
            functionsList = builder.build();
        }
        return functionsList;
    }

    @NotNull
    private List<KFunctions> getKFunctionsImplList() {
        if (kFunctionsList == null) {
            MutablePackageFragmentDescriptor reflect = new MutablePackageFragmentDescriptor(fakeModule, new FqName("kotlin.reflect"));

            ImmutableList.Builder<KFunctions> builder = ImmutableList.builder();
            for (int i = 0; i < KotlinBuiltIns.FUNCTION_TRAIT_COUNT; i++) {
                builder.add(new KFunctions(
                        createFunctionImpl(reflect, "KFunctionImpl" + i, reflectionTypes.getKFunction(i)),
                        createFunctionImpl(reflect, "KMemberFunctionImpl" + i, reflectionTypes.getKMemberFunction(i)),
                        createFunctionImpl(reflect, "KExtensionFunctionImpl" + i, reflectionTypes.getKExtensionFunction(i))
                ));
            }
            kFunctionsList = builder.build();
        }
        return kFunctionsList;
    }

    @NotNull
    private Map<ClassDescriptor, ClassDescriptor> getKFunctionToImplMap() {
        if (kFunctionToImplMap == null) {
            ImmutableMap.Builder<ClassDescriptor, ClassDescriptor> builder = ImmutableMap.builder();
            for (int i = 0; i < KotlinBuiltIns.FUNCTION_TRAIT_COUNT; i++) {
                KFunctions kFunctions = getKFunctionsImplList().get(i);
                builder.put(reflectionTypes.getKFunction(i), kFunctions.kFunctionImpl);
                builder.put(reflectionTypes.getKMemberFunction(i), kFunctions.kMemberFunctionImpl);
                builder.put(reflectionTypes.getKExtensionFunction(i), kFunctions.kExtensionFunctionImpl);
            }
            kFunctionToImplMap = builder.build();
        }
        return kFunctionToImplMap;
    }


    @Nullable
    public ClassDescriptor kFunctionTypeToImpl(@NotNull JetType functionType) {
        //noinspection SuspiciousMethodCalls
        return getKFunctionToImplMap().get(functionType.getConstructor().getDeclarationDescriptor());
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
            KFunctions kFunctions = getKFunctionsImplList().get(arity);
            if (expectedThisObject != null) {
                classDescriptor = kFunctions.kMemberFunctionImpl;
            }
            else if (receiverParameter != null) {
                classDescriptor = kFunctions.kExtensionFunctionImpl;
            }
            else {
                classDescriptor = kFunctions.kFunctionImpl;
            }
        }
        else {
            Functions functions = getFunctionsImplList().get(arity);
            if (receiverParameter != null) {
                classDescriptor = functions.extensionFunctionImpl;
            }
            else {
                classDescriptor = functions.functionImpl;
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

    @NotNull
    private static ClassDescriptor createFunctionImpl(
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

        return functionImpl;
    }
}
