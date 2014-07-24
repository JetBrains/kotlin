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

package org.jetbrains.jet.lang.resolve.java.resolver;

import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeProjectionImpl;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public final class DescriptorResolverUtils {
    public static final FqName OBJECT_FQ_NAME = new FqName("java.lang.Object");

    private DescriptorResolverUtils() {
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Collection<D> resolveOverrides(
            @NotNull Name name,
            @NotNull Collection<D> membersFromSupertypes,
            @NotNull Collection<D> membersFromCurrent,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull final ErrorReporter errorReporter
    ) {
        final Set<D> result = new HashSet<D>();

        OverridingUtil.generateOverridesInFunctionGroup(
                name, membersFromSupertypes, membersFromCurrent, classDescriptor,
                new OverridingUtil.DescriptorSink() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, new Function1<CallableMemberDescriptor, Unit>() {
                            @Override
                            public Unit invoke(@NotNull CallableMemberDescriptor descriptor) {
                                errorReporter.reportCannotInferVisibility(descriptor);
                                return Unit.VALUE;
                            }
                        });
                        result.add((D) fakeOverride);
                    }

                    @Override
                    public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                        // nop
                    }
                }
        );

        return result;
    }

    @Nullable
    public static ValueParameterDescriptor getAnnotationParameterByName(@NotNull Name name, @NotNull ClassDescriptor annotationClass) {
        Collection<ConstructorDescriptor> constructors = annotationClass.getConstructors();
        assert constructors.size() == 1 : "Annotation class descriptor must have only one constructor";

        for (ValueParameterDescriptor parameter : constructors.iterator().next().getValueParameters()) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    /**
     * @return true if {@code method} is a static method of enum class, which is to be put into its class object (and not into the
     *         corresponding package). This applies to values() and valueOf(String) methods
     */
    public static boolean shouldBeInEnumClassObject(@NotNull JavaMethod method) {
        if (!method.getContainingClass().isEnum()) return false;

        String name = method.getName().asString();
        if (name.equals("values")) {
            return method.getValueParameters().isEmpty();
        }
        else if (name.equals("valueOf")) {
            return isMethodWithOneParameterWithFqName(method, "java.lang.String");
        }
        return false;
    }

    public static boolean isObjectMethodInInterface(@NotNull JavaMember member) {
        return member.getContainingClass().isInterface() && member instanceof JavaMethod && isObjectMethod((JavaMethod) member);
    }

    public static boolean isObjectMethod(@NotNull JavaMethod method) {
        String name = method.getName().asString();
        if (name.equals("toString") || name.equals("hashCode")) {
            return method.getValueParameters().isEmpty();
        }
        else if (name.equals("equals")) {
            return isMethodWithOneParameterWithFqName(method, "java.lang.Object");
        }
        return false;
    }

    private static boolean isMethodWithOneParameterWithFqName(@NotNull JavaMethod method, @NotNull String fqName) {
        List<JavaValueParameter> parameters = method.getValueParameters();
        if (parameters.size() == 1) {
            JavaType type = parameters.get(0).getType();
            if (type instanceof JavaClassifierType) {
                JavaClassifier classifier = ((JavaClassifierType) type).getClassifier();
                if (classifier instanceof JavaClass) {
                    FqName classFqName = ((JavaClass) classifier).getFqName();
                    return classFqName != null && classFqName.asString().equals(fqName);
                }
            }
        }
        return false;
    }

    @NotNull
    public static Collection<JavaClass> getClassesInPackage(@NotNull JavaPackage javaPackage) {
        Collection<JavaClass> classes = javaPackage.getClasses();
        Set<FqName> addedQualifiedNames = new HashSet<FqName>(classes.size());
        List<JavaClass> result = new ArrayList<JavaClass>(classes.size());

        for (JavaClass javaClass : classes) {
            FqName fqName = javaClass.getFqName();
            if (fqName != null && addedQualifiedNames.add(fqName)) {
                result.add(javaClass);
            }
        }

        return result;
    }

    /**
     * @see com.intellij.psi.util.TypeConversionUtil#erasure(com.intellij.psi.PsiType)
     */
    @Nullable
    public static JavaType erasure(@NotNull JavaType type) {
        return erasure(type, JavaTypeSubstitutor.EMPTY);
    }

    /**
     * @see com.intellij.psi.util.TypeConversionUtil#erasure(com.intellij.psi.PsiType, com.intellij.psi.PsiSubstitutor)
     */
    @Nullable
    public static JavaType erasure(@NotNull JavaType type, @NotNull JavaTypeSubstitutor substitutor) {
        if (type instanceof JavaClassifierType) {
            JavaClassifier classifier = ((JavaClassifierType) type).getClassifier();
            if (classifier instanceof JavaClass) {
                return ((JavaClass) classifier).getDefaultType();
            }
            else if (classifier instanceof JavaTypeParameter) {
                JavaTypeParameter typeParameter = (JavaTypeParameter) classifier;
                return typeParameterErasure(typeParameter, new HashSet<JavaTypeParameter>(), substitutor);
            }
            else {
                return null;
            }
        }
        else if (type instanceof JavaPrimitiveType) {
            return type;
        }
        else if (type instanceof JavaArrayType) {
            JavaType erasure = erasure(((JavaArrayType) type).getComponentType(), substitutor);
            return erasure == null ? null : erasure.createArrayType();
        }
        else if (type instanceof JavaWildcardType) {
            JavaWildcardType wildcardType = (JavaWildcardType) type;
            JavaType bound = wildcardType.getBound();
            if (bound != null && wildcardType.isExtends()) {
                return erasure(bound, substitutor);
            }
            return wildcardType.getTypeProvider().createJavaLangObjectType();
        }
        else {
            throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    /**
     * @see com.intellij.psi.util.TypeConversionUtil#typeParameterErasure(com.intellij.psi.PsiTypeParameter)
     */
    @Nullable
    private static JavaType typeParameterErasure(
            @NotNull JavaTypeParameter typeParameter,
            @NotNull HashSet<JavaTypeParameter> visited,
            @NotNull JavaTypeSubstitutor substitutor
    ) {
        Collection<JavaClassifierType> upperBounds = typeParameter.getUpperBounds();
        if (!upperBounds.isEmpty()) {
            JavaClassifier classifier = upperBounds.iterator().next().getClassifier();
            if (classifier instanceof JavaTypeParameter && !visited.contains(classifier)) {
                JavaTypeParameter typeParameterBound = (JavaTypeParameter) classifier;
                visited.add(typeParameterBound);
                JavaType substitutedType = substitutor.substitute(typeParameterBound);
                if (substitutedType != null) {
                    return erasure(substitutedType);
                }
                return typeParameterErasure(typeParameterBound, visited, substitutor);
            }
            else if (classifier instanceof JavaClass) {
                return ((JavaClass) classifier).getDefaultType();
            }
        }
        return typeParameter.getTypeProvider().createJavaLangObjectType();
    }

    @NotNull
    public static Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> recreateTypeParametersAndReturnMapping(
            @NotNull List<TypeParameterDescriptor> originalParameters,
            @Nullable DeclarationDescriptor newOwner
    ) {
        // LinkedHashMap to save the order of type parameters
        Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> result =
                new LinkedHashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>();
        for (TypeParameterDescriptor typeParameter : originalParameters) {
            result.put(typeParameter,
                       TypeParameterDescriptorImpl.createForFurtherModification(
                               newOwner == null ? typeParameter.getContainingDeclaration() : newOwner,
                               typeParameter.getAnnotations(),
                               typeParameter.isReified(),
                               typeParameter.getVariance(),
                               typeParameter.getName(),
                               typeParameter.getIndex(),
                               SourceElement.NO_SOURCE)
            );
        }
        return result;
    }

    @NotNull
    public static TypeSubstitutor createSubstitutorForTypeParameters(
            @NotNull Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters
    ) {
        Map<TypeConstructor, TypeProjection> typeSubstitutionContext = new HashMap<TypeConstructor, TypeProjection>();
        for (Map.Entry<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameter : originalToAltTypeParameters
                .entrySet()) {
            typeSubstitutionContext.put(originalToAltTypeParameter.getKey().getTypeConstructor(),
                                        new TypeProjectionImpl(originalToAltTypeParameter.getValue().getDefaultType()));
        }
        return TypeSubstitutor.create(typeSubstitutionContext);
    }

    public static boolean hasStaticMembers(@NotNull JavaClass javaClass) {
        for (JavaMethod method : javaClass.getMethods()) {
            if (method.isStatic() && !shouldBeInEnumClassObject(method)) {
                return true;
            }
        }

        for (JavaField field : javaClass.getFields()) {
            if (field.isStatic() && !field.isEnumEntry()) {
                return true;
            }
        }

        for (JavaClass nestedClass : javaClass.getInnerClasses()) {
            if (SingleAbstractMethodUtils.isSamInterface(nestedClass)) {
                return true;
            }
            if (nestedClass.isStatic() && hasStaticMembers(nestedClass)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public static ClassDescriptor getKotlinBuiltinClassDescriptor(@NotNull FqName qualifiedName) {
        if (!qualifiedName.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return null;

        List<Name> segments = qualifiedName.pathSegments();
        if (segments.size() < 2) return null;

        JetScope scope = KotlinBuiltIns.getInstance().getBuiltInsPackageScope();
        for (int i = 1, size = segments.size(); i < size; i++) {
            ClassifierDescriptor classifier = scope.getClassifier(segments.get(i));
            if (classifier == null) return null;
            assert classifier instanceof ClassDescriptor : "Unexpected classifier in built-ins: " + classifier;
            scope = ((ClassDescriptor) classifier).getUnsubstitutedInnerClassesScope();
        }

        return (ClassDescriptor) scope.getContainingDeclaration();
    }
}
