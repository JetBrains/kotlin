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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.provider.*;
import org.jetbrains.jet.lang.resolve.java.scope.JavaBaseScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScopeWithoutMembers;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.INVALID_VERSION;

public final class JavaNamespaceResolver {

    @NotNull
    public static final ModuleDescriptor FAKE_ROOT_MODULE = new ModuleDescriptorImpl(JavaDescriptorResolver.JAVA_ROOT,
                                                                                     JavaBridgeConfiguration.ALL_JAVA_IMPORTS,
                                                                                     JavaToKotlinClassMap.getInstance());
    @NotNull
    private final Map<FqName, JetScope> resolvedNamespaceCache = Maps.newHashMap();
    @NotNull
    private final Set<FqName> unresolvedCache = Sets.newHashSet();

    private PsiClassFinder psiClassFinder;
    private BindingTrace trace;
    private JavaSemanticServices javaSemanticServices;

    private DeserializedDescriptorResolver deserializedDescriptorResolver;

    public JavaNamespaceResolver() {
    }

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setJavaSemanticServices(JavaSemanticServices javaSemanticServices) {
        this.javaSemanticServices = javaSemanticServices;
    }

    @Inject
    public void setDeserializedDescriptorResolver(DeserializedDescriptorResolver deserializedDescriptorResolver) {
        this.deserializedDescriptorResolver = deserializedDescriptorResolver;
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        // First, let's check that there is no Kotlin package:
        NamespaceDescriptor kotlinNamespaceDescriptor = trace.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, qualifiedName);
        if (kotlinNamespaceDescriptor != null) {
            return searchRule.processFoundInKotlin(kotlinNamespaceDescriptor);
        }

        if (unresolvedCache.contains(qualifiedName)) {
            return null;
        }
        JetScope scope = resolvedNamespaceCache.get(qualifiedName);
        if (scope != null) {
            return (NamespaceDescriptor) scope.getContainingDeclaration();
        }

        NamespaceDescriptorParent parentNs = resolveParentNamespace(qualifiedName);
        if (parentNs == null) {
            return null;
        }

        JavaNamespaceDescriptor javaNamespaceDescriptor = new JavaNamespaceDescriptor(
                parentNs,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                qualifiedName
        );

        JetScope newScope = createNamespaceScope(qualifiedName, javaNamespaceDescriptor);
        if (newScope == null) {
            return null;
        }

        if (newScope instanceof JavaBaseScope) {
            trace.record(BindingContext.NAMESPACE, ((JavaBaseScope) newScope).getPsiElement(), javaNamespaceDescriptor);
        }


        javaNamespaceDescriptor.setMemberScope(newScope);

        return javaNamespaceDescriptor;
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName) {
        return resolveNamespace(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    @Nullable
    private NamespaceDescriptorParent resolveParentNamespace(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            return FAKE_ROOT_MODULE;
        }
        else {
            return resolveNamespace(fqName.parent(), DescriptorSearchRule.INCLUDE_KOTLIN);
        }
    }

    @Nullable
    private JetScope createNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor
    ) {
        JetScope namespaceScope = doCreateNamespaceScope(fqName, namespaceDescriptor);
        cache(fqName, namespaceScope);
        return namespaceScope;
    }

    @Nullable
    private JetScope doCreateNamespaceScope(
            @NotNull FqName fqName,
            @NotNull NamespaceDescriptor namespaceDescriptor
    ) {
        PsiPackage psiPackage = psiClassFinder.findPsiPackage(fqName);
        if (psiPackage != null) {
            PsiClass psiClass = getPsiClassForJavaPackageScope(fqName);
            trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.PROPER);

            if (psiClass != null) {
                boolean isCompiledKotlinPackageClass = DescriptorResolverUtils.isCompiledKotlinPackageClass(psiClass);
                if (isOldKotlinPackageClass(psiClass) && !isCompiledKotlinPackageClass) {
                    // If psiClass has old annotations (@JetPackage) but doesn't have @KotlinPackage, report ABI version error
                    AbiVersionUtil.reportIncompatibleAbiVersion(psiClass, INVALID_VERSION, trace);
                }
                if (isCompiledKotlinPackageClass) {
                    // If psiClass has @KotlinPackage (regardless of whether it has @JetPackage or not), deserialize it to Kotlin descriptor.
                    // Note that @KotlinPackage may still have an old ABI version, in which case null is returned by createKotlinPackageScope
                    VirtualFile file = psiClass.getContainingFile().getVirtualFile();
                    if (file != null) {
                        JetScope kotlinPackageScope = deserializedDescriptorResolver.createKotlinPackageScope(namespaceDescriptor,
                                file, DescriptorResolverUtils.createPsiBasedErrorReporter(psiClass, trace));
                        if (kotlinPackageScope != null) {
                            return kotlinPackageScope;
                        }
                    }
                }
            }

            // Otherwise (if psiClass is null or doesn't have a supported Kotlin annotation), it's a Java class and the package is empty
            PackagePsiDeclarationProvider provider = new PackagePsiDeclarationProviderImpl(psiPackage, psiClassFinder);
            return new JavaPackageScopeWithoutMembers(namespaceDescriptor, provider, fqName, javaSemanticServices);
        }

        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
        if (psiClass == null) {
            return null;
        }
        if (DescriptorResolverUtils.isCompiledKotlinClassOrPackageClass(psiClass)) {
            return null;
        }
        if (!hasStaticMembers(psiClass)) {
            return null;
        }
        trace.record(JavaBindingContext.JAVA_NAMESPACE_KIND, namespaceDescriptor, JavaNamespaceKind.CLASS_STATICS);
        return new JavaClassStaticMembersScope(
                namespaceDescriptor,
                new ClassPsiDeclarationProviderImpl(psiClass, true, psiClassFinder),
                fqName, javaSemanticServices);
    }

    private static boolean isOldKotlinPackageClass(@NotNull PsiClass psiClass) {
        //noinspection deprecation
        return DescriptorResolverUtils.hasAnnotation(psiClass, JvmAnnotationNames.OLD_JET_PACKAGE_CLASS_ANNOTATION.getFqName());
    }

    private void cache(@NotNull FqName fqName, @Nullable JetScope packageScope) {
        if (packageScope == null) {
            unresolvedCache.add(fqName);
            return;
        }
        JetScope oldValue = resolvedNamespaceCache.put(fqName, packageScope);
        if (oldValue != null) {
            throw new IllegalStateException("rewrite at " + fqName);
        }
    }

    @Nullable
    public JetScope getJavaPackageScopeForExistingNamespaceDescriptor(@NotNull NamespaceDescriptor namespaceDescriptor) {
        FqName fqName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
        if (unresolvedCache.contains(fqName)) {
            throw new IllegalStateException(
                    "This means that we are trying to create a Java package, but have a package with the same FQN defined in Kotlin: " +
                    fqName);
        }
        JetScope alreadyResolvedScope = resolvedNamespaceCache.get(fqName);
        if (alreadyResolvedScope != null) {
            return alreadyResolvedScope;
        }
        return createNamespaceScope(fqName, namespaceDescriptor);
    }

    @Nullable
    private PsiClass getPsiClassForJavaPackageScope(@NotNull FqName packageFQN) {
        return psiClassFinder
                .findPsiClass(PackageClassUtils.getPackageClassFqName(packageFQN), PsiClassFinder.RuntimeClassesHandleMode.IGNORE);
    }

    private static boolean hasStaticMembers(@NotNull PsiClass psiClass) {
        for (PsiMember member : ContainerUtil.concat(psiClass.getMethods(), psiClass.getFields())) {
            if (member.hasModifierProperty(PsiModifier.STATIC) && !DescriptorResolverUtils.shouldBeInEnumClassObject(member)) {
                return true;
            }
        }

        for (PsiClass nestedClass : psiClass.getInnerClasses()) {
            if (MembersCache.isSamInterface(nestedClass)) {
                return true;
            }
            if (nestedClass.hasModifierProperty(PsiModifier.STATIC) && hasStaticMembers(nestedClass)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public Collection<Name> getClassNamesInPackage(@NotNull FqName packageName) {
        PsiPackage psiPackage = psiClassFinder.findPsiPackage(packageName);
        if (psiPackage == null) return Collections.emptyList();

        PsiClass[] classes = psiPackage.getClasses();
        List<Name> result = new ArrayList<Name>(classes.length);
        for (PsiClass psiClass : classes) {
            if (DescriptorResolverUtils.isCompiledKotlinClass(psiClass)) {
                result.add(Name.identifier(psiClass.getName()));
            }
        }

        return result;
    }
}