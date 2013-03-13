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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForLazyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AbstractSubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PackageViewFromSubModule;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.declarations.*;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve;

public class LazyCodeAnalyzer implements KotlinCodeAnalyzer {
    private static final Function<FqName, Name> NO_ALIASES = new Function<FqName, Name>() {

        @Override
        public Name fun(FqName name) {
            return null;
        }
    };

    private final StorageManager storageManager;

    private final BindingTrace trace;
    private final RootDeclarationProvider rootProvider;
    private final ModuleSourcesManager moduleSourcesManager;
    private final DeclarationProviderFactory declarationProviderFactory;

    private final InjectorForLazyResolve injector;

    private final Predicate<FqNameUnsafe> specialClasses;
    private final Function<FqName, Name> classifierAliases;

    private final Collection<ModuleDescriptor> modules;
    private final Map<SubModuleDefinition, SubModuleDescriptor> subModules;

    @Deprecated // Internal use only
    public LazyCodeAnalyzer(
            @NotNull Project project,
            @NotNull StorageManager storageManager,
            @NotNull ModuleSourcesManager moduleSourcesManager,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull Function<FqName, Name> classifierAliases,
            @NotNull Predicate<FqNameUnsafe> specialClasses,
            @NotNull BindingTrace delegationTrace
    ) {
        this.storageManager = storageManager;
        this.classifierAliases = classifierAliases;
        this.specialClasses = specialClasses;
        this.trace = storageManager.createSafeTrace(delegationTrace);
        this.injector = new InjectorForLazyResolve(project, moduleSourcesManager, this);
        this.declarationProviderFactory = declarationProviderFactory;
        this.rootProvider = null; // TODO Get rid of module definitions etc
        this.moduleSourcesManager = moduleSourcesManager;

        // this map implicitly becomes shared between all subModule objects
        // it is crucial for thread safety that it is not modified after
        // the constructor of LazyCodeAnalyzer returns
        Map<SubModuleDefinition, SubModuleDescriptor> subModules = Maps.newHashMap();
        this.modules = computeModules(rootProvider.getModuleDefinitions(), subModules);
        this.subModules = Collections.unmodifiableMap(subModules);
    }

    private Collection<ModuleDescriptor> computeModules(
            Collection<ModuleDefinition> definitions,
            Map<SubModuleDefinition, SubModuleDescriptor> subModules
    ) {
        Collection<ModuleDescriptor> result = Lists.newArrayList();

        for (ModuleDefinition moduleDefinition : definitions) {
            MutableModuleDescriptor module = new MutableModuleDescriptor(moduleDefinition.getName(), moduleDefinition.getPlatformToKotlinClassMap());
            result.add(module);

            for (SubModuleDefinition subModuleDefinition : moduleDefinition.getSubModules()) {
                SubModuleDescriptor subModule = createSubModule(module, subModuleDefinition);
                subModules.put(subModuleDefinition, subModule);

                module.addSubModule(subModule);
            }
        }
        return result;
    }

    private AbstractSubModuleDescriptor createSubModule(
            ModuleDescriptor module,
            final SubModuleDefinition subModuleDefinition
    ) {
        return new AbstractSubModuleDescriptor(module, subModuleDefinition.getName()) {

            @NotNull
            @Override
            public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
                List<PackageFragmentDescriptor> result = Lists.newArrayList();
                result.addAll(subModuleDefinition.getPackageFragmentsLoadedExternally(fqName, this));

                PackageMemberDeclarationProvider provider = subModuleDefinition.getPackageMembersFromSourceFiles(fqName);
                if (provider != null) {
                    result.add(new LazyPackageDescriptor(LazyCodeAnalyzer.this, this, fqName, provider));
                }

                return result;
            }

            @Nullable
            @Override
            public PackageViewDescriptor getPackageView(@NotNull FqName fqName) {
                List<PackageFragmentDescriptor> fragments = DescriptorUtils.getPackageFragmentsIncludingDependencies(this, fqName);
                Collection<FqName> subPackages = subModuleDefinition.getAllSubPackages(fqName);
                if (fragments.isEmpty() && subPackages.isEmpty()) {
                    return null;
                }
                return new PackageViewFromSubModule(this, fqName, fragments);
            }

            @NotNull
            @Override
            public Collection<SubModuleDescriptor> getDependencies() {
                Collection<SubModuleDescriptor> result = Lists.newArrayList();

                for (SubModuleDefinition dependency : subModuleDefinition.getDependenciesToBeResolved()) {
                    SubModuleDescriptor descriptor = subModules.get(dependency);
                    assert descriptor != null : "No subModule created for dependency " + subModuleDefinition.getName() + " of " + this;
                    result.add(descriptor);
                }

                result.addAll(subModuleDefinition.getExternalDependencies());

                return result;
            }

            @NotNull
            @Override
            public List<ImportPath> getDefaultImports() {
                return subModuleDefinition.getDefaultImports();
            }
        };
    }

    @NotNull
    public ModuleSourcesManager getModuleSourcesManager() {
        return moduleSourcesManager;
    }

    @NotNull
    public InjectorForLazyResolve getInjector() {
        return injector;
    }

    public boolean isClassSpecial(@NotNull FqNameUnsafe fqName) {
        return specialClasses.apply(fqName);
    }

    @NotNull
    public StorageManager getStorageManager() {
        return storageManager;
    }

    @NotNull
    @Override
    public Collection<ModuleDescriptor> getModules() {
        return modules;
    }

    @NotNull
    private ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
        if (classOrObject.getParent() instanceof JetClassObject) {
            return getClassObjectDescriptor((JetClassObject) classOrObject.getParent());
        }
        JetScope resolutionScope = getInjector().getScopeProvider().getResolutionScopeForDeclaration(classOrObject);
        Name name = safeNameForLazyResolve(classOrObject.getNameAsName());

        // Why not use the result here. Because it may be that there is a redeclaration:
        //     class A {} class A { fun foo(): A<completion here>}
        // and if we find the class by name only, we may b-not get the right one.
        // This call is only needed to make sure the classes are written to trace
        resolutionScope.getClassifier(name);
        DeclarationDescriptor declaration = getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);

        if (declaration == null) {
            // Why not use the result here. See the comment
            resolutionScope.getObjectDescriptor(name);
            declaration = getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);
        }
        if (declaration == null) {
            throw new IllegalArgumentException("Could not find a classifier for " + classOrObject + " " + classOrObject.getText());
        }
        return (ClassDescriptor) declaration;
    }

    /*package*/ LazyClassDescriptor getClassObjectDescriptor(JetClassObject classObject) {
        LazyClassDescriptor classDescriptor = (LazyClassDescriptor) getClassDescriptor(PsiTreeUtil.getParentOfType(classObject, JetClass.class));
        LazyClassDescriptor classObjectDescriptor = (LazyClassDescriptor) classDescriptor.getClassObjectDescriptor();
        assert classObjectDescriptor != null : "Class object is declared, but is null for " + classDescriptor;
        return classObjectDescriptor;
    }

    @Override
    @NotNull
    public BindingContext getBindingContext() {
        return trace.getBindingContext();
    }

    @NotNull
    public BindingTrace getTrace() {
        return trace;
    }

    @NotNull
    public DeclarationProviderFactory getDeclarationProviderFactory() {
        return declarationProviderFactory;
    }

    @Override
    @NotNull
    public DeclarationDescriptor getDescriptor(@NotNull JetDeclaration declaration) {
        DeclarationDescriptor result = declaration.accept(new JetVisitor<DeclarationDescriptor, Void>() {
            @Override
            public DeclarationDescriptor visitClass(JetClass klass, Void data) {
                return getClassDescriptor(klass);
            }

            @Override
            public DeclarationDescriptor visitObjectDeclaration(JetObjectDeclaration declaration, Void data) {
                PsiElement parent = declaration.getParent();
                if (parent instanceof JetClassObject) {
                    JetClassObject jetClassObject = (JetClassObject) parent;
                    return getDescriptor(jetClassObject);
                }
                return getClassDescriptor(declaration);
            }

            @Override
            public DeclarationDescriptor visitClassObject(JetClassObject classObject, Void data) {
                DeclarationDescriptor containingDeclaration =
                        getInjector().getScopeProvider().getResolutionScopeForDeclaration(classObject).getContainingDeclaration();
                return ((ClassDescriptor) containingDeclaration).getClassObjectDescriptor();
            }

            @Override
            public DeclarationDescriptor visitTypeParameter(JetTypeParameter parameter, Void data) {
                JetTypeParameterListOwner ownerElement = PsiTreeUtil.getParentOfType(parameter, JetTypeParameterListOwner.class);
                DeclarationDescriptor ownerDescriptor = getDescriptor(ownerElement);

                List<TypeParameterDescriptor> typeParameters;
                Name name = parameter.getNameAsName();
                if (ownerDescriptor instanceof CallableDescriptor) {
                    CallableDescriptor callableDescriptor = (CallableDescriptor) ownerDescriptor;
                    typeParameters = callableDescriptor.getTypeParameters();
                }
                else if (ownerDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) ownerDescriptor;
                    typeParameters = classDescriptor.getTypeConstructor().getParameters();
                }
                else {
                    throw new IllegalStateException("Unknown owner kind for a type parameter: " + ownerDescriptor);
                }

                for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
                    if (typeParameterDescriptor.getName().equals(name)) {
                        return typeParameterDescriptor;
                    }
                }

                throw new IllegalStateException("Type parameter " + name + " not found for " + ownerDescriptor);
            }

            @Override
            public DeclarationDescriptor visitNamedFunction(JetNamedFunction function, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(function);
                scopeForDeclaration.getFunctions(safeNameForLazyResolve(function));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
            }

            @Override
            public DeclarationDescriptor visitParameter(JetParameter parameter, Void data) {
                PsiElement grandFather = parameter.getParent().getParent();
                if (grandFather instanceof JetClass) {
                    JetClass jetClass = (JetClass) grandFather;
                    // This is a primary constructor parameter
                    if (parameter.getValOrVarNode() != null) {
                        getClassDescriptor(jetClass).getDefaultType().getMemberScope().getProperties(safeNameForLazyResolve(parameter));
                        return getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                    }
                }
                return super.visitParameter(parameter, data);
            }

            @Override
            public DeclarationDescriptor visitProperty(JetProperty property, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(property);
                scopeForDeclaration.getProperties(safeNameForLazyResolve(property));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            }

            @Override
            public DeclarationDescriptor visitObjectDeclarationName(JetObjectDeclarationName declarationName, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(declarationName.getParent());
                scopeForDeclaration.getProperties(safeNameForLazyResolve(declarationName));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, declarationName);
            }

            @Override
            public DeclarationDescriptor visitJetElement(JetElement element, Void data) {
                throw new IllegalArgumentException("Unsupported declaration type: " + element + " " + element.getText());
            }
        }, null);
        if (result == null) {
            throw new IllegalStateException("No descriptor resolved for " + declaration + " " + declaration.getText());
        }
        return result;
    }

    @NotNull
    public Name resolveClassifierAlias(@NotNull FqName packageName, @NotNull Name alias) {
        // TODO: creating a new FqName object every time...
        Name actualName = classifierAliases.fun(packageName.child(alias));
        if (actualName == null) {
            return alias;
        }
        return actualName;
    }

    @Override
    public void forceResolveAll() {
        for (ModuleDescriptor module : modules) {
            module.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {

                @Override
                public Void visitModuleDescriptor(ModuleDescriptor descriptor, Void data) {
                    for (SubModuleDescriptor subModuleDescriptor : descriptor.getSubModules()) {
                        subModuleDescriptor.acceptVoid(this);
                    }
                    return null;
                }

                @Override
                public Void visitSubModuleDescriptor(SubModuleDescriptor descriptor, Void data) {
                    PackageViewDescriptor packageView = descriptor.getPackageView(FqName.ROOT);
                    if (packageView != null) {
                        ForceResolveUtil.forceResolveAllContents(packageView);
                    }
                    return null;
                }

                @Override
                public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, Void data) {
                    ForceResolveUtil.forceResolveAllContents(descriptor);
                    return null;
                }

                @Override
                public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                    ForceResolveUtil.forceResolveAllContents(descriptor);
                    return null;
                }

                @Override
                public Void visitModuleDeclaration(ModuleDescriptor descriptor, Void data) {
                    ForceResolveUtil.forceResolveAllContents(descriptor);
                    return null;
                }

                @Override
                public Void visitScriptDescriptor(ScriptDescriptor scriptDescriptor, Void data) {
                    ForceResolveUtil.forceResolveAllContents(scriptDescriptor);
                    return null;
                }
            });
        }
    }
}
