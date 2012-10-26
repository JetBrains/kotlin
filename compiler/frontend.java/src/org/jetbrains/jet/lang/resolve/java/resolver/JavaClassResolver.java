/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import gnu.trove.THashMap;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.data.ResolverBinaryClassData;
import org.jetbrains.jet.lang.resolve.java.data.ResolverClassData;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassMembersScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public final class JavaClassResolver {

    // NOTE: this complexity is introduced because class descriptors do not always have valid fqnames (class objects)
    private final Map<FqNameBase, ResolverClassData> classDescriptorCache =
            new THashMap<FqNameBase, ResolverClassData>(new TObjectHashingStrategy<FqNameBase>() {
                @Override
                public int computeHashCode(FqNameBase o) {
                    if (o instanceof FqName) {
                        return ((FqName) o).toUnsafe().hashCode();
                    }
                    assert o instanceof FqNameUnsafe;
                    return o.hashCode();
                }

                @Override
                public boolean equals(FqNameBase n1, FqNameBase n2) {
                    return n1.equalsTo(n2.toString()) && n2.equalsTo(n1.toString());
                }
            });

    private BindingTrace trace;
    private JavaDescriptorResolver javaDescriptorResolver;
    private JavaSignatureResolver signatureResolver;
    private JavaSemanticServices semanticServices;
    private JavaAnnotationResolver annotationResolver;
    private PsiClassFinder psiClassFinder;
    private JavaNamespaceResolver namespaceResolver;
    private JavaClassObjectResolver classObjectResolver;
    private JavaSupertypesResolver supertypesResolver;

    public JavaClassResolver() {
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setJavaDescriptorResolver(JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
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
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setNamespaceResolver(JavaNamespaceResolver namespaceResolver) {
        this.namespaceResolver = namespaceResolver;
    }

    @Inject
    public void setSupertypesResolver(JavaSupertypesResolver supertypesResolver) {
        this.supertypesResolver = supertypesResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        PostponedTasks postponedTasks = new PostponedTasks();
        ClassDescriptor clazz = resolveClass(qualifiedName, searchRule, postponedTasks);
        postponedTasks.performTasks();
        return clazz;
    }

    @Nullable
    public ClassDescriptor resolveClass(
            @NotNull FqName qualifiedName,
            @NotNull DescriptorSearchRule searchRule,
            @NotNull PostponedTasks tasks
    ) {
        if (isTraitImplementation(qualifiedName)) {
            return null;
        }

        ClassDescriptor builtinClassDescriptor = semanticServices.getKotlinBuiltinClassDescriptor(qualifiedName);
        if (builtinClassDescriptor != null) {
            return builtinClassDescriptor;
        }

        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            return searchRule.processFoundInKotlin(kotlinClassDescriptor);
        }

        // Not let's take a descriptor of a Java class
        ResolverClassData classData = classDescriptorCache.get(getCorrectedFqName(qualifiedName));
        if (classData != null) {
            return classData.getClassDescriptor();
        }

        return doResolveClass(qualifiedName, tasks);
    }

    @Nullable
    private ClassDescriptor doResolveClass(@NotNull FqName qualifiedName, @NotNull PostponedTasks tasks) {
        PsiClass psiClass = psiClassFinder.findPsiClass(qualifiedName, PsiClassFinder.RuntimeClassesHandleMode.THROW);
        if (psiClass == null) {
            cacheNegativeValue(qualifiedName);
            return null;
        }
        ResolverClassData classData = createJavaClassDescriptor(qualifiedName, psiClass, tasks);
        return classData.getClassDescriptor();
    }

    private void cacheNegativeValue(@NotNull FqName qualifiedName) {
        ResolverClassData oldValue = classDescriptorCache.put(getCorrectedFqName(qualifiedName), ResolverBinaryClassData.NEGATIVE);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at " + qualifiedName);
        }
    }

    private static boolean isTraitImplementation(@NotNull FqName qualifiedName) {
        // TODO: only if -$$TImpl class is created by Kotlin
        return qualifiedName.getFqName().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX);
    }

    @NotNull
    private ResolverClassData createJavaClassDescriptor(
            @NotNull FqName fqName, @NotNull final PsiClass psiClass,
            @NotNull PostponedTasks taskList
    ) {

        checkFqNamesAreConsistent(psiClass, fqName);
        DescriptorResolverUtils.checkPsiClassIsNotJet(psiClass);

        ClassOrNamespaceDescriptor containingDeclaration = resolveParentDescriptor(psiClass);
        // class may be resolved during resolution of parent
        ResolverClassData classData = classDescriptorCache.get(getCorrectedFqName(fqName));
        if (classData != null) {
            return classData;
        }

        return doCreateClassDescriptor(fqName, psiClass, taskList, containingDeclaration);
    }

    @NotNull
    private ResolverClassData doCreateClassDescriptor(
            @NotNull FqName fqName,
            @NotNull PsiClass psiClass,
            @NotNull PostponedTasks taskList,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);
        ClassKind kind = getClassKind(psiClass, jetClassAnnotation);
        ClassDescriptorFromJvmBytecode classDescriptor
                = new ClassDescriptorFromJvmBytecode(containingDeclaration, kind, psiClass, fqName, javaDescriptorResolver);

        ResolverClassData classData = classDescriptor.getResolverBinaryClassData();
        classDescriptorCache.put(getCorrectedFqName(fqName), classData);
        classDescriptor.setName(Name.identifier(psiClass.getName()));

        List<JavaSignatureResolver.TypeParameterDescriptorInitialization> typeParameterDescriptorInitializations
                = signatureResolver.createUninitializedClassTypeParameters(psiClass, classData);

        classDescriptor.setTypeParameterDescriptors(getTypeParametersDescriptors(typeParameterDescriptorInitializations));
        List<JetType> supertypes = Lists.newArrayList();
        classDescriptor.setSupertypes(supertypes);
        classDescriptor.setVisibility(DescriptorResolverUtils.resolveVisibility(psiClass, jetClassAnnotation));
        classDescriptor.setModality(resolveModality(psiClass, classData));
        classDescriptor.createTypeConstructor();
        classDescriptor.setScopeForMemberLookup(new JavaClassMembersScope(semanticServices, classData));

        String context = "class " + psiClass.getQualifiedName();
        signatureResolver.initializeTypeParameters(typeParameterDescriptorInitializations, classDescriptor, context);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        List<TypeParameterDescriptor> classTypeParameters = classDescriptor.getTypeConstructor().getParameters();
        supertypes.addAll(supertypesResolver.getSupertypes(new PsiClassWrapper(psiClass), classData, classTypeParameters));

        ResolverClassData classObjectData = classObjectResolver.createClassObjectDescriptor(classDescriptor, psiClass);
        classDescriptorCache.put(DescriptorResolverUtils.getFqNameForClassObject(psiClass), classObjectData);
        if (classObjectData != null) {
            ClassDescriptorFromJvmBytecode classObjectDescriptor = classObjectData.getClassDescriptor();
            classDescriptor.getBuilder().setClassObjectDescriptor(classObjectDescriptor);
        }

        classDescriptor.setAnnotations(annotationResolver.resolveAnnotations(psiClass, taskList));

        trace.record(BindingContext.CLASS, psiClass, classDescriptor);

        return classData;
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
    private static Modality resolveModality(@NotNull PsiClass psiClass, @NotNull ResolverClassData classData) {
        if (classData.getClassDescriptor().getKind() == ClassKind.ANNOTATION_CLASS) {
            return Modality.FINAL;
        }
        return Modality.convertFromFlags(
                psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                !psiClass.hasModifierProperty(PsiModifier.FINAL));
    }

    void checkFqNamesAreConsistent(@NotNull PsiClass psiClass, @NotNull FqName desiredFqName) {
        final String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;

        FqName fqName = new FqName(qualifiedName);
        assert fqName.equals(desiredFqName);
        if (classDescriptorCache.containsKey(getCorrectedFqName(fqName))) {
            throw new IllegalStateException(qualifiedName);
        }
    }

    @NotNull
    private ClassOrNamespaceDescriptor resolveParentDescriptor(@NotNull PsiClass psiClass) {
        if (isContainedInClass(psiClass)) {
            return resolveParentClass(psiClass);
        }
        else {
            return resolveParentNamespace(psiClass);
        }
    }

    @NotNull
    private static FqName getFqName(@NotNull PsiClass psiClass) {
        final String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;
        return new FqName(qualifiedName);
    }

    @NotNull
    private static FqNameUnsafe getCorrectedFqName(@NotNull FqName rawFqName) {
        StringBuilder correctedFqName = new StringBuilder();
        List<Name> segments = rawFqName.pathSegments();
        for (int i = 0; i < segments.size(); i++) {
            Name segment = segments.get(i);
            if (correctedFqName.length() != 0) {
                correctedFqName.append(".");
            }
            if (JvmAbi.CLASS_OBJECT_CLASS_NAME.equals(segment.getName())) {
                assert i != 0;
                correctedFqName.append(DescriptorUtils.getClassObjectName(segments.get(i - 1)));
            }
            else {
                correctedFqName.append(segment);
            }
        }
        return new FqNameUnsafe(correctedFqName.toString());
    }

    private static boolean isContainedInClass(@NotNull PsiClass psiClass) {
        return psiClass.getContainingClass() != null;
    }

    @NotNull
    private ClassOrNamespaceDescriptor resolveParentClass(@NotNull PsiClass psiClass) {
        PsiClass containingClass = psiClass.getContainingClass();
        assert containingClass != null;
        FqName containerFqName = getFqName(containingClass);
        ClassDescriptor parentClass = resolveClass(containerFqName, DescriptorSearchRule.INCLUDE_KOTLIN);
        if (parentClass == null) {
            throw new IllegalStateException(
                    "PsiClass not found by name " + containerFqName + ", required to be container declaration of " + getFqName(psiClass));
        }
        if (DescriptorResolverUtils.isInnerEnum(psiClass, parentClass) && DescriptorResolverUtils.isKotlinClass(psiClass)) {
            ClassDescriptor classObjectDescriptor = parentClass.getClassObjectDescriptor();
            if (classObjectDescriptor == null) {
                throw new IllegalStateException("Class object for a class with inner enum should've been created earlier: " + parentClass);
            }
            return classObjectDescriptor;
        }
        return parentClass;
    }

    @NotNull
    private ClassOrNamespaceDescriptor resolveParentNamespace(@NotNull PsiClass psiClass) {
        FqName namespaceFqName = getFqName(psiClass).parent();
        NamespaceDescriptor parentNamespace = namespaceResolver.resolveNamespace(namespaceFqName, DescriptorSearchRule.INCLUDE_KOTLIN);
        if (parentNamespace == null) {
            throw new IllegalStateException("cannot resolve namespace " + namespaceFqName +
                                            ", required to be container for " + getFqName(psiClass));
        }
        return parentNamespace;
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

    @Nullable
    public ClassDescriptor resolveClass(FqName name) {
        return resolveClass(name, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }
}