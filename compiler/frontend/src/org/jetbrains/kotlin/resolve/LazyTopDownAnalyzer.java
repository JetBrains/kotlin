/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.*;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.resolveUtil.ResolveUtilPackage;
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;

public class LazyTopDownAnalyzer {
    private BindingTrace trace;

    private DeclarationResolver declarationResolver;

    private OverrideResolver overrideResolver;

    private OverloadResolver overloadResolver;

    private VarianceChecker varianceChecker;

    private ModuleDescriptor moduleDescriptor;

    private LazyDeclarationResolver lazyDeclarationResolver;

    private BodyResolver bodyResolver;

    private TopLevelDescriptorProvider topLevelDescriptorProvider;

    private FileScopeProvider fileScopeProvider;

    private DeclarationScopeProvider declarationScopeProvider;

    @Inject
    public void setLazyDeclarationResolver(@NotNull LazyDeclarationResolver lazyDeclarationResolver) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
    }

    @Inject
    public void setTopLevelDescriptorProvider(@NotNull TopLevelDescriptorProvider topLevelDescriptorProvider) {
        this.topLevelDescriptorProvider = topLevelDescriptorProvider;
    }

    @Inject
    public void setFileScopeProvider(@NotNull FileScopeProvider fileScopeProvider) {
        this.fileScopeProvider = fileScopeProvider;
    }

    @Inject
    public void setDeclarationScopeProvider(DeclarationScopeProviderImpl declarationScopeProvider) {
        this.declarationScopeProvider = declarationScopeProvider;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setDeclarationResolver(@NotNull DeclarationResolver declarationResolver) {
        this.declarationResolver = declarationResolver;
    }

    @Inject
    public void setOverrideResolver(@NotNull OverrideResolver overrideResolver) {
        this.overrideResolver = overrideResolver;
    }

    @Inject
    public void setVarianceChecker(@NotNull VarianceChecker varianceChecker) {
        this.varianceChecker = varianceChecker;
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
    public void setBodyResolver(@NotNull BodyResolver bodyResolver) {
        this.bodyResolver = bodyResolver;
    }

    @NotNull
    public TopDownAnalysisContext analyzeDeclarations(
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<? extends PsiElement> declarations,
            @NotNull DataFlowInfo outerDataFlowInfo
    ) {
        final TopDownAnalysisContext c = new TopDownAnalysisContext(topDownAnalysisParameters, outerDataFlowInfo);

        final Multimap<FqName, JetElement> topLevelFqNames = HashMultimap.create();

        final List<JetProperty> properties = new ArrayList<JetProperty>();
        final List<JetNamedFunction> functions = new ArrayList<JetNamedFunction>();

        // fill in the context
        for (PsiElement declaration : declarations) {
            declaration.accept(
                    new JetVisitorVoid() {
                        private void registerDeclarations(@NotNull List<JetDeclaration> declarations) {
                            for (JetDeclaration jetDeclaration : declarations) {
                                jetDeclaration.accept(this);
                            }
                        }

                        @Override
                        public void visitDeclaration(@NotNull JetDeclaration dcl) {
                            throw new IllegalArgumentException("Unsupported declaration: " + dcl + " " + dcl.getText());
                        }

                        @Override
                        public void visitJetFile(@NotNull JetFile file) {
                            if (file.isScript()) {
                                JetScript script = file.getScript();
                                assert script != null;

                                DescriptorResolver.registerFileInPackage(trace, file);
                                c.getScripts().put(script, topLevelDescriptorProvider.getScriptDescriptor(script));
                            }
                            else {
                                JetPackageDirective packageDirective = file.getPackageDirective();
                                assert packageDirective != null : "No package in a non-script file: " + file;

                                c.addFile(file);

                                packageDirective.accept(this);
                                DescriptorResolver.registerFileInPackage(trace, file);

                                registerDeclarations(file.getDeclarations());

                                topLevelFqNames.put(file.getPackageFqName(), packageDirective);
                            }
                        }

                        @Override
                        public void visitPackageDirective(@NotNull JetPackageDirective directive) {
                            DescriptorResolver.resolvePackageHeader(directive, moduleDescriptor, trace);
                        }

                        @Override
                        public void visitImportDirective(@NotNull JetImportDirective importDirective) {
                            LazyFileScope fileScope = (LazyFileScope) fileScopeProvider.getFileScope(
                                    importDirective.getContainingJetFile());
                            fileScope.forceResolveImport(importDirective);
                        }

                        private void visitClassOrObject(@NotNull JetClassOrObject classOrObject) {
                            ClassDescriptorWithResolutionScopes descriptor =
                                    (ClassDescriptorWithResolutionScopes) lazyDeclarationResolver.getClassDescriptor(classOrObject);

                            c.getDeclaredClasses().put(classOrObject, descriptor);
                            registerDeclarations(classOrObject.getDeclarations());
                            registerTopLevelFqName(topLevelFqNames, classOrObject, descriptor);

                            checkClassOrObjectDeclarations(classOrObject, descriptor);
                        }

                        private void checkClassOrObjectDeclarations(JetClassOrObject classOrObject, ClassDescriptor classDescriptor) {
                            boolean defaultObjectAlreadyFound = false;
                            for (JetDeclaration jetDeclaration : classOrObject.getDeclarations()) {
                                if (jetDeclaration instanceof JetObjectDeclaration && ((JetObjectDeclaration) jetDeclaration).isDefault()) {
                                    if (defaultObjectAlreadyFound) {
                                        trace.report(MANY_DEFAULT_OBJECTS.on((JetObjectDeclaration) jetDeclaration));
                                    }
                                    defaultObjectAlreadyFound = true;
                                }
                                else if (jetDeclaration instanceof JetSecondaryConstructor) {
                                    if (DescriptorUtils.isSingletonOrAnonymousObject(classDescriptor)) {
                                        trace.report(SECONDARY_CONSTRUCTOR_IN_OBJECT.on((JetSecondaryConstructor) jetDeclaration));
                                    }
                                    else if (classDescriptor.getKind() == ClassKind.TRAIT) {
                                        trace.report(CONSTRUCTOR_IN_TRAIT.on(jetDeclaration));
                                    }
                                }
                            }
                        }

                        @Override
                        public void visitClass(@NotNull JetClass klass) {
                            visitClassOrObject(klass);
                            registerPrimaryConstructorParameters(klass);
                        }

                        private void registerPrimaryConstructorParameters(@NotNull JetClass klass) {
                            for (JetParameter jetParameter : klass.getPrimaryConstructorParameters()) {
                                if (jetParameter.hasValOrVarNode()) {
                                    c.getPrimaryConstructorParameterProperties().put(
                                            jetParameter,
                                            (PropertyDescriptor) lazyDeclarationResolver.resolveToDescriptor(jetParameter)
                                    );
                                }
                            }
                        }

                        @Override
                        public void visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor) {
                            ClassDescriptor classDescriptor =
                                    (ClassDescriptor) lazyDeclarationResolver.resolveToDescriptor(constructor.getClassOrObject());
                            if (!DescriptorUtils.canHaveSecondaryConstructors(classDescriptor)) {
                                return;
                            }
                            c.getSecondaryConstructors().put(
                                    constructor,
                                    (ConstructorDescriptor) lazyDeclarationResolver.resolveToDescriptor(constructor)
                            );
                            registerScope(c, constructor);
                        }

                        @Override
                        public void visitEnumEntry(@NotNull JetEnumEntry enumEntry) {
                            visitClassOrObject(enumEntry);
                        }

                        @Override
                        public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
                            visitClassOrObject(declaration);
                        }

                        @Override
                        public void visitAnonymousInitializer(@NotNull JetClassInitializer initializer) {
                            registerScope(c, initializer);
                            JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(initializer, JetClassOrObject.class);
                            c.getAnonymousInitializers().put(
                                    initializer,
                                    (ClassDescriptorWithResolutionScopes) lazyDeclarationResolver.resolveToDescriptor(classOrObject)
                            );
                        }

                        @Override
                        public void visitTypedef(@NotNull JetTypedef typedef) {
                            trace.report(UNSUPPORTED.on(typedef, "Typedefs are not supported"));
                        }

                        @Override
                        public void visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration) {
                            // Ignore: multi-declarations are only allowed locally
                        }

                        @Override
                        public void visitNamedFunction(@NotNull JetNamedFunction function) {
                            functions.add(function);
                        }

                        @Override
                        public void visitProperty(@NotNull JetProperty property) {
                            properties.add(property);
                        }
                    }
            );
        }

        createFunctionDescriptors(c, functions);

        createPropertyDescriptors(c, topLevelFqNames, properties);

        resolveAllHeadersInClasses(c);

        declarationResolver.checkRedeclarationsInPackages(topLevelDescriptorProvider, topLevelFqNames);
        declarationResolver.checkRedeclarations(c);

        ResolveUtilPackage.checkTraitRequirements(c.getDeclaredClasses(), trace);

        overrideResolver.check(c);

        varianceChecker.check(c);

        declarationResolver.resolveAnnotationsOnFiles(c, fileScopeProvider);

        overloadResolver.process(c);

        bodyResolver.resolveBodies(c);

        return c;
    }

    private static void resolveAllHeadersInClasses(TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes classDescriptor : c.getAllClasses()) {
            ((LazyClassDescriptor) classDescriptor).resolveMemberHeaders();
        }
    }

    private void createPropertyDescriptors(
            TopDownAnalysisContext c,
            Multimap<FqName, JetElement> topLevelFqNames,
            List<JetProperty> properties
    ) {
        for (JetProperty property : properties) {
            PropertyDescriptor descriptor = (PropertyDescriptor) lazyDeclarationResolver.resolveToDescriptor(property);

            c.getProperties().put(property, descriptor);
            registerTopLevelFqName(topLevelFqNames, property, descriptor);

            registerScope(c, property);
            registerScope(c, property.getGetter());
            registerScope(c, property.getSetter());
        }
    }

    private void createFunctionDescriptors(
            TopDownAnalysisContext c,
            List<JetNamedFunction> functions
    ) {
        for (JetNamedFunction function : functions) {
            c.getFunctions().put(
                    function,
                    (SimpleFunctionDescriptor) lazyDeclarationResolver.resolveToDescriptor(function)
            );
            registerScope(c, function);
        }
    }

    private void registerScope(
            @NotNull TopDownAnalysisContext c,
            @Nullable JetDeclaration declaration
    ) {
        if (declaration == null) return;
        c.registerDeclaringScope(
                declaration,
                declarationScopeProvider.getResolutionScopeForDeclaration(declaration)
        );
    }

    private static void registerTopLevelFqName(
            @NotNull Multimap<FqName, JetElement> topLevelFqNames,
            @NotNull JetNamedDeclaration declaration,
            @NotNull DeclarationDescriptor descriptor
    ) {
        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            FqName fqName = declaration.getFqName();
            if (fqName != null) {
                topLevelFqNames.put(fqName, declaration);
            }
        }
    }
}


