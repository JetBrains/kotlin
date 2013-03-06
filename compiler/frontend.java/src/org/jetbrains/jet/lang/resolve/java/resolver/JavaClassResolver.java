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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassNonStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public final class JavaClassResolver {

    private final Map<PsiClass, ClassDescriptor> classDescriptorCache = Maps.newHashMap();

    private BindingTrace trace;
    private JavaSignatureResolver signatureResolver;
    private JavaSemanticServices semanticServices;
    private JavaPackageFragmentProvider packageFragmentProvider;
    private JavaAnnotationResolver annotationResolver;
    private JavaClassObjectResolver classObjectResolver;
    private JavaSupertypeResolver supertypesResolver;
    private PsiDeclarationProviderFactory psiDeclarationProviderFactory;

    public JavaClassResolver() {
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setSignatureResolver(JavaSignatureResolver signatureResolver) {
        this.signatureResolver = signatureResolver;
    }

    @Inject
    public void setClassObjectResolver(JavaClassObjectResolver classObjectResolver) {
        this.classObjectResolver = classObjectResolver;
    }

    @Inject
    public void setSemanticServices(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setSupertypesResolver(JavaSupertypeResolver supertypesResolver) {
        this.supertypesResolver = supertypesResolver;
    }

    @Inject
    public void setPsiDeclarationProviderFactory(PsiDeclarationProviderFactory psiDeclarationProviderFactory) {
        this.psiDeclarationProviderFactory = psiDeclarationProviderFactory;
    }

    @Inject
    public void setPackageFragmentProvider(JavaPackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull PsiClass psiClass) {
        return resolveClass(psiClass, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    @Nullable
    public ClassDescriptor resolveClass(
            @NotNull PsiClass psiClass,
            @NotNull DescriptorSearchRule searchRule
    ) {
        FqName fqName = getFqName(psiClass);

        if (isTraitImplementation(fqName)) {
            // TODO must report an error
            return null;
        }

        if (DescriptorResolverUtils.isKotlinLightClass(psiClass)) {
            return searchRule.processFoundInKotlin(semanticServices.getClassDescriptor(psiClass));
        }

        if (isClassObject(psiClass)) {
            JavaClassObjectResolver.Result result =
                    classObjectResolver.createClassObjectFromPsi(resolveParentClass(psiClass), psiClass);
            cache(psiClass, result.getClassObjectDescriptor());
            return result.getClassObjectDescriptor();
        }

        ClassDescriptor cachedDescriptor = classDescriptorCache.get(psiClass);
        if (cachedDescriptor != null) {
            return cachedDescriptor;
        }

        return createJavaClassDescriptor(psiClass);
    }

    private static boolean isClassObject(@NotNull PsiClass psiClass) {
        if (!JvmAbi.CLASS_OBJECT_CLASS_NAME.equals(psiClass.getName())) return false;
        PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass == null) return false;
        return DescriptorResolverUtils.isKotlinClass(containingClass);
    }

    private static boolean isTraitImplementation(@NotNull FqName qualifiedName) {
        // TODO: only if -$$TImpl class is created by Kotlin
        return qualifiedName.getFqName().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX);
    }

    @NotNull
    private ClassDescriptor createJavaClassDescriptor(
            @NotNull final PsiClass psiClass
    ) {
        DescriptorResolverUtils.checkPsiClassIsNotJet(psiClass);

        ClassOrPackageDescriptor containingDeclaration = resolveParentDescriptor(psiClass);
        // class may be resolved during resolution of parent
        ClassDescriptor cachedDescriptor = classDescriptorCache.get(psiClass);
        if (cachedDescriptor != null) {
            return cachedDescriptor;
        }

        return doCreateClassDescriptor(psiClass, containingDeclaration);
    }

    @NotNull
    private ClassDescriptorFromJvmBytecode doCreateClassDescriptor(
            @NotNull PsiClass psiClass,
            @NotNull ClassOrPackageDescriptor containingDeclaration
    ) {
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);
        AbiVersionUtil.checkAbiVersion(psiClass, jetClassAnnotation, trace);

        ClassKind kind = getClassKind(psiClass, jetClassAnnotation);
        ClassPsiDeclarationProvider classData = psiDeclarationProviderFactory.createBinaryClassData(psiClass);
        ClassDescriptorFromJvmBytecode classDescriptor = new ClassDescriptorFromJvmBytecode(containingDeclaration, kind,
                                                                                            isInnerClass(psiClass));

        cache(psiClass, classDescriptor);
        classDescriptor.setName(Name.identifier(psiClass.getName()));

        List<JavaSignatureResolver.TypeParameterDescriptorInitialization> typeParameterDescriptorInitializations
                = signatureResolver.createUninitializedClassTypeParameters(psiClass, classDescriptor);

        classDescriptor.setTypeParameterDescriptors(getTypeParametersDescriptors(typeParameterDescriptorInitializations));
        List<JetType> supertypes = Lists.newArrayList();
        classDescriptor.setSupertypes(supertypes);
        classDescriptor.setVisibility(DescriptorResolverUtils.resolveVisibility(psiClass, jetClassAnnotation));
        classDescriptor.setModality(resolveModality(psiClass, classDescriptor));
        classDescriptor.createTypeConstructor();
        JavaClassNonStaticMembersScope membersScope = new JavaClassNonStaticMembersScope(classDescriptor, classData, semanticServices);
        classDescriptor.setScopeForMemberLookup(membersScope);
        classDescriptor.setScopeForConstructorResolve(membersScope);

        String context = "class " + psiClass.getQualifiedName();
        signatureResolver.initializeTypeParameters(typeParameterDescriptorInitializations, classDescriptor, context);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        List<TypeParameterDescriptor> classTypeParameters = classDescriptor.getTypeConstructor().getParameters();
        supertypes.addAll(supertypesResolver.getSupertypes(classDescriptor, new PsiClassWrapper(psiClass), classData, classTypeParameters));

        JavaClassObjectResolver.Result classObject = classObjectResolver.createClassObjectDescriptor(classDescriptor, psiClass);
        if (classObject != null) {
            PsiClass classObjectPsiClass = classObject.getClassObjectPsiClass();
            if (classObjectPsiClass != null) {
                cache(classObjectPsiClass, classObject.getClassObjectDescriptor());

            }
            classDescriptor.getBuilder().setClassObjectDescriptor(classObject.getClassObjectDescriptor());
        }

        classDescriptor.setAnnotations(annotationResolver.resolveAnnotations(psiClass));

        trace.record(BindingContext.CLASS, psiClass, classDescriptor);

        return classDescriptor;
    }

    private void cache(@NotNull PsiClass psiClass, @NotNull ClassDescriptor classDescriptor) {
        ClassDescriptor oldValue = classDescriptorCache.put(psiClass, classDescriptor);
        assert oldValue == null;
    }

    @NotNull
    private static List<TypeParameterDescriptor> getTypeParametersDescriptors(
            @NotNull List<JavaSignatureResolver.TypeParameterDescriptorInitialization> typeParameterDescriptorInitializations
    ) {
        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        for (JavaSignatureResolver.TypeParameterDescriptorInitialization typeParameter : typeParameterDescriptorInitializations) {
            typeParameters.add(typeParameter.getDescriptor());
        }
        return typeParameters;
    }

    @NotNull
    private static Modality resolveModality(@NotNull PsiClass psiClass, @NotNull ClassDescriptor classDescriptor) {
        if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            return Modality.FINAL;
        }
        return Modality.convertFromFlags(
                psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                !psiClass.hasModifierProperty(PsiModifier.FINAL));
    }

    @NotNull
    private ClassOrPackageDescriptor resolveParentDescriptor(@NotNull PsiClass psiClass) {
        if (isContainedInClass(psiClass)) {
            return resolveParentClass(psiClass);
        }
        else {
            return packageFragmentProvider.getPackageFragment(getFqName(psiClass).parent());
        }
    }

    @NotNull
    private static FqName getFqName(@NotNull PsiClass psiClass) {
        final String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;
        return new FqName(qualifiedName);
    }

    private static boolean isContainedInClass(@NotNull PsiClass psiClass) {
        return psiClass.getContainingClass() != null;
    }

    private static boolean isInnerClass(@NotNull PsiClass psiClass) {
        return isContainedInClass(psiClass) && !psiClass.hasModifierProperty(PsiModifier.STATIC);
    }

    @NotNull
    private ClassDescriptor resolveParentClass(@NotNull PsiClass psiClass) {
        PsiClass containingClass = psiClass.getContainingClass();
        assert containingClass != null;
        ClassDescriptor parentClass = resolveClass(containingClass, DescriptorSearchRule.INCLUDE_KOTLIN);
        if (parentClass == null) {
            throw new IllegalStateException(
                    "PsiClass not found by name " + getFqName(containingClass) + ", required to be container declaration of " + getFqName(psiClass));
        }
        return parentClass;
    }

    @NotNull
    private static ClassKind getClassKind(@NotNull PsiClass psiClass, @NotNull JetClassAnnotation jetClassAnnotation) {
        if (psiClass.isInterface()) {
            return (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT);
        }
        if (psiClass.isEnum()) {
            return ClassKind.ENUM_CLASS;
        }
        else {
            return jetClassAnnotation.kind() == JvmStdlibNames.FLAG_CLASS_KIND_OBJECT ? ClassKind.OBJECT : ClassKind.CLASS;
        }
    }

}