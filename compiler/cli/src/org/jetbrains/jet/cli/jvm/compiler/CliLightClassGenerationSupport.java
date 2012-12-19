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

package org.jetbrains.jet.cli.jvm.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassConstructionContext;
import org.jetbrains.jet.asJava.LightClassGenerationSupport;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
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

    public CliLightClassGenerationSupport() {
    }

    @NotNull
    public BindingTrace getTrace() {
        if (trace == null) {
            trace = new BindingTraceContext();
        }
        return trace;
    }

    public void clearBindingTrace() {
        assert ApplicationManager.getApplication().isUnitTestMode() : "Mutating project service's state shouldn't happen other than in tests";
        trace = null;
    }

    @NotNull
    @Override
    public LightClassConstructionContext analyzeRelevantCode(@NotNull JetFile file) {
        return new LightClassConstructionContext(getTrace().getBindingContext(), null);
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        ClassDescriptor classDescriptor = trace.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqName);
        if (classDescriptor != null) {
            PsiElement element = BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
            if (element != null && PsiSearchScopeUtil.isInScope(searchScope, element)) {
                return Collections.singletonList((JetClassOrObject) element);
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull FqName fqName, @NotNull final GlobalSearchScope searchScope) {
        NamespaceDescriptor namespaceDescriptor = trace.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName);
        if (namespaceDescriptor != null) {
            Collection<JetFile> files = trace.get(BindingContext.NAMESPACE_TO_FILES, namespaceDescriptor);
            if (files != null) {
                return Collections2.filter(files, new Predicate<JetFile>() {
                    @Override
                    public boolean apply(JetFile input) {
                        return PsiSearchScopeUtil.isInScope(searchScope, input);
                    }
                });
            }
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
    public boolean packageExists(
            @NotNull FqName fqName, @NotNull GlobalSearchScope scope
    ) {
        return trace.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName) != null;
    }
}
