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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightClass;
import org.jetbrains.jet.asJava.LightClassConstructionContext;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.KotlinLightClassResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;

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
public class CliLightClassGenerationSupport extends LightClassGenerationSupport {

    public static CliLightClassGenerationSupport getInstanceForCli(@NotNull Project project) {
        return ServiceManager.getService(project, CliLightClassGenerationSupport.class);
    }

    private BindingTrace trace;
    private final CliIndexManager cliIndexManager;

    public CliLightClassGenerationSupport(@NotNull Project project) {
        this.cliIndexManager = CliIndexManager.getInstance(project);
    }

    @NotNull
    public BindingTrace getTrace() {
        if (trace == null) {
            trace = new BindingTraceContext();
        }
        return trace;
    }

    public void newBindingTrace() {
        assert ApplicationManager.getApplication().isUnitTestMode() : "Mutating project service's state shouldn't happen other than in tests";
        trace = null;
    }

    @NotNull
    @Override
    public LightClassConstructionContext analyzeRelevantCode(@NotNull Collection<JetFile> files) {
        return new LightClassConstructionContext(getTrace().getBindingContext(), null);
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        ClassDescriptor classDescriptor = getTrace().get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqName);
        if (classDescriptor != null) {
            PsiElement element = BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
            if (element != null && PsiSearchScopeUtil.isInScope(searchScope, element)) {
                return Collections.singletonList((JetClassOrObject) element);
            }
        }

        if (JvmAbi.isClassObjectFqName(fqName)) {
            Collection<JetClassOrObject> parentClasses = findClassOrObjectDeclarations(fqName.parent(), searchScope);
            return ContainerUtil.mapNotNull(parentClasses,
                                            new Function<JetClassOrObject, JetClassOrObject>() {
                                                @Override
                                                public JetClassOrObject fun(JetClassOrObject classOrObject) {
                                                    if (classOrObject instanceof JetClass) {
                                                        JetClass jetClass = (JetClass) classOrObject;
                                                        JetClassObject classObject = jetClass.getClassObject();
                                                        if (classObject != null) {
                                                            return classObject.getObjectDeclaration();
                                                        }
                                                    }
                                                    return null;
                                                }
                                            });
        }

        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull FqName fqName, @NotNull final GlobalSearchScope searchScope) {
        return Collections2.filter(cliIndexManager.getPackageSources(fqName),
                                   new Predicate<JetFile>() {
                                       @Override
                                       public boolean apply(JetFile file) {
                                           return PsiSearchScopeUtil.isInScope(searchScope, file);
                                       }
                                   });
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
        return cliIndexManager.packageExists(fqName);
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope) {
        return cliIndexManager.getSubpackagesOf(fqn);
    }

    @NotNull
    public KotlinLightClassResolver getLightClassResolver() {
        return new KotlinLightClassResolver() {
            @Nullable
            @Override
            public ClassDescriptor resolveLightClass(@NotNull PsiClass kotlinLightClass) {
                if (kotlinLightClass instanceof KotlinLightClass) {
                    KotlinLightClass lightClass = (KotlinLightClass) kotlinLightClass;
                    return trace.get(BindingContext.CLASS, lightClass.getSourceElement());
                }
                return null;
            }
        };
    }
}
