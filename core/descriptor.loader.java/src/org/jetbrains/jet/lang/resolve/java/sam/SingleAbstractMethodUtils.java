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

package org.jetbrains.jet.lang.resolve.java.sam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.descriptor.*;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.types.Variance.INVARIANT;

public class SingleAbstractMethodUtils {
    private SingleAbstractMethodUtils() {
    }

    @NotNull
    public static List<CallableMemberDescriptor> getAbstractMembers(@NotNull JetType type) {
        List<CallableMemberDescriptor> abstractMembers = new ArrayList<CallableMemberDescriptor>();
        for (DeclarationDescriptor member : type.getMemberScope().getAllDescriptors()) {
            if (member instanceof CallableMemberDescriptor && ((CallableMemberDescriptor) member).getModality() == Modality.ABSTRACT) {
                abstractMembers.add((CallableMemberDescriptor) member);
            }
        }
        return abstractMembers;
    }

    private static JetType fixProjections(@NotNull JetType functionType) {
        //removes redundant projection kinds and detects conflicts

        List<TypeParameterDescriptor> typeParameters = functionType.getConstructor().getParameters();
        List<TypeProjection> arguments = new ArrayList<TypeProjection>(typeParameters.size());
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            Variance variance = typeParameter.getVariance();
            TypeProjection argument = functionType.getArguments().get(typeParameter.getIndex());
            Variance kind = argument.getProjectionKind();
            if (kind != INVARIANT && variance != INVARIANT) {
                if (kind == variance) {
                    arguments.add(new TypeProjectionImpl(argument.getType()));
                }
                else {
                    return null;
                }
            }
            else {
                 arguments.add(argument);
            }
        }
        ClassifierDescriptor classifier = functionType.getConstructor().getDeclarationDescriptor();
        assert classifier instanceof ClassDescriptor : "Not class: " + classifier;
        return new JetTypeImpl(
                functionType.getAnnotations(),
                functionType.getConstructor(),
                functionType.isNullable(),
                arguments,
                ((ClassDescriptor) classifier).getMemberScope(arguments)
        );
    }

    @Nullable
    private static JetType getFunctionTypeForSamType(@NotNull JetType samType) {
        // e.g. samType == Comparator<String>?

        ClassifierDescriptor classifier = samType.getConstructor().getDeclarationDescriptor();
        if (classifier instanceof JavaClassDescriptor) {
            // Function2<T, T, Int>
            JetType functionTypeDefault = ((JavaClassDescriptor) classifier).getFunctionTypeForSamInterface();

            if (functionTypeDefault != null) {
                // Function2<String, String, Int>?
                JetType substitute = TypeSubstitutor.create(samType).substitute(functionTypeDefault, Variance.INVARIANT);

                return substitute == null ? null : fixProjections(TypeUtils.makeNullableAsSpecified(substitute, samType.isNullable()));
            }
        }
        return null;
    }

    @NotNull
    public static JetType getFunctionTypeForAbstractMethod(@NotNull FunctionDescriptor function) {
        JetType returnType = function.getReturnType();
        assert returnType != null : "function is not initialized: " + function;
        List<ValueParameterDescriptor> valueParameters = function.getValueParameters();
        List<JetType> parameterTypes = new ArrayList<JetType>(valueParameters.size());
        for (ValueParameterDescriptor parameter : valueParameters) {
            parameterTypes.add(parameter.getType());
        }
        return KotlinBuiltIns.getInstance().getFunctionType(
                Annotations.EMPTY, null, parameterTypes, returnType);
    }

    private static boolean isSamInterface(@NotNull ClassDescriptor klass) {
        if (klass.getKind() != ClassKind.TRAIT) {
            return false;
        }

        List<CallableMemberDescriptor> abstractMembers = getAbstractMembers(klass.getDefaultType());
        if (abstractMembers.size() == 1) {
            CallableMemberDescriptor member = abstractMembers.get(0);
            if (member instanceof SimpleFunctionDescriptor) {
                return member.getTypeParameters().isEmpty();
            }
        }
        return false;
    }

    @NotNull
    public static SamConstructorDescriptor createSamConstructorFunction(
            @NotNull ClassOrPackageFragmentDescriptor owner,
            @NotNull JavaClassDescriptor samInterface
    ) {
        assert isSamInterface(samInterface) : samInterface;

        SamConstructorDescriptor result = new SamConstructorDescriptor(owner, samInterface);

        TypeParameters typeParameters = recreateAndInitializeTypeParameters(samInterface.getTypeConstructor().getParameters(), result);

        JetType parameterTypeUnsubstituted = getFunctionTypeForSamType(samInterface.getDefaultType());
        assert parameterTypeUnsubstituted != null : "couldn't get function type for SAM type " + samInterface.getDefaultType();
        JetType parameterType = typeParameters.substitutor.substitute(parameterTypeUnsubstituted, Variance.IN_VARIANCE);
        assert parameterType != null : "couldn't substitute type: " + parameterTypeUnsubstituted +
                                       ", substitutor = " + typeParameters.substitutor;
        ValueParameterDescriptor parameter = new ValueParameterDescriptorImpl(
                result, null, 0, Annotations.EMPTY, Name.identifier("function"), parameterType, false, null);

        JetType returnType = typeParameters.substitutor.substitute(samInterface.getDefaultType(), Variance.OUT_VARIANCE);
        assert returnType != null : "couldn't substitute type: " + samInterface.getDefaultType() +
                                    ", substitutor = " + typeParameters.substitutor;

        result.initialize(
                null,
                null,
                typeParameters.descriptors,
                Arrays.asList(parameter),
                returnType,
                Modality.FINAL,
                samInterface.getVisibility()
        );

        return result;
    }

    public static boolean isSamType(@NotNull JetType type) {
        return getFunctionTypeForSamType(type) != null;
    }

    public static boolean isSamAdapterNecessary(@NotNull FunctionDescriptor fun) {
        for (ValueParameterDescriptor param : fun.getValueParameters()) {
            if (isSamType(param.getType())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static SamAdapterDescriptor<JavaMethodDescriptor> createSamAdapterFunction(@NotNull final JavaMethodDescriptor original) {
        final SamAdapterFunctionDescriptor result = new SamAdapterFunctionDescriptor(original);
        return initSamAdapter(original, result, new FunctionInitializer() {
            @Override
            public void initialize(
                    @NotNull List<TypeParameterDescriptor> typeParameters,
                    @NotNull List<ValueParameterDescriptor> valueParameters,
                    @Nullable JetType returnType
            ) {
                result.initialize(
                        null,
                        original.getExpectedThisObject(),
                        typeParameters,
                        valueParameters,
                        returnType,
                        Modality.FINAL,
                        original.getVisibility()
                );
            }
        });
    }

    @NotNull
    public static SamAdapterDescriptor<JavaConstructorDescriptor> createSamAdapterConstructor(@NotNull final JavaConstructorDescriptor original) {
        final SamAdapterConstructorDescriptor result = new SamAdapterConstructorDescriptor(original);
        return initSamAdapter(original, result, new FunctionInitializer() {
            @Override
            public void initialize(
                    @NotNull List<TypeParameterDescriptor> typeParameters,
                    @NotNull List<ValueParameterDescriptor> valueParameters,
                    @Nullable JetType returnType
            ) {
                result.initialize(
                        typeParameters,
                        valueParameters,
                        original.getVisibility(),
                        original.getExpectedThisObject() == ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER
                );
            }
        });
    }

    @NotNull
    private static <F extends FunctionDescriptor> SamAdapterDescriptor<F> initSamAdapter(
            @NotNull F original,
            @NotNull SamAdapterDescriptor<F> adapter,
            @NotNull FunctionInitializer initializer
    ) {
        TypeParameters typeParameters = recreateAndInitializeTypeParameters(original.getTypeParameters(), adapter);

        JetType returnTypeUnsubstituted = original.getReturnType();
        JetType returnType;
        if (returnTypeUnsubstituted == null) { // return type may be null for not yet initialized constructors
            returnType = null;
        }
        else {
            returnType = typeParameters.substitutor.substitute(returnTypeUnsubstituted, Variance.OUT_VARIANCE);
            assert returnType != null : "couldn't substitute type: " + returnTypeUnsubstituted +
                                        ", substitutor = " + typeParameters.substitutor;
        }

        List<ValueParameterDescriptor> originalValueParameters = original.getValueParameters();
        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(originalValueParameters.size());
        for (ValueParameterDescriptor originalParam : originalValueParameters) {
            JetType originalType = originalParam.getType();
            JetType functionType = getFunctionTypeForSamType(originalType);
            JetType newTypeUnsubstituted = functionType != null ? functionType : originalType;
            JetType newType = typeParameters.substitutor.substitute(newTypeUnsubstituted, Variance.IN_VARIANCE);
            assert newType != null : "couldn't substitute type: " + newTypeUnsubstituted + ", substitutor = " + typeParameters.substitutor;

            ValueParameterDescriptor newParam = new ValueParameterDescriptorImpl(
                    adapter, null, originalParam.getIndex(), originalParam.getAnnotations(), originalParam.getName(), newType, false, null);
            valueParameters.add(newParam);
        }

        initializer.initialize(typeParameters.descriptors, valueParameters, returnType);

        return adapter;
    }

    @NotNull
    private static TypeParameters recreateAndInitializeTypeParameters(
            @NotNull List<TypeParameterDescriptor> originalParameters,
            @Nullable DeclarationDescriptor newOwner
    ) {
        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> traitToFunTypeParameters =
                DescriptorResolverUtils.recreateTypeParametersAndReturnMapping(originalParameters, newOwner);
        TypeSubstitutor typeParametersSubstitutor = DescriptorResolverUtils.createSubstitutorForTypeParameters(traitToFunTypeParameters);
        for (Map.Entry<TypeParameterDescriptor, TypeParameterDescriptorImpl> mapEntry : traitToFunTypeParameters.entrySet()) {
            TypeParameterDescriptor traitTypeParameter = mapEntry.getKey();
            TypeParameterDescriptorImpl funTypeParameter = mapEntry.getValue();

            for (JetType upperBound : traitTypeParameter.getUpperBounds()) {
                JetType upperBoundSubstituted = typeParametersSubstitutor.substitute(upperBound, Variance.INVARIANT);
                assert upperBoundSubstituted != null : "couldn't substitute type: " + upperBound + ", substitutor = " + typeParametersSubstitutor;
                funTypeParameter.addUpperBound(upperBoundSubstituted);
            }

            funTypeParameter.setInitialized();
        }

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(traitToFunTypeParameters.values());
        return new TypeParameters(typeParameters, typeParametersSubstitutor);
    }

    public static boolean isSamInterface(@NotNull JavaClass javaClass) {
        return getSamInterfaceMethod(javaClass) != null;
    }

    // Returns null if not SAM interface
    @Nullable
    public static JavaMethod getSamInterfaceMethod(@NotNull JavaClass javaClass) {
        FqName fqName = javaClass.getFqName();
        if (fqName == null || fqName.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) {
            return null;
        }
        if (!javaClass.isInterface() || javaClass.isAnnotationType()) {
            return null;
        }

        return findOnlyAbstractMethod(javaClass);
    }

    @Nullable
    private static JavaMethod findOnlyAbstractMethod(@NotNull JavaClass javaClass) {
        OnlyAbstractMethodFinder finder = new OnlyAbstractMethodFinder();
        if (finder.find(javaClass.getDefaultType())) {
            return finder.getFoundMethod();
        }
        return null;
    }

    private static class TypeParameters {
        public final List<TypeParameterDescriptor> descriptors;
        public final TypeSubstitutor substitutor;

        private TypeParameters(List<TypeParameterDescriptor> descriptors, TypeSubstitutor substitutor) {
            this.descriptors = descriptors;
            this.substitutor = substitutor;
        }
    }

    private static abstract class FunctionInitializer {
        public abstract void initialize(
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull List<ValueParameterDescriptor> valueParameters,
                @Nullable JetType returnType
        );
    }

    private static class OnlyAbstractMethodFinder {
        private JavaMethod foundMethod;
        private JavaTypeSubstitutor foundClassSubstitutor;

        private boolean find(@NotNull JavaClassifierType classifierType) {
            JavaTypeSubstitutor classSubstitutor = classifierType.getSubstitutor();
            JavaClassifier classifier = classifierType.getClassifier();
            if (classifier == null) {
                return false; // can't resolve class -> not a SAM interface
            }
            assert classifier instanceof JavaClass : "Classifier should be a class here: " + classifier;
            JavaClass javaClass = (JavaClass) classifier;
            if (DescriptorResolverUtils.OBJECT_FQ_NAME.equals(javaClass.getFqName())) {
                return true;
            }
            for (JavaMethod method : javaClass.getMethods()) {
                if (DescriptorResolverUtils.isObjectMethod(method)) { // e.g., ignore toString() declared in interface
                    continue;
                }
                if (!method.getTypeParameters().isEmpty()) {
                    return false; // if interface has generic methods, it is not a SAM interface
                }

                if (foundMethod == null) {
                    foundMethod = method;
                    foundClassSubstitutor = classSubstitutor;
                    continue;
                }

                if (!areSignaturesErasureEqual(method, classSubstitutor, foundMethod, foundClassSubstitutor)) {
                    return false; // different signatures
                }
            }

            for (JavaClassifierType t : classifierType.getSupertypes()) {
                if (!find(t)) {
                    return false;
                }
            }

            return true;
        }

        /**
         * @see com.intellij.psi.util.MethodSignatureUtil#areSignaturesErasureEqual
         */
        private static boolean areSignaturesErasureEqual(
                @NotNull JavaMethod method1,
                @NotNull JavaTypeSubstitutor substitutor1,
                @NotNull JavaMethod method2,
                @NotNull JavaTypeSubstitutor substitutor2
        ) {
            if (method1.isConstructor() != method2.isConstructor()) return false;
            if (!method1.isConstructor() && !method1.getName().equals(method2.getName())) return false;

            if (method1.isVararg() != method2.isVararg()) return false;

            Collection<JavaValueParameter> parameters1 = method1.getValueParameters();
            Collection<JavaValueParameter> parameters2 = method2.getValueParameters();
            if (parameters1.size() != parameters2.size()) return false;

            for (Iterator<JavaValueParameter> it1 = parameters1.iterator(), it2 = parameters2.iterator(); it1.hasNext(); ) {
                JavaType type1 = DescriptorResolverUtils.erasure(substitutor1.substitute(it1.next().getType()), substitutor1);
                JavaType type2 = DescriptorResolverUtils.erasure(substitutor2.substitute(it2.next().getType()), substitutor2);
                if (!(type1 == null ? type2 == null : type1.equals(type2))) return false;
            }

            return true;
        }

        @Nullable
        private JavaMethod getFoundMethod() {
            return foundMethod;
        }
    }
}
