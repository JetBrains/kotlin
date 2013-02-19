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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

public class NamespaceFactoryImpl implements NamespaceFactory {

    private ModuleDescriptor moduleDescriptor;
    private BindingTrace trace;
    private ModuleConfiguration configuration;

    @Inject
    public void setModuleDescriptor(ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setConfiguration(ModuleConfiguration configuration) {
        this.configuration = configuration;
    }

    @NotNull
    public NamespaceDescriptorImpl createNamespaceDescriptorPathIfNeeded(@NotNull JetFile file,
                                                                         @NotNull JetScope outerScope,
                                                                         @NotNull RedeclarationHandler handler) {
        JetNamespaceHeader namespaceHeader = file.getNamespaceHeader();

        if (moduleDescriptor.getRootNamespaceDescriptorImpl() == null) {
            createRootNamespaceDescriptorIfNeeded(null, moduleDescriptor, null, handler);
        }

        NamespaceDescriptorImpl currentOwner = moduleDescriptor.getRootNamespaceDescriptorImpl();
        if (currentOwner == null) {
            throw new IllegalStateException("must be initialized 5 lines above");
        }

        for (JetSimpleNameExpression nameExpression : namespaceHeader.getParentNamespaceNames()) {
            Name namespaceName = Name.identifier(nameExpression.getReferencedName());

            NamespaceDescriptorImpl namespaceDescriptor = createNamespaceDescriptorIfNeeded(
                    null, currentOwner, namespaceName, nameExpression, handler);

            trace.record(BindingContext.NAMESPACE_IS_SRC, namespaceDescriptor, true);
            trace.record(RESOLUTION_SCOPE, nameExpression, outerScope);

            outerScope = namespaceDescriptor.getMemberScope();
            currentOwner = namespaceDescriptor;
        }

        NamespaceDescriptorImpl namespaceDescriptor;
        Name name;
        if (namespaceHeader.isRoot()) {
            // previous call to createRootNamespaceDescriptorIfNeeded couldn't store occurrence for current file.
            namespaceDescriptor = moduleDescriptor.getRootNamespaceDescriptorImpl();
            storeBindingForFileAndExpression(file, null, namespaceDescriptor);
        }
        else {
            name = namespaceHeader.getNameAsName();
            namespaceDescriptor = createNamespaceDescriptorIfNeeded(
                    file, currentOwner, name, namespaceHeader.getLastPartExpression(), handler);

            trace.record(RESOLUTION_SCOPE, namespaceHeader, outerScope);
        }
        trace.record(BindingContext.NAMESPACE_IS_SRC, namespaceDescriptor, true);

        return namespaceDescriptor;
    }

    @Override
    @NotNull
    public NamespaceDescriptorImpl createNamespaceDescriptorPathIfNeeded(@NotNull FqName fqName) {
        NamespaceDescriptorImpl owner = null;
        for (FqName pathElement : fqName.path()) {
            if (pathElement.isRoot()) {
                owner = createRootNamespaceDescriptorIfNeeded(null,
                                                              moduleDescriptor,
                                                              null,
                                                              RedeclarationHandler.DO_NOTHING);
            }
            else {
                assert owner != null : "Should never be null as first element in the path must be root";
                owner = createNamespaceDescriptorIfNeeded(null,
                                                          owner,
                                                          pathElement.shortName(),
                                                          null,
                                                          RedeclarationHandler.DO_NOTHING);
            }

        }

        assert owner != null : "Should never be null as first element in the path must be root";
        return owner;
    }

    private NamespaceDescriptorImpl createRootNamespaceDescriptorIfNeeded(@Nullable JetFile file,
                                                                          @NotNull ModuleDescriptor owner,
                                                                          @Nullable JetReferenceExpression expression,
                                                                          @NotNull RedeclarationHandler handler) {
        FqName fqName = FqName.ROOT;
        NamespaceDescriptorImpl namespaceDescriptor = owner.getRootNamespaceDescriptorImpl();

        if (namespaceDescriptor == null) {
            namespaceDescriptor = createNewNamespaceDescriptor(owner, FqNameUnsafe.ROOT_NAME, expression, handler, fqName);
        }

        storeBindingForFileAndExpression(file, expression, namespaceDescriptor);

        return namespaceDescriptor;
    }

    @NotNull
    private NamespaceDescriptorImpl createNamespaceDescriptorIfNeeded(@Nullable JetFile file,
                                                                      @NotNull NamespaceDescriptorImpl owner,
                                                                      @NotNull Name name,
                                                                      @Nullable JetReferenceExpression expression,
                                                                      @NotNull RedeclarationHandler handler) {
        FqName ownerFqName = DescriptorUtils.getFQName(owner).toSafe();
        FqName fqName = ownerFqName.child(name);
        // !!!
        NamespaceDescriptorImpl namespaceDescriptor = (NamespaceDescriptorImpl) owner.getMemberScope().getDeclaredNamespace(name);

        if (namespaceDescriptor == null) {
            namespaceDescriptor = createNewNamespaceDescriptor(owner, name, expression, handler, fqName);
        }

        storeBindingForFileAndExpression(file, expression, namespaceDescriptor);

        return namespaceDescriptor;
    }

    private NamespaceDescriptorImpl createNewNamespaceDescriptor(NamespaceDescriptorParent owner,
                                                                 Name name,
                                                                 PsiElement expression,
                                                                 RedeclarationHandler handler,
                                                                 FqName fqName) {
        NamespaceDescriptorImpl namespaceDescriptor;
        namespaceDescriptor = new NamespaceDescriptorImpl(
                owner,
                Collections.<AnnotationDescriptor>emptyList(), // TODO: annotations
                name
        );
        trace.record(FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName, namespaceDescriptor);

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, handler, "Namespace member scope");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        namespaceDescriptor.initialize(scope);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        //
        configuration.extendNamespaceScope(trace, namespaceDescriptor, scope);
        owner.addNamespace(namespaceDescriptor);
        if (expression != null) {
            trace.record(BindingContext.NAMESPACE, expression, namespaceDescriptor);
        }
        return namespaceDescriptor;
    }

    private void storeBindingForFileAndExpression(@Nullable JetFile file,
                                                  @Nullable JetReferenceExpression expression,
                                                  @NotNull NamespaceDescriptor namespaceDescriptor) {
        if (expression != null) {
            trace.record(REFERENCE_TARGET, expression, namespaceDescriptor);
        }

        if (file != null) {
            trace.record(BindingContext.FILE_TO_NAMESPACE, file, namespaceDescriptor);

            // Register files corresponding to this namespace
            // The trace currently does not support bi-di multimaps that would handle this task nicer
            Collection<JetFile> files = trace.get(NAMESPACE_TO_FILES, namespaceDescriptor);
            if (files == null) {
                files = Sets.newIdentityHashSet();
            }
            files.add(file);
            trace.record(BindingContext.NAMESPACE_TO_FILES, namespaceDescriptor, files);
        }
    }
}
