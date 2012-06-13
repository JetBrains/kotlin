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

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForLazyResolve;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.List;

/**
 * @author abreslav
 */
public class ResolveSession {
    private final ModuleDescriptor module;
    private final LazyPackageDescriptor rootPackage;

    private final BindingTrace trace = new BindingTraceContext();
    private final DeclarationProviderFactory declarationProviderFactory;

    private final InjectorForLazyResolve injector;
    private final ModuleConfiguration moduleConfiguration;

    public ResolveSession(
            @NotNull Project project,
            @NotNull ModuleDescriptor rootDescriptor,
            @NotNull ModuleConfiguration moduleConfiguration,
            @NotNull DeclarationProviderFactory declarationProviderFactory
    ) {
        this.injector = new InjectorForLazyResolve(project, this, trace);
        this.module = rootDescriptor;
        this.moduleConfiguration = moduleConfiguration;
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

    @NotNull
    public ModuleConfiguration getModuleConfiguration() {
        return moduleConfiguration;
    }

    @Nullable
    public NamespaceDescriptor getPackageDescriptor(@NotNull Name shortName) {
        return rootPackage.getMemberScope().getNamespace(shortName);
    }

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

    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
        if (classOrObject.getParent() instanceof JetClassObject) {
            return getClassObjectDescriptor((JetClassObject) classOrObject.getParent());
        }
        JetScope resolutionScope = getInjector().getScopeProvider().getResolutionScopeForDeclaration((JetDeclaration) classOrObject);
        Name name = classOrObject.getNameAsName();
        assert name != null : "Name is null for " + classOrObject + " " + classOrObject.getText();
        ClassifierDescriptor classifier = resolutionScope.getClassifier(name);
        return (ClassDescriptor) classifier;
    }

    /*package*/ LazyClassDescriptor getClassObjectDescriptor(JetClassObject classObject) {
        LazyClassDescriptor classDescriptor = (LazyClassDescriptor) getClassDescriptor(PsiTreeUtil.getParentOfType(classObject, JetClass.class));
        LazyClassDescriptor classObjectDescriptor = (LazyClassDescriptor) classDescriptor.getClassObjectDescriptor();
        assert classObjectDescriptor != null : "Class object is declared, but is null for " + classDescriptor;
        return classObjectDescriptor;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return trace.getBindingContext();
    }

    @NotNull
    /*package*/ BindingTrace getTrace() {
        return trace;
    }

    @NotNull
    public DeclarationProviderFactory getDeclarationProviderFactory() {
        return declarationProviderFactory;
    }

    @NotNull
    public JetScope getResolutionScope(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof JetFile) {
            JetFile file = (JetFile) parent;
            return getInjector().getScopeProvider().getFileScopeForDeclarationResolution(file);
        }

        if (parent instanceof JetClassBody) {
            return getEnclosingLazyClass(element).getScopeForMemberDeclarationResolution();
        }

        if (parent instanceof JetClassObject) {
            return new InnerClassesScopeWrapper(getEnclosingLazyClass(element).getScopeForMemberDeclarationResolution());
        }

        throw new IllegalArgumentException("Unsupported PSI element: " + element);
    }

    private LazyClassDescriptor getEnclosingLazyClass(PsiElement element) {
        JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(element.getParent(), JetClassOrObject.class);
        assert classOrObject != null : "Called for an element that is not a class member: " + element;
        ClassDescriptor classDescriptor = getClassDescriptor(classOrObject);
        assert classDescriptor instanceof LazyClassDescriptor : "Trying to resolve a member of a non-lazily loaded class: " + element;
        return (LazyClassDescriptor) classDescriptor;
    }

    @NotNull
    public DeclarationDescriptor resolveToDescriptor(JetDeclaration declaration) {
        return declaration.accept(new JetVisitor<DeclarationDescriptor, Void>() {
            @Override
            public DeclarationDescriptor visitClass(JetClass klass, Void data) {
                return getClassDescriptor(klass);
            }

            @Override
            public DeclarationDescriptor visitObjectDeclaration(JetObjectDeclaration declaration, Void data) {
                PsiElement parent = declaration.getParent();
                if (parent instanceof JetClassObject) {
                    JetClassObject jetClassObject = (JetClassObject) parent;
                    return resolveToDescriptor(jetClassObject);
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
                DeclarationDescriptor ownerDescriptor = resolveToDescriptor(ownerElement);

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
                scopeForDeclaration.getFunctions(function.getNameAsName());
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
            }

            @Override
            public DeclarationDescriptor visitParameter(JetParameter parameter, Void data) {
                PsiElement grandFather = parameter.getParent().getParent();
                if (grandFather instanceof JetClass) {
                    JetClass jetClass = (JetClass) grandFather;
                    // This is a primary constructor parameter
                    if (parameter.getValOrVarNode() != null) {
                        getClassDescriptor(jetClass).getDefaultType().getMemberScope().getProperties(parameter.getNameAsName());
                        return getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                    }
                }
                return super.visitParameter(parameter, data);
            }

            @Override
            public DeclarationDescriptor visitProperty(JetProperty property, Void data) {
                JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(property);
                scopeForDeclaration.getProperties(property.getNameAsName());
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            }

            @Override
            public DeclarationDescriptor visitJetElement(JetElement element, Void data) {
                throw new IllegalArgumentException("Unsupported declaration type: " + element + " " + element.getText());
            }
        }, null);
    }
}
