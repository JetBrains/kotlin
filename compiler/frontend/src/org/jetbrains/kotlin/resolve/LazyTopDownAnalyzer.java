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
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.LazyFileScope;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.kotlin.resolve.resolveUtil.ResolveUtilPackage;
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.MANY_CLASS_OBJECTS;
import static org.jetbrains.kotlin.diagnostics.Errors.UNSUPPORTED;

public class LazyTopDownAnalyzer {
    private BindingTrace trace;

    private DeclarationResolver declarationResolver;

    private OverrideResolver overrideResolver;

    private OverloadResolver overloadResolver;

    private VarianceChecker varianceChecker;

    private ModuleDescriptor moduleDescriptor;

    private KotlinCodeAnalyzer resolveSession;

    private BodyResolver bodyResolver;

    private TopDownAnalyzer topDownAnalyzer;

    @Inject
    public void setKotlinCodeAnalyzer(@NotNull KotlinCodeAnalyzer kotlinCodeAnalyzer) {
        this.resolveSession = kotlinCodeAnalyzer;
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

    @Inject
    public void setTopDownAnalyzer(@NotNull TopDownAnalyzer topDownAnalyzer) {
        this.topDownAnalyzer = topDownAnalyzer;
    }

    @NotNull
    public TopDownAnalysisContext analyzeFiles(
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<JetFile> files,
            @NotNull List<? extends PackageFragmentProvider> additionalProviders
    ) {
        if (!topDownAnalysisParameters.isLazy()) {
            return topDownAnalyzer.analyzeFiles(
                    topDownAnalysisParameters, files,
                    additionalProviders.toArray(new PackageFragmentProvider[additionalProviders.size()]));
        }
        
        PackageFragmentProvider provider;
        if (additionalProviders.isEmpty()) {
            provider = resolveSession.getPackageFragmentProvider();
        }
        else {
            provider = new CompositePackageFragmentProvider(KotlinPackage.plus(
                    Arrays.asList(resolveSession.getPackageFragmentProvider()),
                    additionalProviders));
        }

        ((ModuleDescriptorImpl) resolveSession.getModuleDescriptor()).initialize(provider);

        return analyzeDeclarations(topDownAnalysisParameters, files);
    }

    @NotNull
    public TopDownAnalysisContext analyzeDeclarations(
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<? extends PsiElement> declarations
    ) {
        assert topDownAnalysisParameters.isLazy() : "Lazy analyzer is run in non-lazy mode";

        final TopDownAnalysisContext c = new TopDownAnalysisContext(topDownAnalysisParameters);
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
                                c.getScripts().put(script, resolveSession.getScriptDescriptor(script));
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
                            LazyFileScope fileScope = resolveSession.getScopeProvider().getFileScope(
                                    importDirective.getContainingJetFile());
                            fileScope.forceResolveImport(importDirective);
                        }

                        private void visitClassOrObject(@NotNull JetClassOrObject classOrObject) {
                            ClassDescriptorWithResolutionScopes descriptor =
                                    (ClassDescriptorWithResolutionScopes) resolveSession.getClassDescriptor(classOrObject);

                            c.getDeclaredClasses().put(classOrObject, descriptor);
                            registerDeclarations(classOrObject.getDeclarations());
                            registerTopLevelFqName(topLevelFqNames, classOrObject, descriptor);

                            checkManyClassObjects(classOrObject);
                        }

                        private void checkManyClassObjects(JetClassOrObject classOrObject) {
                            boolean classObjectAlreadyFound = false;
                            for (JetDeclaration jetDeclaration : classOrObject.getDeclarations()) {
                                jetDeclaration.accept(this);

                                if (jetDeclaration instanceof JetClassObject) {
                                    if (classObjectAlreadyFound) {
                                        trace.report(MANY_CLASS_OBJECTS.on((JetClassObject) jetDeclaration));
                                    }
                                    classObjectAlreadyFound = true;
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
                                            (PropertyDescriptor) resolveSession.resolveToDescriptor(jetParameter)
                                    );
                                }
                            }
                        }

                        @Override
                        public void visitClassObject(@NotNull JetClassObject classObject) {
                            visitClassOrObject(classObject.getObjectDeclaration());
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
                            registerScope(c, resolveSession, initializer);
                            JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(initializer, JetClassOrObject.class);
                            c.getAnonymousInitializers().put(
                                    initializer,
                                    (ClassDescriptorWithResolutionScopes) resolveSession.resolveToDescriptor(classOrObject)
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

        createFunctionDescriptors(c, resolveSession, functions);

        createPropertyDescriptors(c, resolveSession, topLevelFqNames, properties);

        resolveAllHeadersInClasses(c);

        declarationResolver.checkRedeclarationsInPackages(resolveSession, topLevelFqNames);
        declarationResolver.checkRedeclarationsInInnerClassNames(c);

        ResolveUtilPackage.checkTraitRequirements(c.getDeclaredClasses(), trace);

        overrideResolver.check(c);

        varianceChecker.check(c);

        resolveImportsInAllFiles(c, resolveSession);

        declarationResolver.resolveAnnotationsOnFiles(c, resolveSession.getScopeProvider());

        overloadResolver.process(c);

        bodyResolver.resolveBodies(c);

        return c;
    }

    private static void resolveImportsInAllFiles(TopDownAnalysisContext c, KotlinCodeAnalyzer resolveSession) {
        for (JetFile file : c.getFiles()) {
            resolveAndCheckImports(file, resolveSession);
        }

        for (JetScript script : c.getScripts().keySet()) {
            resolveAndCheckImports(script.getContainingJetFile(), resolveSession);
        }
    }

    private static void resolveAllHeadersInClasses(TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes classDescriptor : c.getAllClasses()) {
            ((LazyClassDescriptor) classDescriptor).resolveMemberHeaders();
        }
    }

    private static void createPropertyDescriptors(
            TopDownAnalysisContext c,
            KotlinCodeAnalyzer resolveSession,
            Multimap<FqName, JetElement> topLevelFqNames,
            List<JetProperty> properties
    ) {
        for (JetProperty property : properties) {
            PropertyDescriptor descriptor = (PropertyDescriptor) resolveSession.resolveToDescriptor(property);

            c.getProperties().put(property, descriptor);
            registerTopLevelFqName(topLevelFqNames, property, descriptor);

            registerScope(c, resolveSession, property);
            registerScope(c, resolveSession, property.getGetter());
            registerScope(c, resolveSession, property.getSetter());
        }
    }

    private static void createFunctionDescriptors(
            TopDownAnalysisContext c,
            KotlinCodeAnalyzer resolveSession,
            List<JetNamedFunction> functions
    ) {
        for (JetNamedFunction function : functions) {
            c.getFunctions().put(
                    function,
                    (SimpleFunctionDescriptor) resolveSession.resolveToDescriptor(function)
            );
            registerScope(c, resolveSession, function);
        }
    }

    private static void resolveAndCheckImports(@NotNull JetFile file, @NotNull KotlinCodeAnalyzer resolveSession) {
        LazyFileScope fileScope = resolveSession.getScopeProvider().getFileScope(file);
        fileScope.forceResolveAllImports();
    }

    private static void registerScope(
            @NotNull TopDownAnalysisContext c,
            @NotNull KotlinCodeAnalyzer resolveSession,
            @Nullable JetDeclaration declaration
    ) {
        if (declaration == null) return;
        c.registerDeclaringScope(
                declaration,
                resolveSession.getScopeProvider().getResolutionScopeForDeclaration(declaration)
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

    @NotNull
    public KotlinCodeAnalyzer getCodeAnalyzer() {
        return resolveSession;
    }
}


