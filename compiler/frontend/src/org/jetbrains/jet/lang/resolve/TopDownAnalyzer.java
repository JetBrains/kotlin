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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    @NotNull
    private DeclarationResolver declarationResolver;
    @NotNull
    private TypeHierarchyResolver typeHierarchyResolver;
    @NotNull
    private DelegationResolver delegationResolver;
    @NotNull
    private OverrideResolver overrideResolver;
    @NotNull
    private OverloadResolver overloadResolver;


    @Inject
    public void setDeclarationResolver(@NotNull DeclarationResolver declarationResolver) {
        this.declarationResolver = declarationResolver;
    }

    @Inject
    public void setTypeHierarchyResolver(@NotNull TypeHierarchyResolver typeHierarchyResolver) {
        this.typeHierarchyResolver = typeHierarchyResolver;
    }

    @Inject
    public void setDelegationResolver(@NotNull DelegationResolver delegationResolver) {
        this.delegationResolver = delegationResolver;
    }

    @Inject
    public void setOverrideResolver(@NotNull OverrideResolver overrideResolver) {
        this.overrideResolver = overrideResolver;
    }

    @Inject
    public void setOverloadResolver(@NotNull OverloadResolver overloadResolver) {
        this.overloadResolver = overloadResolver;
    }



    public static void process(
            Project project, @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull NamespaceLike owner,
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> analyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull ModuleConfiguration configuration
    ) {
        process(project, trace, outerScope, owner, files, analyzeCompletely, flowDataTraceFactory, configuration, false);
    }

    private static void process(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull NamespaceLike owner,
            @NotNull Collection<? extends PsiElement> declarations,
            @NotNull Predicate<PsiFile> analyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull ModuleConfiguration configuration,
            boolean declaredLocally) {
        TopDownAnalysisContext context = new TopDownAnalysisContext(project, trace, analyzeCompletely, configuration, declaredLocally, false, flowDataTraceFactory);
        context.getInjector().getTopDownAnalyzer().doProcess(context, outerScope, owner, declarations);

    }

    private void doProcess(
            TopDownAnalysisContext context, JetScope outerScope,
            NamespaceLike owner,
            Collection<? extends PsiElement> declarations) {
//        context.enableDebugOutput();
        context.debug("Enter");

        typeHierarchyResolver.process(outerScope, owner, declarations);
        declarationResolver.process();
        delegationResolver.process();
        overrideResolver.process();

        lockScopes(context);

        overloadResolver.process();

        if (!context.analyzingBootstrapLibrary()) {
            context.getInjector().getBodyResolver().resolveBehaviorDeclarationBodies();
            context.getInjector().getControlFlowAnalyzer().process();
            context.getInjector().getDeclarationsChecker().process();
        }

        context.debug("Exit");
        context.printDebugOutput(System.out);
    }


    private static void lockScopes(TopDownAnalysisContext context) {
        for (MutableClassDescriptor mutableClassDescriptor : context.getClasses().values()) {
            mutableClassDescriptor.lockScopes();
        }
        for (MutableClassDescriptor mutableClassDescriptor : context.getObjects().values()) {
            mutableClassDescriptor.lockScopes();
        }
        for (WritableScope namespaceScope : context.getNamespaceScopes().values()) {
            namespaceScope.changeLockLevel(WritableScope.LockLevel.READING);
        }
    }

    public static void processStandardLibraryNamespace(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull WritableScope outerScope,
            @NotNull NamespaceDescriptorImpl standardLibraryNamespace,
            @NotNull List<JetFile> files) {
        TopDownAnalysisContext context = new TopDownAnalysisContext(project, trace, Predicates.<PsiFile>alwaysFalse(), ModuleConfiguration.EMPTY, false, true, null);
        ArrayList<JetDeclaration> toAnalyze = new ArrayList<JetDeclaration>();
        for(JetFile file : files) {
            context.getNamespaceDescriptors().put(file, standardLibraryNamespace);
            context.getNamespaceScopes().put(file, standardLibraryNamespace.getMemberScope());
            toAnalyze.addAll(file.getDeclarations());
        }
//        context.getDeclaringScopes().put(file, outerScope);

        context.getInjector().getTopDownAnalyzer().doProcess(context, outerScope, standardLibraryNamespace, toAnalyze);
    }

    public static void processObject(
            @NotNull Project project,
            @NotNull final BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetObjectDeclaration object) {
        process(project, trace, outerScope, new NamespaceLike() {

            @NotNull
            @Override
            public DeclarationDescriptor getOwnerForChildren() {
                return containingDeclaration;
            }

            @Override
            public NamespaceDescriptorImpl getNamespace(String name) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {

            }

            @Override
            public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {

            }

            @Override
            public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {

            }

            @Override
            public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptorLite classObjectDescriptor) {
                return ClassObjectStatus.NOT_ALLOWED;
            }
        }, Collections.<PsiElement>singletonList(object), Predicates.equalTo(object.getContainingFile()), JetControlFlowDataTraceFactory.EMPTY, ModuleConfiguration.EMPTY, true);
    }

}


