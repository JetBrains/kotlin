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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.FakeOverrideVisibilityResolver;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClassObject;

public final class DescriptorResolverUtils {

    private DescriptorResolverUtils() {
    }

    public static boolean isCompiledKotlinPackageClass(@NotNull JavaClass javaClass) {
        if (javaClass.getOriginKind() == JavaClass.OriginKind.COMPILED) {
            FqName fqName = javaClass.getFqName();
            if (fqName != null && PackageClassUtils.isPackageClassFqName(fqName)) {
                return javaClass.findAnnotation(JvmAnnotationNames.KOTLIN_PACKAGE.getFqName()) != null;
            }
        }
        return false;
    }

    public static boolean isCompiledKotlinClass(@NotNull JavaClass javaClass) {
        if (javaClass.getOriginKind() == JavaClass.OriginKind.COMPILED) {
            return javaClass.findAnnotation(JvmAnnotationNames.KOTLIN_CLASS.getFqName()) != null;
        }
        return false;
    }

    public static boolean isCompiledKotlinClassOrPackageClass(@NotNull JavaClass javaClass) {
        return isCompiledKotlinClass(javaClass) || isCompiledKotlinPackageClass(javaClass);
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Collection<D> resolveOverrides(
            @NotNull Name name,
            @NotNull Collection<D> membersFromSupertypes,
            @NotNull Collection<D> membersFromCurrent,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull final FakeOverrideVisibilityResolver visibilityResolver
    ) {
        final Set<D> result = new HashSet<D>();

        OverrideResolver.generateOverridesInFunctionGroup(
                name, membersFromSupertypes, membersFromCurrent, classDescriptor,
                new OverrideResolver.DescriptorSink() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        visibilityResolver.resolveUnknownVisibilityForMember(fakeOverride);
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
     * @return true if {@code member} is a static member of enum class, which is to be put into its class object (and not into the
     *         corresponding package). This applies to enum entries, values() and valueOf(String) methods
     */
    public static boolean shouldBeInEnumClassObject(@NotNull JavaMember member) {
        if (!member.getContainingClass().isEnum()) return false;

        if (member instanceof JavaField && ((JavaField) member).isEnumEntry()) return true;

        if (!(member instanceof JavaMethod)) return false;
        String signature = JavaSignatureFormatter.getInstance().formatMethod((JavaMethod) member);

        return "values()".equals(signature) ||
               "valueOf(java.lang.String)".equals(signature);
    }

    public static boolean isCorrectOwnerForEnumMember(@NotNull ClassOrNamespaceDescriptor ownerDescriptor, @NotNull JavaMember member) {
        return isEnumClassObject(ownerDescriptor) == shouldBeInEnumClassObject(member);
    }

    public static boolean isObjectMethodInInterface(@NotNull JavaMember member) {
        return member.getContainingClass().isInterface() && member instanceof JavaMethod && isObjectMethod((JavaMethod) member);
    }

    public static boolean isObjectMethod(@NotNull JavaMethod method) {
        String signature = JavaSignatureFormatter.getInstance().formatMethod(method);
        return "hashCode()".equals(signature) ||
               "equals(java.lang.Object)".equals(signature) ||
               "toString()".equals(signature);
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
            return erasure == null ? null : JavaElementFactory.getInstance().createArrayType(erasure);
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
}
