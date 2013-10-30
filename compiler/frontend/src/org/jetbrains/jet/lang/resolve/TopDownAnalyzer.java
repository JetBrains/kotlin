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
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
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
    private MutablePackageFragmentProvider packageFragmentProvider;
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
    public void setPackageFragmentProvider(@NotNull MutablePackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
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
                KotlinBuiltIns.getInstance().getBuiltInsModule(), PlatformToKotlinClassMap.EMPTY);

        injector.getTopDownAnalyzer().doProcessStandardLibraryNamespace(outerScope, standardLibraryNamespace, files);
    }

    private void doProcessStandardLibraryNamespace(
            WritableScope outerScope, NamespaceDescriptorImpl standardLibraryNamespace, List<JetFile> files) {
        ArrayList<JetDeclaration> toAnalyze = new ArrayList<JetDeclaration>();
        for (JetFile file : files) {
            // TODO 1 restore
            //context.getPackageFragments().put(file, standardLibraryNamespace);
            context.getNamespaceScopes().put(file, standardLibraryNamespace.getMemberScope());
            toAnalyze.addAll(file.getDeclarations());
        }
//        context.getDeclaringScopes().put(file, outerScope);

        doProcess(outerScope, standardLibraryNamespace.getBuilder(), toAnalyze);
    }

    public static void processClassOrObject(
            @NotNull ExpressionTypingContext context,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject object
    ) {
        ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(Name.special("<dummy for object>"),
                                                                         Collections.<ImportPath>emptyList(),
                                                                         PlatformToKotlinClassMap.EMPTY);

        TopDownAnalysisParameters topDownAnalysisParameters =
                new TopDownAnalysisParameters(Predicates.equalTo(object.getContainingFile()),
                false, true, Collections.<AnalyzerScriptParameter>emptyList());

        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                object.getProject(), topDownAnalysisParameters, new ObservableBindingTrace(context.trace),
                moduleDescriptor, context.expressionTypingServices.getPlatformToKotlinClassMap());

        injector.getTopDownAnalysisContext().setOuterDataFlowInfo(context.dataFlowInfo);

        injector.getTopDownAnalyzer().doProcess(context.scope, new NamespaceLikeBuilder() {

            @NotNull
            @Override
            public DeclarationDescriptor getOwnerForChildren() {
                return containingDeclaration;
            }

            @Override
            public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {

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
        ((ModuleDescriptorImpl) moduleDescriptor).addFragmentProvider(packageFragmentProvider);

        // "depend on" builtins module
        ((ModuleDescriptorImpl) moduleDescriptor).addFragmentProvider(KotlinBuiltIns.getInstance().getBuiltInsModule().getPackageFragmentProvider());

        // Import a scope that contains all top-level namespaces that come from dependencies
        // This makes the namespaces visible at all, does not import themselves
        PackageViewDescriptor rootPackage = moduleDescriptor.getPackage(FqName.ROOT);
        assert rootPackage != null : "Coulnd't find root package for " + moduleDescriptor;

        // dummy builder is used because "root" is module descriptor,
        // namespaces added to module explicitly in
        doProcess(rootPackage.getMemberScope(), new NamespaceLikeBuilderDummy(), files);
    }


    public void prepareForTheNextReplLine() {
        context.getScriptScopes().clear();
        context.getScripts().clear();
    }


    @NotNull
    public MutablePackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }
}


