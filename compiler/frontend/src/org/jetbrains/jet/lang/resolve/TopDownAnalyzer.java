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
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

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
    private BindingTrace trace;
    @NotNull
    private ModuleDescriptor moduleDescriptor;
    @NotNull
    private NamespaceFactoryImpl namespaceFactory;
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
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setNamespaceFactory(@NotNull NamespaceFactoryImpl namespaceFactory) {
        this.namespaceFactory = namespaceFactory;
    }

    @Inject
    public void setBodyResolver(@NotNull BodyResolver bodyResolver) {
        this.bodyResolver = bodyResolver;
    }



    public void doProcess(
            JetScope outerScope,
            NamespaceLikeBuilder owner,
            Collection<? extends PsiElement> declarations) {
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
        for (Map.Entry<JetFile, WritableScope> namespaceScope : context.getNamespaceScopes().entrySet()) {
            // todo: this is hack in favor of REPL
            if(!namespaceScope.getKey().isScript())
                namespaceScope.getValue().changeLockLevel(WritableScope.LockLevel.READING);
        }
    }

    public static void processStandardLibraryNamespace(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull WritableScope outerScope,
            @NotNull NamespaceDescriptorImpl standardLibraryNamespace,
            @NotNull List<JetFile> files) {

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                Predicates.<PsiFile>alwaysFalse(), true, false, Collections.<AnalyzerScriptParameter>emptyList());
        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                project, topDownAnalysisParameters, new ObservableBindingTrace(trace),       
                KotlinBuiltIns.getInstance().getBuiltInsModule(), ModuleConfiguration.EMPTY);

        injector.getTopDownAnalyzer().doProcessStandardLibraryNamespace(outerScope, standardLibraryNamespace, files);
    }

    private void doProcessStandardLibraryNamespace(
            WritableScope outerScope, NamespaceDescriptorImpl standardLibraryNamespace, List<JetFile> files) {
        ArrayList<JetDeclaration> toAnalyze = new ArrayList<JetDeclaration>();
        for(JetFile file : files) {
            context.getNamespaceDescriptors().put(file, standardLibraryNamespace);
            context.getNamespaceScopes().put(file, standardLibraryNamespace.getMemberScope());
            toAnalyze.addAll(file.getDeclarations());
        }
//        context.getDeclaringScopes().put(file, outerScope);

        doProcess(outerScope, standardLibraryNamespace.getBuilder(), toAnalyze);
    }

    public static void processClassOrObject(
            @NotNull Project project,
            @NotNull final BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject object
    ) {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor(Name.special("<dummy for object>"));

        TopDownAnalysisParameters topDownAnalysisParameters =
                new TopDownAnalysisParameters(Predicates.equalTo(object.getContainingFile()),
                false, true, Collections.<AnalyzerScriptParameter>emptyList());

        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                project, topDownAnalysisParameters, new ObservableBindingTrace(trace), moduleDescriptor,
                ModuleConfiguration.EMPTY);

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
            @NotNull List<AnalyzerScriptParameter> scriptParameters) {
        final WritableScope scope = new WritableScopeImpl(
                JetScope.EMPTY, moduleDescriptor,
                new TraceBasedRedeclarationHandler(trace), "Root scope in analyzeNamespace");

        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        NamespaceDescriptorImpl rootNs = namespaceFactory.createNamespaceDescriptorPathIfNeeded(FqName.ROOT);

        // map "jet" namespace into KotlinBuiltIns
        // @see DefaultModuleConfiguraiton#extendNamespaceScope
        namespaceFactory.createNamespaceDescriptorPathIfNeeded(KotlinBuiltIns.getInstance().getBuiltInsPackageFqName());

        // Import a scope that contains all top-level namespaces that come from dependencies
        // This makes the namespaces visible at all, does not import themselves
        scope.importScope(rootNs.getMemberScope());
        
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        // dummy builder is used because "root" is module descriptor,
        // namespaces added to module explicitly in
        doProcess(scope, new NamespaceLikeBuilderDummy(), files);
    }


    public void prepareForTheNextReplLine() {
        context.getScriptScopes().clear();
        context.getScripts().clear();
    }


}


