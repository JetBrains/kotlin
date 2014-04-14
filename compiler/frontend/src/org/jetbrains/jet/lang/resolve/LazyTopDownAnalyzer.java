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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyImportScope;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_CLASS_OBJECTS;

public class LazyTopDownAnalyzer {

    @NotNull
    private BindingTrace trace;
    @NotNull
    private DeclarationResolver declarationResolver;
    @NotNull
    private OverrideResolver overrideResolver;
    @NotNull
    private OverloadResolver overloadResolver;
    @NotNull
    private ModuleDescriptor moduleDescriptor;
    @NotNull
    private BodyResolver bodyResolver;

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
            final ResolveSession resolveSession,
            @NotNull TopDownAnalysisParameters topDownAnalysisParameters,
            @NotNull Collection<? extends PsiElement> declarations
    ) {
        assert topDownAnalysisParameters.isLazyTopDownAnalysis() : "Lazy analyzer is run in non-lazy mode";

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

                                c.getScripts().put(script, resolveSession.getScriptDescriptor(script));
                            }
                            else {
                                JetPackageDirective packageDirective = file.getPackageDirective();
                                assert packageDirective != null : "No package in a non-script file: " + file;

                                c.addFile(file);

                                DescriptorResolver.resolvePackageHeader(packageDirective, moduleDescriptor, trace);
                                DescriptorResolver.registerFileInPackage(trace, file);

                                registerDeclarations(file.getDeclarations());

                                topLevelFqNames.put(file.getPackageFqName(), packageDirective);
                            }
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
                                if (jetParameter.getValOrVarNode() != null) {
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

        forceResolveAllClasses(c);

        declarationResolver.checkRedeclarationsInPackages(resolveSession, topLevelFqNames);
        declarationResolver.checkRedeclarationsInInnerClassNames(c);

        overrideResolver.check(c);

        resolveImportsInAllFiles(c, resolveSession);

        overloadResolver.process(c);

        bodyResolver.resolveBodies(c);


        return c;
    }

    private static void resolveImportsInAllFiles(TopDownAnalysisContext c, ResolveSession resolveSession) {
        for (JetFile file : c.getFiles()) {
            resolveAndCheckImports(file, resolveSession);
        }

        for (JetScript script : c.getScripts().keySet()) {
            resolveAndCheckImports((JetFile) script.getContainingFile(), resolveSession);
        }
    }

    private static void forceResolveAllClasses(TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes classDescriptor : c.getAllClasses()) {
            ForceResolveUtil.forceResolveAllContents(classDescriptor);
        }
    }

    private static void createPropertyDescriptors(
            TopDownAnalysisContext c,
            ResolveSession resolveSession,
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
            ResolveSession resolveSession,
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

    private static void resolveAndCheckImports(@NotNull JetFile file, @NotNull ResolveSession resolveSession) {
        LazyImportScope fileScope = resolveSession.getScopeProvider().getExplicitImportsScopeForFile(file);
        fileScope.forceResolveAllContents();
    }

    private static void registerScope(
            @NotNull TopDownAnalysisContext c,
            @NotNull ResolveSession resolveSession,
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

}


