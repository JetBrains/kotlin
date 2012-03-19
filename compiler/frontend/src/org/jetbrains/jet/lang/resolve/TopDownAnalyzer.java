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
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeAdapter;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

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
    @NotNull
    private TopDownAnalysisParameters topDownAnalysisParameters;
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ModuleConfiguration configuration;
    @NotNull
    private ModuleDescriptor moduleDescriptor;
    @NotNull
    private NamespaceFactoryImpl namespaceFactory;

    private BodyResolver bodyResolver;
    private ControlFlowAnalyzer controlFlowAnalyzer;
    private DeclarationsChecker declarationsChecker;

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

    @Inject
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setConfiguration(@NotNull ModuleConfiguration configuration) {
        this.configuration = configuration;
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
    public void setBodyResolver(BodyResolver bodyResolver) {
        this.bodyResolver = bodyResolver;
    }

    @Inject
    public void setControlFlowAnalyzer(ControlFlowAnalyzer controlFlowAnalyzer) {
        this.controlFlowAnalyzer = controlFlowAnalyzer;
    }

    @Inject
    public void setDeclarationsChecker(DeclarationsChecker declarationsChecker) {
        this.declarationsChecker = declarationsChecker;
    }




    public static void process(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull NamespaceLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations,
            @NotNull Predicate<PsiFile> analyzeCompletely,
            @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory,
            @NotNull ModuleConfiguration configuration,
            boolean declaredLocally) {

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(analyzeCompletely, false, declaredLocally, trace);

        InjectorForTopDownAnalyzer injector = new InjectorForTopDownAnalyzer(project, topDownAnalysisParameters, configuration, moduleDescriptor, flowDataTraceFactory);

        injector.getTopDownAnalyzer().doProcess(outerScope, owner, declarations);

    }

    public void doProcess(
            JetScope outerScope,
            NamespaceLikeBuilder owner,
            Collection<? extends PsiElement> declarations) {
//        context.enableDebugOutput();
        context.debug("Enter");

        typeHierarchyResolver.process(outerScope, owner, declarations);
        declarationResolver.process();
        delegationResolver.process();
        overrideResolver.process();

        lockScopes();

        overloadResolver.process();

        if (!topDownAnalysisParameters.isAnalyzingBootstrapLibrary()) {
            bodyResolver.resolveBehaviorDeclarationBodies();
            controlFlowAnalyzer.process();
            declarationsChecker.process();
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

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(Predicates.<PsiFile>alwaysFalse(), true, false, trace);
        InjectorForTopDownAnalyzer injector = new InjectorForTopDownAnalyzer(
                project, topDownAnalysisParameters, ModuleConfiguration.EMPTY,
                JetStandardClasses.FAKE_STANDARD_CLASSES_MODULE, null);

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

        doProcess(outerScope, standardLibraryNamespace, toAnalyze);
    }

    public static void processObject(
            @NotNull Project project,
            @NotNull final BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetObjectDeclaration object) {

        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("<dummy for objec>");

        process(project, trace, outerScope, moduleDescriptor, new NamespaceLikeBuilder() {

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
        }, Collections.<PsiElement>singletonList(object), Predicates.equalTo(object.getContainingFile()), JetControlFlowDataTraceFactory.EMPTY, ModuleConfiguration.EMPTY, true);
    }


    public static void doAnalyzeFilesWithGivenTrace(Project project, final ModuleConfiguration configuration, Collection<JetFile> files, Predicate<PsiFile> filesToAnalyzeCompletely, JetControlFlowDataTraceFactory flowDataTraceFactory, BindingTraceContext bindingTraceContext) {
        final ModuleDescriptor owner = new ModuleDescriptor("<module>");

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                filesToAnalyzeCompletely, false, false, bindingTraceContext);

        InjectorForTopDownAnalyzer injector = new InjectorForTopDownAnalyzer(
                project, topDownAnalysisParameters, configuration, owner, flowDataTraceFactory);


        injector.getTopDownAnalyzer().doAnalyzeFilesWithGivenTrance2(files);
    }

    private void doAnalyzeFilesWithGivenTrance2(Collection<JetFile> files) {
        final WritableScope scope = new WritableScopeImpl(
                JetScope.EMPTY, moduleDescriptor,
                new TraceBasedRedeclarationHandler(topDownAnalysisParameters.getTrace())).setDebugName("Root scope in analyzeNamespace");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        NamespaceDescriptorImpl rootNs = namespaceFactory.createNamespaceDescriptorPathIfNeeded(FqName.ROOT);

        // map "jet" namespace into JetStandardLibrary/Classes
        // @see DefaultModuleConfiguraiton#extendNamespaceScope
        namespaceFactory.createNamespaceDescriptorPathIfNeeded(JetStandardClasses.STANDARD_CLASSES_FQNAME);

        // Import a scope that contains all top-level namespaces that come from dependencies
        // This makes the namespaces visible at all, does not import themselves
        scope.importScope(rootNs.getMemberScope());
        
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        // dummy builder is used because "root" is module descriptor,
        // namespaces added to module explicitly in
        doProcess(scope, new NamespaceLikeBuilderDummy(), files);
    }


}


