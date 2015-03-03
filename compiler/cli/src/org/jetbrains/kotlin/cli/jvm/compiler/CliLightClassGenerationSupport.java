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

package org.jetbrains.kotlin.cli.jvm.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage;
import org.jetbrains.kotlin.asJava.LightClassConstructionContext;
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class solves the problem of interdependency between analyzing Kotlin code and generating JetLightClasses
 *
 * Consider the following example:
 *
 * KClass.kt refers to JClass.java and vice versa
 *
 * To analyze KClass.kt we need to load descriptors from JClass.java, and to do that we need a JetLightClass instance for KClass,
 * which can only be constructed when the structure of KClass is known.
 *
 * To mitigate this, CliLightClassGenerationSupport hold a trace that is shared between the analyzer and JetLightClasses
 */
public class CliLightClassGenerationSupport extends LightClassGenerationSupport implements CodeAnalyzerInitializer {
    private final PsiManager psiManager;
    private BindingContext bindingContext = null;
    private ModuleDescriptor module = null;

    public CliLightClassGenerationSupport(@NotNull Project project) {
        this.psiManager = PsiManager.getInstance(project);
    }

    @Override
    public void initialize(@NotNull BindingTrace trace, @NotNull ModuleDescriptor module, @Nullable KotlinCodeAnalyzer analyzer) {
        this.bindingContext = trace.getBindingContext();
        this.module = module;

        if (!(trace instanceof CliBindingTrace)) {
            throw new IllegalArgumentException("Shared trace is expected to be subclass of " + CliBindingTrace.class.getSimpleName() + " class");
        }

        ((CliBindingTrace) trace).setKotlinCodeAnalyzer(analyzer);
    }

    @NotNull
    private BindingContext getBindingContext() {
        assert bindingContext != null : "Call initialize() first";
        return bindingContext;
    }

    @NotNull
    private ModuleDescriptor getModule() {
        assert module != null : "Call initialize() first";
        return module;
    }

    @NotNull
    @Override
    public LightClassConstructionContext getContextForPackage(@NotNull Collection<JetFile> files) {
        return getContext();
    }

    @NotNull
    @Override
    public LightClassConstructionContext getContextForClassOrObject(@NotNull JetClassOrObject classOrObject) {
        return getContext();
    }

    @NotNull
    private LightClassConstructionContext getContext() {
        return new LightClassConstructionContext(bindingContext, getModule());
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull final GlobalSearchScope searchScope) {
        Collection<ClassDescriptor> classDescriptors = ResolveSessionUtils.getClassDescriptorsByFqName(getModule(), fqName);

        return ContainerUtil.mapNotNull(classDescriptors, new Function<ClassDescriptor, JetClassOrObject>() {
            @Override
            public JetClassOrObject fun(ClassDescriptor descriptor) {
                PsiElement element = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor);
                if (element instanceof JetClassOrObject && PsiSearchScopeUtil.isInScope(searchScope, element)) {
                    return (JetClassOrObject) element;
                }
                return null;
            }
        });
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull FqName fqName, @NotNull final GlobalSearchScope searchScope) {
        Collection<JetFile> files = getBindingContext().get(BindingContext.PACKAGE_TO_FILES, fqName);
        if (files != null) {
            return Collections2.filter(files, new Predicate<JetFile>() {
                @Override
                public boolean apply(JetFile input) {
                    return PsiSearchScopeUtil.isInScope(searchScope, input);
                }
            });
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarationsInPackage(
            @NotNull FqName packageFqName, @NotNull GlobalSearchScope searchScope
    ) {
        Collection<JetFile> files = findFilesForPackage(packageFqName, searchScope);
        List<JetClassOrObject> result = new SmartList<JetClassOrObject>();
        for (JetFile file : files) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetClassOrObject) {
                    result.add((JetClassOrObject) declaration);
                }
            }
        }
        return result;
    }

    @Override
    public boolean packageExists(@NotNull FqName fqName, @NotNull GlobalSearchScope scope) {
        return getModule().getPackage(fqName) != null;
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope) {
        PackageViewDescriptor packageView = getModule().getPackage(fqn);
        if (packageView == null) return Collections.emptyList();

        Collection<DeclarationDescriptor> members = packageView.getMemberScope().getDescriptors(DescriptorKindFilter.PACKAGES, JetScope.ALL_NAME_FILTER);
        return ContainerUtil.mapNotNull(members, new Function<DeclarationDescriptor, FqName>() {
            @Override
            public FqName fun(DeclarationDescriptor member) {
                if (member instanceof PackageViewDescriptor) {
                    return ((PackageViewDescriptor) member).getFqName();
                }
                return null;
            }
        });
    }

    @Nullable
    @Override
    public PsiClass getPsiClass(@NotNull JetClassOrObject classOrObject) {
        return KotlinLightClassForExplicitDeclaration.create(psiManager, classOrObject);
    }

    @NotNull
    @Override
    public Collection<PsiClass> getPackageClasses(@NotNull FqName packageFqName, @NotNull GlobalSearchScope scope) {
        Collection<JetFile> filesInPackage = findFilesForPackage(packageFqName, scope);

        if (PackagePartClassUtils.getPackageFilesWithCallables(filesInPackage).isEmpty()) return Collections.emptyList();

        //noinspection RedundantTypeArguments
        return UtilsPackage.<PsiClass>emptyOrSingletonList(KotlinLightClassForPackage.Factory.create(psiManager, packageFqName, scope, filesInPackage));
    }

    @NotNull
    @Override
    public BindingTraceContext createTrace() {
        return new NoScopeRecordCliBindingTrace();
    }

    public static class NoScopeRecordCliBindingTrace extends CliBindingTrace {
        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            if (slice == BindingContext.RESOLUTION_SCOPE || slice == BindingContext.TYPE_RESOLUTION_SCOPE) {
                // In the compiler there's no need to keep scopes
                return;
            }
            super.record(slice, key, value);
        }

        @Override
        public String toString() {
            return NoScopeRecordCliBindingTrace.class.getName();
        }
    }

    public static class CliBindingTrace extends BindingTraceContext {
        private KotlinCodeAnalyzer kotlinCodeAnalyzer;

        @TestOnly
        public CliBindingTrace() {
        }

        @Override
        public String toString() {
            return CliBindingTrace.class.getName();
        }

        public void setKotlinCodeAnalyzer(KotlinCodeAnalyzer kotlinCodeAnalyzer) {
            this.kotlinCodeAnalyzer = kotlinCodeAnalyzer;
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            V value = super.get(slice, key);

            if (value == null && TopDownAnalysisParameters.LAZY) {
                if (BindingContext.FUNCTION == slice || BindingContext.VARIABLE == slice) {
                    if (key instanceof JetDeclaration) {
                        JetDeclaration jetDeclaration = (JetDeclaration) key;
                        if (!JetPsiUtil.isLocal(jetDeclaration)) {
                            kotlinCodeAnalyzer.resolveToDescriptor(jetDeclaration);
                        }
                    }
                }

                return super.get(slice, key);
            }

            return value;
        }
    }
}
