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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.BindingContext.*;

/**
 * @author Stepan Koltsov
 */
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

    public NamespaceDescriptorImpl createNamespaceDescriptorPathIfNeeded(JetFile file, JetScope outerScope) {
        JetNamespaceHeader namespaceHeader = file.getNamespaceHeader();

        if (moduleDescriptor.getRootNs() == null) {
            createNamespaceDescriptorIfNeeded(null, moduleDescriptor, "<root>", true, null);
        }

        NamespaceDescriptorParent currentOwner = moduleDescriptor.getRootNs();
        if (currentOwner == null) {
            throw new IllegalStateException("must be initialized 5 lines above");
        }

        for (JetSimpleNameExpression nameExpression : namespaceHeader.getParentNamespaceNames()) {
            String namespaceName = JetPsiUtil.safeName(nameExpression.getReferencedName());

            NamespaceDescriptorImpl namespaceDescriptor = createNamespaceDescriptorIfNeeded(
                    null, currentOwner, namespaceName, false, nameExpression);

            trace.record(BindingContext.NAMESPACE_IS_SRC, namespaceDescriptor, true);
            trace.record(RESOLUTION_SCOPE, nameExpression, outerScope);

            outerScope = namespaceDescriptor.getMemberScope();
            currentOwner = namespaceDescriptor;
        }

        NamespaceDescriptorImpl namespaceDescriptor;
        String name;
        if (namespaceHeader.isRoot()) {
            // again to register file in trace
            namespaceDescriptor = createNamespaceDescriptorIfNeeded(file, moduleDescriptor, "<root>", true, null);
        }
        else {
            name = namespaceHeader.getName();
            namespaceDescriptor = createNamespaceDescriptorIfNeeded(file, currentOwner, name, namespaceHeader.isRoot(),
                    namespaceHeader.getLastPartExpression());

            trace.record(BindingContext.NAMESPACE_IS_SRC, namespaceDescriptor, true);
            trace.record(RESOLUTION_SCOPE, namespaceHeader, outerScope);
        }

        return namespaceDescriptor;
    }

    @Override
    @NotNull
    public NamespaceDescriptorImpl createNamespaceDescriptorPathIfNeeded(@NotNull FqName fqName) {
        NamespaceDescriptorParent owner = moduleDescriptor;
        for (FqName pathElement : fqName.path()) {
            owner = createNamespaceDescriptorIfNeeded(null,
                    owner, pathElement.isRoot() ? "<root>" : pathElement.shortName(), pathElement.isRoot(), null);
        }
        return (NamespaceDescriptorImpl) owner;
    }

    @NotNull
    public NamespaceDescriptorImpl createNamespaceDescriptorIfNeeded(@Nullable JetFile file,
            @NotNull NamespaceDescriptorParent owner, @NotNull String name, boolean root, @Nullable JetReferenceExpression expression) {
        FqName fqName;
        NamespaceDescriptorImpl namespaceDescriptor;
        if (root) {
            if (!(owner instanceof ModuleDescriptor)) {
                throw new IllegalStateException();
            }
            fqName = FqName.ROOT;
            namespaceDescriptor = ((ModuleDescriptor) owner).getRootNs();
        }
        else {
            FqName ownerFqName = DescriptorUtils.getFQName(owner).toSafe();
            fqName = ownerFqName.child(name);
            namespaceDescriptor = ((NamespaceDescriptorImpl) owner).getNamespace(name);
        }

        if (namespaceDescriptor == null) {
            namespaceDescriptor = new NamespaceDescriptorImpl(
                    owner,
                    Collections.<AnnotationDescriptor>emptyList(), // TODO: annotations
                    name
            );
            trace.record(FQNAME_TO_NAMESPACE_DESCRIPTOR, fqName, namespaceDescriptor);
            WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, RedeclarationHandler.DO_NOTHING).setDebugName("Namespace member scope");
            scope.changeLockLevel(WritableScope.LockLevel.BOTH);
            namespaceDescriptor.initialize(scope);
            configuration.extendNamespaceScope(trace, namespaceDescriptor, scope);
            owner.addNamespace(namespaceDescriptor);
            if (expression != null) {
                trace.record(BindingContext.NAMESPACE, expression, namespaceDescriptor);
            }
        }

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

        return namespaceDescriptor;
    }


}
