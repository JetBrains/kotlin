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

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerBasic;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.util.*;

public class TopDownAnalyzer {

    @NotNull
    private DeclarationResolver declarationResolver;
    @NotNull
    private TypeHierarchyResolver typeHierarchyResolver;
    @NotNull
    private OverrideResolver overrideResolver;
    @NotNull
    private OverloadResolver overloadResolver;
    @NotNull
    private TopDownAnalysisParameters topDownAnalysisParameters;
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private BodyResolver bodyResolver;

    @Inject
    public void setDeclarationResolver(@NotNull DeclarationResolver declarationResolver) {
        this.declarationResolver = declarationResolver;
    }

    @Inject
    public void setTypeHierarchyResolver(@NotNull TypeHierarchyResolver typeHierarchyResolver) {
        this.typeHierarchyResolver = typeHierarchyResolver;
    }

    @Inject
    public void setOverrideResolver(@NotNull OverrideResolver overrideResolver) {
        this.overrideResolver = overrideResolver;
    }

    @Inject
    public void setOverloadResolver(@NotNull OverloadResolver overloadResolver) {
        this.overloadResolver = overloadResolver;
    }

    @Inject
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setBodyResolver(@NotNull BodyResolver bodyResolver) {
        this.bodyResolver = bodyResolver;
    }



    public void doProcess(
            JetScope outerScope,
            NamespaceLikeBuilder owner,
            Collection<? extends PsiElement> declarations
    ) {
//        context.enableDebugOutput();
        context.debug("Enter");

        typeHierarchyResolver.process(outerScope, owner, declarations);
        declarationResolver.process(outerScope);
        overrideResolver.process();

        lockScopes();

        overloadResolver.process();

        if (!topDownAnalysisParameters.isAnalyzingBootstrapLibrary()) {
            bodyResolver.resolveBodies();
        }

        context.debug("Exit");
        context.printDebugOutput(System.out);
    }

    private void lockScopes() {
        for (MutableClassDescriptor mutableClassDescriptor : context.getClasses().values()) {
            mutableClassDescriptor.lockScopes();
        }
        for (MutableClassDescriptor mutableClassDescriptor : context.getObjects().values()) {
            mutableClassDescriptor.lockScopes();
        }
        for (Map.Entry<JetFile, WritableScope> namespaceScope : context.getFileScopes().entrySet()) {
            // todo: this is hack in favor of REPL
            if(!namespaceScope.getKey().isScript())
                namespaceScope.getValue().changeLockLevel(WritableScope.LockLevel.READING);
        }
    }

    public static void processStandardLibraryNamespace(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull ModuleSourcesManager moduleSourcesManager,
            @NotNull JetScope outerScope,
            @NotNull MutablePackageFragmentDescriptor standardLibraryPackageFragment,
            @NotNull List<JetFile> files) {

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysFalse(), true, false, Collections.<AnalyzerScriptParameter>emptyList());
        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                project, topDownAnalysisParameters, new ObservableBindingTrace(trace),       
                moduleSourcesManager);

        injector.getTopDownAnalyzer().doProcessStandardLibraryNamespace(outerScope, standardLibraryPackageFragment, files);
    }

    private void doProcessStandardLibraryNamespace(
            JetScope outerScope, MutablePackageFragmentDescriptor standardLibraryPackageFragment, List<JetFile> files) {
        List<JetDeclaration> toAnalyze = new ArrayList<JetDeclaration>();
        for(JetFile file : files) {
            context.getPackageFragmentDescriptors().put(file, standardLibraryPackageFragment);
            context.getFileScopes().put(file, standardLibraryPackageFragment.getMemberScope());
            toAnalyze.addAll(file.getDeclarations());
        }
//        context.getDeclaringScopes().put(file, outerScope);

        doProcess(outerScope, standardLibraryPackageFragment.getBuilder(), toAnalyze);
    }

    public static void processClassOrObject(
            @NotNull Project project,
            @NotNull ModuleSourcesManager moduleSourcesManager,
            @NotNull final BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject object
    ) {
        TopDownAnalysisParameters topDownAnalysisParameters =
                new TopDownAnalysisParameters(Predicates.equalTo(object.getContainingFile()),
                false, true, Collections.<AnalyzerScriptParameter>emptyList());

        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                project, topDownAnalysisParameters, new ObservableBindingTrace(trace),
                moduleSourcesManager);

        injector.getTopDownAnalyzer().doProcess(outerScope, new NamespaceLikeBuilder() {

            @NotNull
            @Override
            public DeclarationDescriptor getOwnerForChildren() {
                return containingDeclaration;
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
        }, Collections.<PsiElement>singletonList(object));
    }

    public void analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull List<AnalyzerScriptParameter> scriptParameters
    ) {

        // dummy builder is used because "root" is module descriptor,
        // namespaces added to module explicitly in TypeHierarchyResolver
        doProcess(JetScope.EMPTY, new NamespaceLikeBuilderDummy(), files);
    }


    public void prepareForTheNextReplLine() {
        context.getScriptScopes().clear();
        context.getScripts().clear();
    }


}


