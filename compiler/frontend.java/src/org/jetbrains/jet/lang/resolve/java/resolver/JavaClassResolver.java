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
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
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
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.MembersCache;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassNonStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorResolver.createEnumClassObjectValueOfMethod;
import static org.jetbrains.jet.lang.resolve.DescriptorResolver.createEnumClassObjectValuesMethod;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;

public final class JavaClassResolver {

    // NOTE: this complexity is introduced because class descriptors do not always have valid fqnames (class objects)
    @NotNull
    private final Map<FqNameBase, ClassDescriptor> classDescriptorCache =
            new THashMap<FqNameBase, ClassDescriptor>(new TObjectHashingStrategy<FqNameBase>() {
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

    @NotNull
    private final Set<FqNameBase> unresolvedCache = Sets.newHashSet();

    private BindingTrace trace;
    private JavaSignatureResolver signatureResolver;
    private JavaSemanticServices semanticServices;
    private JavaAnnotationResolver annotationResolver;
    private PsiClassFinder psiClassFinder;
    private JavaNamespaceResolver namespaceResolver;
    private JavaSupertypeResolver supertypesResolver;
    private JavaFunctionResolver functionResolver;
    private DeserializedDescriptorResolver kotlinDescriptorResolver;

    public JavaClassResolver() {
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setKotlinDescriptorResolver(DeserializedDescriptorResolver kotlinDescriptorResolver) {
        this.kotlinDescriptorResolver = kotlinDescriptorResolver;
    }

    @Inject
    public void setSignatureResolver(JavaSignatureResolver signatureResolver) {
        this.signatureResolver = signatureResolver;
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
    public void setSupertypesResolver(JavaSupertypeResolver supertypesResolver) {
        this.supertypesResolver = supertypesResolver;
    }

    @Inject
    public void setFunctionResolver(JavaFunctionResolver functionResolver) {
        this.functionResolver = functionResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        PostponedTasks postponedTasks = new PostponedTasks();
        ClassDescriptor classDescriptor = resolveClass(qualifiedName, searchRule, postponedTasks);
        postponedTasks.performTasks();
        return classDescriptor;
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

        ClassDescriptor builtinClassDescriptor = getKotlinBuiltinClassDescriptor(qualifiedName);
        if (builtinClassDescriptor != null) {
            return builtinClassDescriptor;
        }

        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            return searchRule.processFoundInKotlin(kotlinClassDescriptor);
        }

        // Not let's take a descriptor of a Java class
        FqNameUnsafe fqName = javaClassToKotlinFqName(qualifiedName);
        ClassDescriptor cachedDescriptor = classDescriptorCache.get(fqName);
        if (cachedDescriptor != null) {
            return cachedDescriptor;
        }

        if (unresolvedCache.contains(fqName)) {
            return null;
        }

        return doResolveClass(qualifiedName, tasks);
    }

    @Nullable
    private static ClassDescriptor getKotlinBuiltinClassDescriptor(@NotNull FqName qualifiedName) {
        if (!qualifiedName.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return null;

        List<Name> segments = qualifiedName.pathSegments();
        if (segments.size() < 2) return null;

        JetScope scope = KotlinBuiltIns.getInstance().getBuiltInsScope();
        for (int i = 1, size = segments.size(); i < size; i++) {
            ClassifierDescriptor classifier = scope.getClassifier(segments.get(i));
            if (classifier == null) return null;
            assert classifier instanceof ClassDescriptor : "Unexpected classifier in built-ins: " + classifier;
            scope = ((ClassDescriptor) classifier).getUnsubstitutedInnerClassesScope();
        }

        return (ClassDescriptor) scope.getContainingDeclaration();
    }

    private ClassDescriptor doResolveClass(@NotNull FqName qualifiedName, @NotNull PostponedTasks tasks) {
        PsiClass psiClass = psiClassFinder.findPsiClass(qualifiedName, PsiClassFinder.RuntimeClassesHandleMode.REPORT_ERROR);
        if (psiClass == null) {
            cacheNegativeValue(javaClassToKotlinFqName(qualifiedName));
            return null;
        }

        // Class may have been resolved previously by different Java resolver instance, and we are reusing its trace
        ClassDescriptor alreadyResolved = trace.get(BindingContext.CLASS, psiClass);
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        return createJavaClassDescriptor(qualifiedName, psiClass, tasks);
    }

    private void cacheNegativeValue(@NotNull FqNameBase qualifiedName) {
        if (unresolvedCache.contains(qualifiedName) || classDescriptorCache.containsKey(qualifiedName)) {
            throw new IllegalStateException("rewrite at " + qualifiedName);
        }
        unresolvedCache.add(qualifiedName);
    }

    private static boolean isTraitImplementation(@NotNull FqName qualifiedName) {
        // TODO: only if -$$TImpl class is created by Kotlin
        return qualifiedName.asString().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX);
    }

    @NotNull
    private ClassDescriptor createJavaClassDescriptor(
            @NotNull FqName fqName, @NotNull PsiClass psiClass,
            @NotNull PostponedTasks taskList
    ) {
        checkFqNamesAreConsistent(psiClass, fqName);
        DescriptorResolverUtils.checkPsiClassIsNotJet(psiClass);

        ClassOrNamespaceDescriptor containingDeclaration = resolveParentDescriptor(psiClass);
        // class may be resolved during resolution of parent
        ClassDescriptor cachedDescriptor = classDescriptorCache.get(javaClassToKotlinFqName(fqName));
        if (cachedDescriptor != null) {
            return cachedDescriptor;
        }

        assert (!unresolvedCache.contains(fqName)) : "We can resolve the class, so it can't be 'unresolved' during parent resolution";

        ClassDescriptor deserializedDescriptor = kotlinDescriptorResolver.resolveClass(fqName, psiClass, containingDeclaration);
        if (deserializedDescriptor != null) {
            //TODO: class object and psi class
            cache(javaClassToKotlinFqName(fqName), deserializedDescriptor);
            return deserializedDescriptor;
        }

        return doCreateClassDescriptor(fqName, psiClass, taskList, containingDeclaration);
    }

    @NotNull
    private ClassDescriptorFromJvmBytecode doCreateClassDescriptor(
            @NotNull FqName fqName,
            @NotNull PsiClass psiClass,
            @NotNull PostponedTasks taskList,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        ClassKind kind = getClassKind(psiClass);
        ClassPsiDeclarationProvider classData = semanticServices.getPsiDeclarationProviderFactory().createBinaryClassData(psiClass);
        ClassDescriptorFromJvmBytecode classDescriptor = new ClassDescriptorFromJvmBytecode(
                containingDeclaration, kind, isInnerClass(psiClass));

        cache(javaClassToKotlinFqName(fqName), classDescriptor);
        classDescriptor.setName(Name.identifier(psiClass.getName()));

        List<JavaSignatureResolver.TypeParameterDescriptorInitialization> typeParameterDescriptorInitializations
                = JavaSignatureResolver.createUninitializedClassTypeParameters(psiClass, classDescriptor);

        classDescriptor.setTypeParameterDescriptors(getTypeParametersDescriptors(typeParameterDescriptorInitializations));
        List<JetType> supertypes = Lists.newArrayList();
        classDescriptor.setSupertypes(supertypes);
        classDescriptor.setVisibility(DescriptorResolverUtils.resolveVisibility(psiClass));
        classDescriptor.setModality(resolveModality(psiClass, classDescriptor));
        classDescriptor.createTypeConstructor();
        JavaClassNonStaticMembersScope membersScope = new JavaClassNonStaticMembersScope(classDescriptor, classData, semanticServices);
        classDescriptor.setScopeForMemberLookup(membersScope);
        classDescriptor.setScopeForConstructorResolve(membersScope);

        String context = "class " + psiClass.getQualifiedName();
        signatureResolver.initializeTypeParameters(typeParameterDescriptorInitializations, classDescriptor, context);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        List<TypeParameterDescriptor> classTypeParameters = classDescriptor.getTypeConstructor().getParameters();
        supertypes.addAll(supertypesResolver.getSupertypes(classDescriptor, new PsiClassWrapper(psiClass), classTypeParameters));

        if (psiClass.isEnum()) {
            ClassDescriptorFromJvmBytecode classObjectDescriptor = createClassObjectDescriptorForEnum(classDescriptor, psiClass);
            cache(DescriptorResolverUtils.getFqNameForClassObject(psiClass), classObjectDescriptor);
            classDescriptor.getBuilder().setClassObjectDescriptor(classObjectDescriptor);
        }

        classDescriptor.setAnnotations(annotationResolver.resolveAnnotations(psiClass, taskList));

        trace.record(BindingContext.CLASS, psiClass, classDescriptor);

        PsiMethod samInterfaceMethod = MembersCache.getSamInterfaceMethod(psiClass);
        if (samInterfaceMethod != null) {
            SimpleFunctionDescriptor abstractMethod = resolveFunctionOfSamInterface(samInterfaceMethod, classDescriptor);
            classDescriptor.setFunctionTypeForSamInterface(SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod));
        }

        return classDescriptor;
    }

    @NotNull
    private SimpleFunctionDescriptor resolveFunctionOfSamInterface(
            @NotNull PsiMethod samInterfaceMethod,
            @NotNull ClassDescriptorFromJvmBytecode samInterface
    ) {
        PsiClass methodContainer = samInterfaceMethod.getContainingClass();
        assert methodContainer != null : "method container is null for " + samInterfaceMethod;
        String containerQualifiedName = methodContainer.getQualifiedName();
        assert containerQualifiedName != null : "qualified name is null for " + methodContainer;

        if (DescriptorUtils.getFQName(samInterface).asString().equals(containerQualifiedName)) {
            SimpleFunctionDescriptor abstractMethod =
                    functionResolver.resolveFunctionMutely(new PsiMethodWrapper(samInterfaceMethod), samInterface);
            assert abstractMethod != null : "couldn't resolve method " + samInterfaceMethod;
            return abstractMethod;
        }
        else {
            return findFunctionWithMostSpecificReturnType(TypeUtils.getAllSupertypes(samInterface.getDefaultType()));
        }
    }

    private static SimpleFunctionDescriptor findFunctionWithMostSpecificReturnType(@NotNull Set<JetType> supertypes) {
        List<SimpleFunctionDescriptor> candidates = Lists.newArrayList();
        for (JetType supertype : supertypes) {
            List<CallableMemberDescriptor> abstractMembers = SingleAbstractMethodUtils.getAbstractMembers(supertype);
            if (!abstractMembers.isEmpty()) {
                candidates.add((SimpleFunctionDescriptor) abstractMembers.get(0));
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Couldn't find abstract method in supertypes " + supertypes);
        }
        SimpleFunctionDescriptor currentMostSpecificType = candidates.get(0);
        for (SimpleFunctionDescriptor candidate : candidates) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(candidate.getReturnType(), currentMostSpecificType.getReturnType())) {
                currentMostSpecificType = candidate;
            }
        }
        return currentMostSpecificType;
    }

    private void cache(@NotNull FqNameBase fqName, @Nullable ClassDescriptor classDescriptor) {
        if (classDescriptor == null) {
            cacheNegativeValue(fqName);
        }
        else {
            ClassDescriptor oldValue = classDescriptorCache.put(fqName, classDescriptor);
            assert oldValue == null;
        }
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

    void checkFqNamesAreConsistent(@NotNull PsiClass psiClass, @NotNull FqName desiredFqName) {
        String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;

        FqName fqName = new FqName(qualifiedName);
        assert fqName.equals(desiredFqName);
        FqNameUnsafe correctedName = javaClassToKotlinFqName(fqName);
        if (classDescriptorCache.containsKey(correctedName) || unresolvedCache.contains(correctedName)) {
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
        String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;
        return new FqName(qualifiedName);
    }

    // This method replaces "object" segments of FQ name to "<class-object-for-...>"
    @NotNull
    private static FqNameUnsafe javaClassToKotlinFqName(@NotNull FqName rawFqName) {
        List<Name> correctedSegments = new ArrayList<Name>();
        for (Name segment : rawFqName.pathSegments()) {
            if (JvmAbi.CLASS_OBJECT_CLASS_NAME.equals(segment.asString())) {
                assert !correctedSegments.isEmpty();
                Name previous = correctedSegments.get(correctedSegments.size() - 1);
                correctedSegments.add(DescriptorUtils.getClassObjectName(previous));
            }
            else {
                correctedSegments.add(segment);
            }
        }
        return new FqNameUnsafe(StringUtil.join(correctedSegments, "."));
    }

    private static boolean isContainedInClass(@NotNull PsiClass psiClass) {
        return psiClass.getContainingClass() != null;
    }

    private static boolean isInnerClass(@NotNull PsiClass psiClass) {
        return isContainedInClass(psiClass) && !psiClass.hasModifierProperty(PsiModifier.STATIC);
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
    private static ClassKind getClassKind(@NotNull PsiClass psiClass) {
        if (psiClass.isInterface()) {
            return (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT);
        }
        if (psiClass.isEnum()) {
            return ClassKind.ENUM_CLASS;
        }
        return ClassKind.CLASS;
    }

    @Nullable
    public ClassDescriptor resolveClass(FqName name) {
        return resolveClass(name, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    @NotNull
    private ClassDescriptorFromJvmBytecode createClassObjectDescriptorForEnum(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        ClassDescriptorFromJvmBytecode classObjectDescriptor = createSyntheticClassObject(containing, psiClass);

        classObjectDescriptor.getBuilder().addFunctionDescriptor(createEnumClassObjectValuesMethod(classObjectDescriptor, trace));
        classObjectDescriptor.getBuilder().addFunctionDescriptor(createEnumClassObjectValueOfMethod(classObjectDescriptor, trace));

        return classObjectDescriptor;
    }

    @NotNull
    private ClassDescriptorFromJvmBytecode createSyntheticClassObject(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        ClassDescriptorFromJvmBytecode classObjectDescriptor =
                new ClassDescriptorFromJvmBytecode(containing, ClassKind.CLASS_OBJECT, false);
        ClassPsiDeclarationProvider data =
                semanticServices.getPsiDeclarationProviderFactory().createSyntheticClassObjectClassData(psiClass);
        setUpClassObjectDescriptor(classObjectDescriptor, containing, data, getClassObjectName(containing.getName().asString()));
        return classObjectDescriptor;
    }

    private void setUpClassObjectDescriptor(
            @NotNull ClassDescriptorFromJvmBytecode classObjectDescriptor,
            @NotNull ClassDescriptor containing,
            @NotNull ClassPsiDeclarationProvider data,
            @NotNull Name classObjectName
    ) {
        classObjectDescriptor.setName(classObjectName);
        classObjectDescriptor.setModality(Modality.FINAL);
        classObjectDescriptor.setVisibility(containing.getVisibility());
        classObjectDescriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classObjectDescriptor.createTypeConstructor();
        JavaClassNonStaticMembersScope classMembersScope =
                new JavaClassNonStaticMembersScope(classObjectDescriptor, data, semanticServices);
        WritableScopeImpl writableScope =
                new WritableScopeImpl(classMembersScope, classObjectDescriptor, RedeclarationHandler.THROW_EXCEPTION,
                                      "Member lookup scope");
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        classObjectDescriptor.setScopeForMemberLookup(writableScope);
        classObjectDescriptor.setScopeForConstructorResolve(classMembersScope);
    }
}
