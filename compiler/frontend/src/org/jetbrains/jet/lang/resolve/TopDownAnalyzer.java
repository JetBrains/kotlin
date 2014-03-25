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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForLazyResolve;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerBasic;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyImportScope;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.LockBasedStorageManager;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_CLASS_OBJECTS;

public class TopDownAnalyzer {

    public static boolean LAZY;

    static {
        LAZY = "true".equals(System.getProperty("lazy.tda"));
    }

    @NotNull
    private BindingTrace trace;
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
    @NotNull
    private ScriptHeaderResolver scriptHeaderResolver;
    @NotNull
    private Project project;

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

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

    @Inject
    public void setScriptHeaderResolver(@NotNull ScriptHeaderResolver scriptHeaderResolver) {
        this.scriptHeaderResolver = scriptHeaderResolver;
    }

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    public void doProcess(
            @NotNull final TopDownAnalysisContext c,
            @NotNull JetScope outerScope,
            @NotNull PackageLikeBuilder owner,
            @NotNull Collection<? extends PsiElement> declarations
    ) {
//        c.enableDebugOutput();
        c.debug("Enter");

        if (LAZY && !c.getTopDownAnalysisParameters().isDeclaredLocally()) {
            final ResolveSession resolveSession = new InjectorForLazyResolve(
                    project,
                    new GlobalContextImpl((LockBasedStorageManager) c.getStorageManager(), c.getExceptionTracker()), // TODO
                    (ModuleDescriptorImpl) moduleDescriptor, // TODO
                    new FileBasedDeclarationProviderFactory(c.getStorageManager(), getFiles(declarations)),
                    trace
            ).getResolveSession();

            final Multimap<FqName, JetElement> topLevelFqNames = HashMultimap.create();

            // fill in the context
            for (PsiElement declaration : declarations) {
                declaration.accept(
                        new JetVisitorVoid() {
                            private void registerDeclarations(@NotNull List<JetDeclaration> declarations) {
                                for (JetDeclaration jetDeclaration : declarations) {
                                    jetDeclaration.accept(this);
                                }
                            }

                            private void registerTopLevelFqName(@NotNull JetNamedDeclaration declaration, @NotNull DeclarationDescriptor descriptor) {
                                if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
                                    FqName fqName = declaration.getFqName();
                                    if (fqName != null) {
                                        topLevelFqNames.put(fqName, declaration);
                                    }
                                }
                            }

                            private void registerScope(@Nullable JetDeclaration declaration) {
                                if (declaration == null) return;
                                c.registerDeclaringScope(
                                        declaration,
                                        resolveSession.getScopeProvider().getResolutionScopeForDeclaration(declaration)
                                );
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
                                    scriptHeaderResolver.processScriptHierarchy(c, script, resolveSession.getScopeProvider().getFileScope(file));
                                }
                                else {
                                    JetPackageDirective packageDirective = file.getPackageDirective();
                                    assert packageDirective != null : "No package in a non-script file: " + file;

                                    c.addFile(file);

                                    DescriptorResolver.resolvePackageHeader(packageDirective, moduleDescriptor, trace);
                                    registerDeclarations(file.getDeclarations());

                                    topLevelFqNames.put(file.getPackageFqName(), packageDirective);
                                }
                                resolveAndCheckImports(file, resolveSession);
                            }

                            private void resolveAndCheckImports(@NotNull JetFile file, @NotNull ResolveSession resolveSession) {
                               LazyImportScope fileScope = resolveSession.getScopeProvider().getExplicitImportsScopeForFile(file);
                               fileScope.forceResolveAllContents();
                           }

                            private void visitClassOrObject(@NotNull JetClassOrObject classOrObject) {
                                ClassDescriptorWithResolutionScopes descriptor = ForceResolveUtil.forceResolveAllContents(
                                        (ClassDescriptorWithResolutionScopes) resolveSession.getClassDescriptor(classOrObject)
                                );

                                c.getClasses().put(classOrObject, descriptor);
                                registerDeclarations(classOrObject.getDeclarations());
                                registerTopLevelFqName(classOrObject, descriptor);

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
                                registerScope(initializer);
                            }

                            @Override
                            public void visitNamedFunction(@NotNull JetNamedFunction function) {
                                c.getFunctions().put(
                                        function,
                                        ForceResolveUtil.forceResolveAllContents(
                                                (SimpleFunctionDescriptor) resolveSession.resolveToDescriptor(function)
                                        )
                                );
                                registerScope(function);
                            }

                            @Override
                            public void visitProperty(@NotNull JetProperty property) {
                                PropertyDescriptor descriptor = ForceResolveUtil.forceResolveAllContents(
                                        (PropertyDescriptor) resolveSession.resolveToDescriptor(property)
                                );

                                c.getProperties().put(property, descriptor);
                                registerTopLevelFqName(property, descriptor);

                                registerScope(property);
                                registerScope(property.getGetter());
                                registerScope(property.getSetter());
                            }
                        }
                );
            }

            declarationResolver.checkRedeclarationsInPackages(resolveSession, topLevelFqNames);
            declarationResolver.checkRedeclarationsInInnerClassNames(c);
            overrideResolver.check(c);
        }
        else {
            typeHierarchyResolver.process(c, outerScope, owner, declarations);
            declarationResolver.process(c);
            overrideResolver.process(c);
            lockScopes(c);
        }

        overloadResolver.process(c);

        if (!c.getTopDownAnalysisParameters().isAnalyzingBootstrapLibrary()) {
            bodyResolver.resolveBodies(c);
        }

        c.debug("Exit");
        c.printDebugOutput(System.out);
    }

    private static Collection<JetFile> getFiles(Collection<? extends PsiElement> declarations) {
        return new LinkedHashSet<JetFile>(KotlinPackage.map(declarations, new Function1<PsiElement, JetFile>() {
            @Nullable
            @Override
            public JetFile invoke(PsiElement element) {
                return (JetFile) element.getContainingFile();
            }
        }));
    }

    private void lockScopes(@NotNull TopDownAnalysisContext c) {
        for (ClassDescriptorWithResolutionScopes mutableClassDescriptor : c.getClasses().values()) {
            ((MutableClassDescriptor) mutableClassDescriptor).lockScopes();
        }

        // SCRIPT: extra code for scripts
        Set<FqName> scriptFqNames = Sets.newHashSet();
        for (JetFile file : c.getFileScopes().keySet()) {
            if (file.isScript()) {
                scriptFqNames.add(file.getPackageFqName());
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


