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
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerBasic;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

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
            @NotNull TopDownAnalysisContext c,
            @NotNull JetScope outerScope,
            @NotNull PackageLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations
    ) {
//        c.enableDebugOutput();
        c.debug("Enter");

        typeHierarchyResolver.process(c, outerScope, owner, declarations);
        declarationResolver.process(c);
        overrideResolver.process(c);

        lockScopes(c);

        overloadResolver.process(c);

        if (!c.getTopDownAnalysisParameters().isAnalyzingBootstrapLibrary()) {
            bodyResolver.resolveBodies(c);
        }

        c.debug("Exit");
        c.printDebugOutput(System.out);
    }

    private void lockScopes(@NotNull TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes mutableClassDescriptor : c.getClasses().values()) {
            ((MutableClassDescriptor) mutableClassDescriptor).lockScopes();
        }
        Set<FqName> scriptFqNames = Sets.newHashSet();
        for (JetFile file : c.getFileScopes().keySet()) {
            if (file.isScript()) {
                scriptFqNames.add(JetPsiUtil.getFQName(file));
            }
        }
        for (MutablePackageFragmentDescriptor fragment : packageFragmentProvider.getAllFragments()) {
            // todo: this is hack in favor of REPL
            if (!scriptFqNames.contains(fragment.getFqName())) {
                fragment.getMemberScope().changeLockLevel(WritableScope.LockLevel.READING);
            }
        }
    }

    public static void processClassOrObject(
            @NotNull GlobalContext globalContext,
            @Nullable final WritableScope scope,
            @NotNull ExpressionTypingContext context,
            @NotNull final DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject object
    ) {
        ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(Name.special("<dummy for object>"),
                                                                         Collections.<ImportPath>emptyList(),
                                                                         PlatformToKotlinClassMap.EMPTY);

        TopDownAnalysisParameters topDownAnalysisParameters =
                new TopDownAnalysisParameters(
                        globalContext.getStorageManager(),
                        globalContext.getExceptionTracker(),
                        Predicates.equalTo(object.getContainingFile()),
                        false,
                        true,
                        Collections.<AnalyzerScriptParameter>emptyList()
                );

        InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(
                object.getProject(), topDownAnalysisParameters, context.trace, moduleDescriptor
        );

        TopDownAnalysisContext c = new TopDownAnalysisContext(topDownAnalysisParameters);
        c.setOuterDataFlowInfo(context.dataFlowInfo);

        injector.getTopDownAnalyzer().doProcess(
               c,
               context.scope,
               new PackageLikeBuilder() {

                   @NotNull
                   @Override
                   public DeclarationDescriptor getOwnerForChildren() {
                       return containingDeclaration;
                   }

                   @Override
                   public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
                       if (scope != null) {
                           scope.addClassifierDescriptor(classDescriptor);
                       }
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
               },
               Collections.<PsiElement>singletonList(object)
        );
    }

    @NotNull
    public TopDownAnalysisContext analyzeFiles(
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<JetFile> files
    ) {
        ((ModuleDescriptorImpl) moduleDescriptor).addFragmentProvider(DependencyKind.SOURCES, packageFragmentProvider);

        // "depend on" builtins module
        ((ModuleDescriptorImpl) moduleDescriptor).addFragmentProvider(DependencyKind.BUILT_INS, KotlinBuiltIns.getInstance().getBuiltInsModule().getPackageFragmentProvider());

        // dummy builder is used because "root" is module descriptor,
        // packages added to module explicitly in

        TopDownAnalysisContext c = new TopDownAnalysisContext(topDownAnalysisParameters);
        doProcess(c, JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor), new PackageLikeBuilderDummy(), files);
        return c;
    }


    public void prepareForTheNextReplLine(@NotNull TopDownAnalysisContext c) {
        c.getScriptScopes().clear();
        c.getScripts().clear();
    }


    @NotNull
    public MutablePackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }
}


