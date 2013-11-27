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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

public class NamespaceFactoryImpl implements NamespaceFactory {

    private ModuleDescriptorImpl module;
    private MutablePackageFragmentProvider packageFragmentProvider;
    private BindingTrace trace;

    @Inject
    public void setModule(ModuleDescriptorImpl module) {
        this.module = module;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setPackageFragmentProvider(MutablePackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @NotNull
    public MutablePackageFragmentDescriptor createPackageFragmentIfNeeded(@NotNull JetFile file, @NotNull RedeclarationHandler handler) {
        JetNamespaceHeader namespaceHeader = file.getNamespaceHeader();

        // TODO 2 make this code neater
        List<JetSimpleNameExpression> allNamespaceNames = new ArrayList<JetSimpleNameExpression>(namespaceHeader.getParentNamespaceNames());
        ContainerUtil.addIfNotNull(allNamespaceNames, namespaceHeader.getLastPartExpression());
        for (JetSimpleNameExpression nameExpression : allNamespaceNames) {
            FqName parentFqName = namespaceHeader.getParentFqName(nameExpression);
            getOrCreatePackageFragment(parentFqName, nameExpression, handler);

            trace.record(RESOLUTION_SCOPE, nameExpression, module.getPackage(parentFqName).getMemberScope());
        }

        MutablePackageFragmentDescriptor fragment = getOrCreatePackageFragment(
                namespaceHeader.getFqName(), namespaceHeader.getLastPartExpression(), handler);
        storeBindingForFile(file, fragment);
        return fragment;
    }

    @Override
    @NotNull
    public PackageFragmentDescriptor createNamespaceDescriptorPathIfNeeded(@NotNull FqName fqName) {
        return packageFragmentProvider.getOrCreateFragment(fqName);
    }

    @NotNull
    private MutablePackageFragmentDescriptor getOrCreatePackageFragment(
            @NotNull FqName fqName,
            @Nullable JetReferenceExpression expression,
            @NotNull RedeclarationHandler handler
    ) {
        // TODO 1 use handler
        MutablePackageFragmentDescriptor fragment = packageFragmentProvider.getOrCreateFragment(fqName);
        if (expression != null) {
            trace.record(REFERENCE_TARGET, expression, packageFragmentProvider.getModule().getPackage(fqName));
        }

        return fragment;
    }

    private void storeBindingForFile(@NotNull JetFile file, @NotNull PackageFragmentDescriptor fragment) {
        trace.record(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file, fragment);

        // Register files corresponding to this namespace
        // The trace currently does not support bi-di multimaps that would handle this task nicer
        FqName fqName = fragment.getFqName();
        Collection<JetFile> files = trace.get(PACKAGE_TO_FILES, fqName);
        if (files == null) {
            files = Sets.newIdentityHashSet();
        }
        files.add(file);
        trace.record(BindingContext.PACKAGE_TO_FILES, fqName, files);
    }
}
