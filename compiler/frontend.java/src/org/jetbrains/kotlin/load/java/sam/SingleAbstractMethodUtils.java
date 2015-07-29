/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.sam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils;
import org.jetbrains.kotlin.load.java.descriptors.*;
import org.jetbrains.kotlin.load.java.lazy.types.LazyJavaTypeResolver;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JavaResolverUtils;
import org.jetbrains.kotlin.resolve.jvm.JvmPackage;
import org.jetbrains.kotlin.types.*;

import java.util.*;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;
import static org.jetbrains.kotlin.types.Variance.INVARIANT;

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
                functionType.isMarkedNullable(),
                arguments,
                ((ClassDescriptor) classifier).getMemberScope(arguments)
        );
    }

    @Nullable
    private static JetType getFunctionTypeForSamType(@NotNull JetType samType, boolean isSamConstructor) {
        // e.g. samType == Comparator<String>?

        ClassifierDescriptor classifier = samType.getConstructor().getDeclarationDescriptor();
        if (classifier instanceof JavaClassDescriptor) {
            // Function2<T, T, Int>
            JetType functionTypeDefault = ((JavaClassDescriptor) classifier).getFunctionTypeForSamInterface();

            if (functionTypeDefault != null) {
                // Function2<String, String, Int>?
                JetType substitute = TypeSubstitutor.create(samType).substitute(functionTypeDefault, Variance.INVARIANT);

                if (substitute == null) return null;

                JetType type = fixProjections(substitute);
                if (type == null) return null;

                if (JvmPackage.getPLATFORM_TYPES() && !isSamConstructor && TypesPackage.isNullabilityFlexible(samType)) {
                    return LazyJavaTypeResolver.FlexibleJavaClassifierTypeCapabilities.create(type, TypeUtils.makeNullable(type));
                }

                return TypeUtils.makeNullableAsSpecified(type, !isSamConstructor && samType.isMarkedNullable());
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
        return getBuiltIns(function).getFunctionType(Annotations.EMPTY, null, parameterTypes, returnType);
    }

    private static boolean isSamInterface(@NotNull ClassDescriptor klass) {
        if (klass.getKind() != ClassKind.INTERFACE) {
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
            @NotNull DeclarationDescriptor owner,
            @NotNull JavaClassDescriptor samInterface
    ) {
        assert isSamInterface(samInterface) : samInterface;

        SamConstructorDescriptor result = new SamConstructorDescriptor(owner, samInterface);

        TypeParameters typeParameters = recreateAndInitializeTypeParameters(samInterface.getTypeConstructor().getParameters(), result);

        JetType parameterTypeUnsubstituted = getFunctionTypeForSamType(samInterface.getDefaultType(), true);
        assert parameterTypeUnsubstituted != null : "couldn't get function type for SAM type " + samInterface.getDefaultType();
        JetType parameterType = typeParameters.substitutor.substitute(parameterTypeUnsubstituted, Variance.IN_VARIANCE);
        assert parameterType != null : "couldn't substitute type: " + parameterTypeUnsubstituted +
                                       ", substitutor = " + typeParameters.substitutor;
        ValueParameterDescriptor parameter = new ValueParameterDescriptorImpl(
                result, null, 0, Annotations.EMPTY, Name.identifier("function"), parameterType, false, null, SourceElement.NO_SOURCE);

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
        return getFunctionTypeForSamType(type, /* irrelevant */ false) != null;
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
                    @NotNull JetType returnType
            ) {
                result.initialize(
                        null,
                        original.getDispatchReceiverParameter(),
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
                    @NotNull JetType returnType
            ) {
                result.initialize(typeParameters, valueParameters, original.getVisibility());
                result.setReturnType(returnType);
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
        assert returnTypeUnsubstituted != null : "Creating SAM adapter for not initialized original: " + original;

        TypeSubstitutor substitutor = typeParameters.substitutor;
        JetType returnType = substitutor.substitute(returnTypeUnsubstituted, Variance.OUT_VARIANCE);
        assert returnType != null : "couldn't substitute type: " + returnTypeUnsubstituted +
                                        ", substitutor = " + substitutor;


        List<ValueParameterDescriptor> valueParameters = createValueParametersForSamAdapter(original, adapter, substitutor);

        initializer.initialize(typeParameters.descriptors, valueParameters, returnType);

        return adapter;
    }

    public static List<ValueParameterDescriptor> createValueParametersForSamAdapter(
            @NotNull FunctionDescriptor original,
            @NotNull FunctionDescriptor samAdapter,
            @NotNull TypeSubstitutor substitutor
    ) {
        List<ValueParameterDescriptor> originalValueParameters = original.getValueParameters();
        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(originalValueParameters.size());
        for (ValueParameterDescriptor originalParam : originalValueParameters) {
            JetType originalType = originalParam.getType();
            JetType functionType = getFunctionTypeForSamType(originalType, false);
            JetType newTypeUnsubstituted = functionType != null ? functionType : originalType;
            JetType newType = substitutor.substitute(newTypeUnsubstituted, Variance.IN_VARIANCE);
            assert newType != null : "couldn't substitute type: " + newTypeUnsubstituted + ", substitutor = " + substitutor;

            ValueParameterDescriptor newParam = new ValueParameterDescriptorImpl(
                    samAdapter, null, originalParam.getIndex(), originalParam.getAnnotations(),
                    originalParam.getName(), newType, false, null, SourceElement.NO_SOURCE
            );
            valueParameters.add(newParam);
        }
        return valueParameters;
    }

    @NotNull
    private static TypeParameters recreateAndInitializeTypeParameters(
            @NotNull List<TypeParameterDescriptor> originalParameters,
            @Nullable DeclarationDescriptor newOwner
    ) {
        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> traitToFunTypeParameters =
                JavaResolverUtils.recreateTypeParametersAndReturnMapping(originalParameters, newOwner);
        TypeSubstitutor typeParametersSubstitutor = JavaResolverUtils.createSubstitutorForTypeParameters(traitToFunTypeParameters);
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
                @NotNull JetType returnType
        );
    }

    private static class OnlyAbstractMethodFinder {
        private static final FqName OBJECT_FQ_NAME = new FqName("java.lang.Object");

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
            if (OBJECT_FQ_NAME.equals(javaClass.getFqName())) {
                return true;
            }
            for (JavaMethod method : javaClass.getMethods()) {

                //skip java 8 default methods
                if (!method.isAbstract()) {
                    continue;
                }

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
            if (!method1.getName().equals(method2.getName())) return false;

            Collection<JavaValueParameter> parameters1 = method1.getValueParameters();
            Collection<JavaValueParameter> parameters2 = method2.getValueParameters();
            if (parameters1.size() != parameters2.size()) return false;

            for (Iterator<JavaValueParameter> it1 = parameters1.iterator(), it2 = parameters2.iterator(); it1.hasNext(); ) {
                JavaValueParameter param1 = it1.next();
                JavaValueParameter param2 = it2.next();
                if (param1.isVararg() != param2.isVararg()) return false;

                JavaType type1 = JavaResolverUtils.erasure(substitutor1.substitute(param1.getType()), substitutor1);
                JavaType type2 = JavaResolverUtils.erasure(substitutor2.substitute(param2.getType()), substitutor2);
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
