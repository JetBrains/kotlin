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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeProjectionImpl;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqNameSafe;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClassObject;

public final class DescriptorResolverUtils {
    private DescriptorResolverUtils() {
    }

    public static boolean isCompiledKotlinPackageClass(@NotNull JavaClass javaClass) {
        if (javaClass.getOriginKind() == JavaClass.OriginKind.COMPILED) {
            return javaClass.findAnnotation(JvmAnnotationNames.KOTLIN_PACKAGE) != null
                   || javaClass.findAnnotation(JvmAnnotationNames.KOTLIN_PACKAGE_FRAGMENT) != null;
        }
        return false;
    }

    public static boolean isCompiledKotlinClass(@NotNull JavaClass javaClass) {
        if (javaClass.getOriginKind() == JavaClass.OriginKind.COMPILED) {
            return javaClass.findAnnotation(JvmAnnotationNames.KOTLIN_CLASS) != null;
        }
        return false;
    }

    private static boolean isCompiledKotlinClassOrPackageClass(@NotNull JavaClass javaClass) {
        return isCompiledKotlinClass(javaClass) || isCompiledKotlinPackageClass(javaClass);
    }

    @NotNull
    public static FqName fqNameByClass(@NotNull Class<?> clazz) {
        return new FqName(clazz.getCanonicalName());
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
                        OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, new OverridingUtil.NotInferredVisibilitySink() {
                            @Override
                            public void cannotInferVisibility(@NotNull CallableMemberDescriptor descriptor) {
                                errorReporter.reportCannotInferVisibility(descriptor);
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

        String signature = JavaSignatureFormatter.getInstance().formatMethod(method);

        return "values()".equals(signature) ||
               "valueOf(java.lang.String)".equals(signature);
    }

    public static boolean isCorrectOwnerForEnumMethod(@NotNull ClassOrNamespaceDescriptor ownerDescriptor, @NotNull JavaMethod method) {
        return isEnumClassObject(ownerDescriptor) == shouldBeInEnumClassObject(method);
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
                               typeParameter.getIndex()));
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

    @Nullable
    private static JavaClassDescriptor findClassInScope(@NotNull JetScope memberScope, @NotNull Name name) {
        ClassifierDescriptor classifier = memberScope.getClassifier(name);
        if (classifier instanceof JavaClassDescriptor) {
            return (JavaClassDescriptor) classifier;
        }
        return null;
    }

    // E.g. we have foo.Bar.Baz class declared in Java. It will produce the following descriptors structure:
    // package fragment foo
    // +-- class Bar
    // |    +-- class Baz
    // +-- package fragment Bar
    // We need to find class 'Baz' in fragment 'foo.Bar'.
    @Nullable
    static JavaClassDescriptor findClassInPackage(@NotNull JavaPackageFragmentDescriptor fragment, @NotNull Name name) {
        // First, try to find in fragment directly
        JavaClassDescriptor found = findClassInScope(fragment.getMemberScope(), name);
        if (found != null) {
            return found;
        }

        // If unsuccessful, try to find class of the same name as current (class 'foo.Bar')
        JavaPackageFragmentDescriptor parentPackage = getParentPackage(fragment);
        if (parentPackage == null) {
            return null;
        }

        // Calling recursively, looking for 'Bar' in 'foo'
        ClassDescriptor classForCurrentPackage = findClassInPackage(parentPackage, fragment.getName());
        if (classForCurrentPackage != null) {
            // Try to find nested class 'Baz' in class 'foo.Bar'
            return findClassInScope(DescriptorUtils.getStaticNestedClassesScope(classForCurrentPackage), name);
        }

        return null;
    }

    @Nullable
    private static JavaPackageFragmentDescriptor getParentPackage(@NotNull JavaPackageFragmentDescriptor fragment) {
        FqName fqName = fragment.getFqName();
        if (fqName.isRoot()) {
            return null;
        }

        JavaPackageFragmentDescriptor parentPackage = fragment.getProvider().getOrCreatePackage(fqName.parent());
        assert parentPackage != null : " couldn't find parent package for " + fragment;
        return parentPackage;
    }

    @Nullable
    public static JavaClassDescriptor getClassForCorrespondingJavaPackage(@NotNull JavaPackageFragmentDescriptor fragment) {
        JavaPackageFragmentDescriptor parentPackage = getParentPackage(fragment);
        if (parentPackage == null) {
            return null;
        }

        return findClassInPackage(parentPackage, fragment.getFqName().shortName());
    }

    @Nullable
    public static JavaPackageFragmentDescriptor getPackageForCorrespondingJavaClass(@NotNull JavaClassDescriptor javaClass) {
        PackageFragmentDescriptor packageFragment = DescriptorUtils.getParentOfType(javaClass, PackageFragmentDescriptor.class);
        assert packageFragment instanceof JavaPackageFragmentDescriptor :
                "java class " + javaClass + " is under non-java fragment: " + packageFragment;

        JavaPackageFragmentProvider provider = ((JavaPackageFragmentDescriptor) packageFragment).getProvider();
        return provider.getOrCreatePackage(getFqNameSafe(javaClass));
    }

    public static boolean isJavaClassVisibleAsPackage(@NotNull JavaClass javaClass) {
        return !isCompiledKotlinClassOrPackageClass(javaClass) && hasStaticMembers(javaClass);
    }

    private static boolean hasStaticMembers(@NotNull JavaClass javaClass) {
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
}
