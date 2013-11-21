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
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForLazyResolve;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.LazyResolveStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve;

public class ResolveSession implements KotlinCodeAnalyzer {
    public static final Function<FqName, Name> NO_ALIASES = new Function<FqName, Name>() {

        @Override
        public Name fun(FqName name) {
            return null;
        }
    };

    private final LazyResolveStorageManager storageManager;

    private final ModuleDescriptor module;
    private final LazyPackageDescriptor rootPackage;

    private final BindingTrace trace;
    private final DeclarationProviderFactory declarationProviderFactory;

    private final Predicate<FqNameUnsafe> specialClasses;


    private final InjectorForLazyResolve injector;

    private final Function<FqName, Name> classifierAliases;

    public ResolveSession(
            @NotNull Project project,
            @NotNull LazyResolveStorageManager storageManager,
            @NotNull ModuleDescriptorImpl rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory
    ) {
        this(project, storageManager, rootDescriptor, declarationProviderFactory, NO_ALIASES,
             Predicates.<FqNameUnsafe>alwaysFalse(),
             new BindingTraceContext());
    }

    public ResolveSession(
            @NotNull Project project,
            @NotNull LazyResolveStorageManager storageManager,
            @NotNull ModuleDescriptorImpl rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull BindingTrace delegationTrace
    ) {
        this(project,
             storageManager,
             rootDescriptor,
             declarationProviderFactory,
             NO_ALIASES,
             Predicates.<FqNameUnsafe>alwaysFalse(),
             delegationTrace);
    }

    @Deprecated // Internal use only
    public ResolveSession(
            @NotNull Project project,
            @NotNull LazyResolveStorageManager storageManager,
            @NotNull ModuleDescriptorImpl rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull Function<FqName, Name> classifierAliases,
            @NotNull Predicate<FqNameUnsafe> specialClasses,
            @NotNull BindingTrace delegationTrace
    ) {
        this.storageManager = storageManager;
        this.classifierAliases = classifierAliases;
        this.specialClasses = specialClasses;
        this.trace = storageManager.createSafeTrace(delegationTrace);
        this.injector = new InjectorForLazyResolve(project, this, rootDescriptor);
        this.module = rootDescriptor;
        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(FqName.ROOT);
        assert provider != null : "No declaration provider for root package in " + rootDescriptor;
        this.rootPackage = new LazyPackageDescriptor(rootDescriptor, FqNameUnsafe.ROOT_NAME, this, provider);
        rootDescriptor.setRootNamespace(rootPackage);

        this.declarationProviderFactory = declarationProviderFactory;
    }

    @NotNull
    public InjectorForLazyResolve getInjector() {
        return injector;
    }

    public boolean isClassSpecial(@NotNull FqNameUnsafe fqName) {
        return specialClasses.apply(fqName);
    }

    @Override
    public ModuleDescriptor getRootModuleDescriptor() {
        return module;
    }

    @NotNull
    public LazyResolveStorageManager getStorageManager() {
        return storageManager;
    }

    @Override
    @Nullable
    public NamespaceDescriptor getPackageDescriptor(@NotNull Name shortName) {
        return rootPackage.getMemberScope().getNamespace(shortName);
    }

    @Override
    @Nullable
    public NamespaceDescriptor getPackageDescriptorByFqName(FqName fqName) {
        if (fqName.isRoot()) {
            return rootPackage;
        }
        List<Name> names = fqName.pathSegments();
        NamespaceDescriptor current = getPackageDescriptor(names.get(0));
        if (current == null) return null;
        for (Name name : names.subList(1, names.size())) {
            current = current.getMemberScope().getNamespace(name);
            if (current == null) return null;
        }
        return current;
    }

    @Override
    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
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
        JetClass aClass = PsiTreeUtil.getParentOfType(classObject, JetClass.class);

        final LazyClassDescriptor parentClassDescriptor;

        if (aClass != null) {
            parentClassDescriptor = (LazyClassDescriptor) getClassDescriptor(aClass);
        }
        else {
            // Class object in object is an error but we want to find descriptors even for this case
            JetObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(classObject, JetObjectDeclaration.class);
            assert objectDeclaration != null : String.format("Class object %s can be in class or object in file %s", classObject, classObject.getContainingFile().getText());
            parentClassDescriptor = (LazyClassDescriptor) getClassDescriptor(objectDeclaration);
        }

        // Activate resolution and writing to trace
        parentClassDescriptor.getClassObjectDescriptor();
        DeclarationDescriptor declaration = getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classObject.getObjectDeclaration());

        if (declaration == null) {
            // It's possible that there are several class objects and another class object is taking part in lazy resolve. We still want to
            // build descriptors for such class objects.
            final JetClassLikeInfo classObjectInfo = parentClassDescriptor.getClassObjectInfo(classObject);
            if (classObjectInfo != null) {
                final Name name = SpecialNames.getClassObjectName(parentClassDescriptor.getName());
                return storageManager.compute(new Function0<LazyClassDescriptor>() {
                    @Override
                    public LazyClassDescriptor invoke() {
                        // Create under lock to avoid premature access to published 'this'
                        return new LazyClassDescriptor(ResolveSession.this, parentClassDescriptor, name, classObjectInfo);
                    }
                });
            }
        }

        return (LazyClassDescriptor) declaration;
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
    public DeclarationDescriptor resolveToDescriptor(JetDeclaration declaration) {
        DeclarationDescriptor result = declaration.accept(new JetVisitor<DeclarationDescriptor, Void>() {
            @Override
            public DeclarationDescriptor visitClass(@NotNull JetClass klass, Void data) {
                return getClassDescriptor(klass);
            }

            @Override
            public DeclarationDescriptor visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, Void data) {
                PsiElement parent = declaration.getParent();
                if (parent instanceof JetClassObject) {
                    JetClassObject jetClassObject = (JetClassObject) parent;
                    return resolveToDescriptor(jetClassObject);
                }
                return getClassDescriptor(declaration);
            }

            @Override
            public DeclarationDescriptor visitClassObject(@NotNull JetClassObject classObject, Void data) {
                return getClassObjectDescriptor(classObject);
            }

            @Override
            public DeclarationDescriptor visitTypeParameter(@NotNull JetTypeParameter parameter, Void data) {
                JetTypeParameterListOwner ownerElement = PsiTreeUtil.getParentOfType(parameter, JetTypeParameterListOwner.class);
                DeclarationDescriptor ownerDescriptor = resolveToDescriptor(ownerElement);

                List<TypeParameterDescriptor> typeParameters;
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

                Name name = ResolveSessionUtils.safeNameForLazyResolve(parameter.getNameAsName());
                for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
                    if (typeParameterDescriptor.getName().equals(name)) {
                        return typeParameterDescriptor;
                    }
                }

                throw new IllegalStateException("Type parameter " + name + " not found for " + ownerDescriptor);
            }

            @Override
            public DeclarationDescriptor visitNamedFunction(@NotNull JetNamedFunction function, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(function);
                scopeForDeclaration.getFunctions(safeNameForLazyResolve(function));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
            }

            @Override
            public DeclarationDescriptor visitParameter(@NotNull JetParameter parameter, Void data) {
                PsiElement grandFather = parameter.getParent().getParent();
                if (grandFather instanceof JetClass) {
                    JetClass jetClass = (JetClass) grandFather;
                    // This is a primary constructor parameter
                    ClassDescriptor classDescriptor = getClassDescriptor(jetClass);
                    if (parameter.getValOrVarNode() != null) {
                        classDescriptor.getDefaultType().getMemberScope().getProperties(safeNameForLazyResolve(parameter));
                        return getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                    }
                    else {
                        ConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                        assert constructor != null: "There are constructor parameters found, so a constructor should also exist";
                        constructor.getValueParameters();
                        return getBindingContext().get(BindingContext.VALUE_PARAMETER, parameter);
                    }
                }
                return super.visitParameter(parameter, data);
            }

            @Override
            public DeclarationDescriptor visitProperty(@NotNull JetProperty property, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(property);
                scopeForDeclaration.getProperties(safeNameForLazyResolve(property));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            }

            @Override
            public DeclarationDescriptor visitObjectDeclarationName(@NotNull JetObjectDeclarationName declarationName, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(declarationName.getParent());
                scopeForDeclaration.getProperties(safeNameForLazyResolve(declarationName));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, declarationName);
            }

            @Override
            public DeclarationDescriptor visitJetElement(@NotNull JetElement element, Void data) {
                throw new IllegalArgumentException("Unsupported declaration type: " + element + " " +
                                                   JetPsiUtil.getElementTextWithContext(element));
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
        rootPackage.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {

            @Override
            public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, Void data) {
                ForceResolveUtil.forceResolveAllContents(descriptor);
                return null;
            }

            @Override
            public Void visitNamespaceDescriptor(NamespaceDescriptor descriptor, Void data) {
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
