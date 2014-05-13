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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.reflect.ReflectionTypes;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class JvmFunctionImplTypes {
    private final ReflectionTypes reflectionTypes;

    private final ClassDescriptor functionImpl;
    private final ClassDescriptor extensionFunctionImpl;

    private volatile List<KFunctions> kFunctionsList;

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
        ModuleDescriptor fakeModule = new ModuleDescriptorImpl(Name.special("<fake module for functions impl>"),
                                                               Collections.<ImportPath>emptyList(), JavaToKotlinClassMap.getInstance());

        MutablePackageFragmentDescriptor kotlinJvmInternal =
                new MutablePackageFragmentDescriptor(fakeModule, new FqName("kotlin.jvm.internal"));

        MutableClassDescriptor functionImpl = createClass(kotlinJvmInternal, "FunctionImpl");
        TypeParameterDescriptor funR = createTypeParameter(functionImpl, Variance.OUT_VARIANCE, "R", 0);

        MutableClassDescriptor extensionFunctionImpl = createClass(kotlinJvmInternal, "ExtensionFunctionImpl");
        TypeParameterDescriptor extFunT = createTypeParameter(extensionFunctionImpl, Variance.IN_VARIANCE, "T", 0);
        TypeParameterDescriptor extFunR = createTypeParameter(extensionFunctionImpl, Variance.OUT_VARIANCE, "R", 1);

        this.functionImpl = initializeFunctionImplClass(functionImpl, Arrays.asList(funR));
        this.extensionFunctionImpl = initializeFunctionImplClass(extensionFunctionImpl, Arrays.asList(extFunT, extFunR));
    }

    @NotNull
    private List<KFunctions> getKFunctionsImplList() {
        if (kFunctionsList == null) {
            MutablePackageFragmentDescriptor packageFragment = new MutablePackageFragmentDescriptor(
                    DescriptorUtils.getContainingModule(functionImpl), new FqName("kotlin.reflect.jvm.internal")
            );

            ImmutableList.Builder<KFunctions> builder = ImmutableList.builder();
            for (int i = 0; i < KotlinBuiltIns.FUNCTION_TRAIT_COUNT; i++) {
                builder.add(new KFunctions(
                        createKFunctionImpl(packageFragment, "KFunction" + i + "Impl", reflectionTypes.getKFunction(i)),
                        createKFunctionImpl(packageFragment, "KMemberFunction" + i + "Impl", reflectionTypes.getKMemberFunction(i)),
                        createKFunctionImpl(packageFragment, "KExtensionFunction" + i + "Impl", reflectionTypes.getKExtensionFunction(i))
                ));
            }
            kFunctionsList = builder.build();
        }
        return kFunctionsList;
    }

    @NotNull
    private static ClassDescriptor createKFunctionImpl(
            @NotNull PackageFragmentDescriptor containingDeclaration,
            @NotNull String name,
            @NotNull ClassDescriptor functionInterface
    ) {
        return initializeFunctionImplClass(createClass(containingDeclaration, name),
                                           functionInterface.getDefaultType().getConstructor().getParameters());
    }


    @NotNull
    public Collection<JetType> getSupertypesForClosure(@NotNull FunctionDescriptor descriptor) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();

        List<TypeProjection> typeArguments = new ArrayList<TypeProjection>(2);

        ClassDescriptor classDescriptor;
        if (receiverParameter != null) {
            classDescriptor = extensionFunctionImpl;
            typeArguments.add(new TypeProjectionImpl(receiverParameter.getType()));
        }
        else {
            classDescriptor = functionImpl;
        }

        //noinspection ConstantConditions
        typeArguments.add(new TypeProjectionImpl(descriptor.getReturnType()));

        JetType functionImplType = new JetTypeImpl(
                classDescriptor.getDefaultType().getAnnotations(),
                classDescriptor.getTypeConstructor(),
                false,
                typeArguments,
                classDescriptor.getMemberScope(typeArguments)
        );

        JetType functionType = KotlinBuiltIns.getInstance().getFunctionType(
                Annotations.EMPTY,
                receiverParameter == null ? null : receiverParameter.getType(),
                DescriptorUtils.getValueParametersTypes(descriptor.getValueParameters()),
                descriptor.getReturnType()
        );

        return Arrays.asList(functionImplType, functionType);
    }

    @NotNull
    public JetType getSupertypeForCallableReference(@NotNull FunctionDescriptor descriptor) {
        int arity = descriptor.getValueParameters().size();

        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        ReceiverParameterDescriptor expectedThisObject = descriptor.getExpectedThisObject();

        List<TypeProjection> typeArguments = new ArrayList<TypeProjection>(arity + 2);
        if (receiverParameter != null) {
            typeArguments.add(new TypeProjectionImpl(receiverParameter.getType()));
        }
        else if (expectedThisObject != null) {
            typeArguments.add(new TypeProjectionImpl(expectedThisObject.getType()));
        }

        for (ValueParameterDescriptor parameter : descriptor.getValueParameters()) {
            typeArguments.add(new TypeProjectionImpl(parameter.getType()));
        }

        //noinspection ConstantConditions
        typeArguments.add(new TypeProjectionImpl(descriptor.getReturnType()));

        ClassDescriptor classDescriptor;
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

        return new JetTypeImpl(
                classDescriptor.getDefaultType().getAnnotations(),
                classDescriptor.getTypeConstructor(),
                false,
                typeArguments,
                classDescriptor.getMemberScope(typeArguments)
        );
    }

    @NotNull
    private static MutableClassDescriptor createClass(@NotNull PackageFragmentDescriptor packageFragment, @NotNull String name) {
        return new MutableClassDescriptor(packageFragment, packageFragment.getMemberScope(), ClassKind.CLASS, false, Name.identifier(name));
    }

    @NotNull
    private static TypeParameterDescriptor createTypeParameter(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull Variance variance,
            @NotNull String name,
            int index
    ) {
        TypeParameterDescriptorImpl typeParameter = TypeParameterDescriptorImpl.createForFurtherModification(
                classDescriptor, Annotations.EMPTY, false, variance, Name.identifier(name), index
        );
        typeParameter.setInitialized();
        return typeParameter;
    }

    @NotNull
    private static ClassDescriptor initializeFunctionImplClass(
            @NotNull MutableClassDescriptor functionImpl,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {
        functionImpl.setModality(Modality.FINAL);
        functionImpl.setVisibility(Visibilities.PUBLIC);
        functionImpl.setTypeParameterDescriptors(typeParameters);
        functionImpl.createTypeConstructor();

        return functionImpl;
    }
}
