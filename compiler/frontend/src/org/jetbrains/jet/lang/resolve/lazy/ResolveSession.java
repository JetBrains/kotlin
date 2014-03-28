/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.storage.ExceptionTracker;
import org.jetbrains.jet.storage.LazyResolveStorageManager;
import org.jetbrains.jet.storage.LockBasedLazyResolveStorageManager;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve;

public class ResolveSession implements KotlinCodeAnalyzer {
    private static final Function<FqName, Name> NO_ALIASES = new Function<FqName, Name>() {
        @Override
        public Name fun(FqName name) {
            return null;
        }
    };

    private final LazyResolveStorageManager storageManager;
    private final ExceptionTracker exceptionTracker;

    private final ModuleDescriptor module;

    private final BindingTrace trace;
    private final DeclarationProviderFactory declarationProviderFactory;

    private final Function<FqName, Name> classifierAliases;

    private final MemoizedFunctionToNullable<FqName, LazyPackageDescriptor> packages;
    private final PackageFragmentProvider packageFragmentProvider;

    private ScopeProvider scopeProvider;

    private JetImportsFactory jetImportFactory;
    private AnnotationResolver annotationResolve;
    private DescriptorResolver descriptorResolver;
    private TypeResolver typeResolver;
    private QualifiedExpressionResolver qualifiedExpressionResolver;

    @Inject
    public void setJetImportFactory(JetImportsFactory jetImportFactory) {
        this.jetImportFactory = jetImportFactory;
    }

    @Inject
    public void setAnnotationResolve(AnnotationResolver annotationResolve) {
        this.annotationResolve = annotationResolve;
    }

    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setQualifiedExpressionResolver(QualifiedExpressionResolver qualifiedExpressionResolver) {
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
    }

    @Inject
    public void setScopeProvider(ScopeProvider scopeProvider) {
        this.scopeProvider = scopeProvider;
    }

    // Only calls from injectors expected
    @Deprecated
    public ResolveSession(
            @NotNull Project project,
            @NotNull GlobalContextImpl globalContext,
            @NotNull ModuleDescriptorImpl rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull BindingTrace delegationTrace
    ) {
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager = new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());
        this.storageManager = lockBasedLazyResolveStorageManager;
        this.exceptionTracker = globalContext.getExceptionTracker();
        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
        this.module = rootDescriptor;

        this.classifierAliases = NO_ALIASES;

        this.packages = storageManager.createMemoizedFunctionWithNullableValues(new MemoizedFunctionToNullable<FqName, LazyPackageDescriptor>() {
            @Nullable
            @Override
            public LazyPackageDescriptor invoke(FqName fqName) {
                return createPackage(fqName);
            }
        });

        this.declarationProviderFactory = declarationProviderFactory;

        this.packageFragmentProvider = new PackageFragmentProvider() {
            @NotNull
            @Override
            public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
                return ContainerUtil.<PackageFragmentDescriptor>createMaybeSingletonList(getPackageFragment(fqName));
            }

            @NotNull
            @Override
            public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
                LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
                if (packageDescriptor == null) {
                    return Collections.emptyList();
                }
                return packageDescriptor.getDeclarationProvider().getAllDeclaredSubPackages();
            }
        };

        // TODO: parameter modification
        rootDescriptor.addFragmentProvider(DependencyKind.SOURCES, packageFragmentProvider);
    }

    @NotNull
    public PackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }

    @Nullable
    public LazyPackageDescriptor getPackageFragment(@NotNull FqName fqName) {
        return packages.invoke(fqName);
    }

    @Nullable
    private LazyPackageDescriptor createPackage(FqName fqName) {
        if (!fqName.isRoot() && getPackageFragment(fqName.parent()) == null) {
            return null;
        }
        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName);
        if (provider == null) {
            return null;
        }
        return new LazyPackageDescriptor(module, fqName, this, provider);
    }

    @NotNull
    @Override
    public ModuleDescriptor getModuleDescriptor() {
        return module;
    }

    @NotNull
    //@Override
    public LazyResolveStorageManager getStorageManager() {
        return storageManager;
    }

    @NotNull
    //@Override
    public ExceptionTracker getExceptionTracker() {
        return exceptionTracker;
    }

    @NotNull
    @ReadOnly
    public Collection<ClassDescriptor> getTopLevelClassDescriptors(@NotNull FqName fqName) {
        if (fqName.isRoot()) return Collections.emptyList();

        PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName.parent());
        if (provider == null) return Collections.emptyList();

        return ContainerUtil.mapNotNull(
                provider.getClassOrObjectDeclarations(fqName.shortName()),
                new Function<JetClassLikeInfo, ClassDescriptor>() {
                    @Override
                    public ClassDescriptor fun(JetClassLikeInfo classLikeInfo) {
                        JetClassOrObject classOrObject = classLikeInfo.getCorrespondingClassOrObject();
                        if (classOrObject == null) return null;
                        return getClassDescriptor(classOrObject);
                    }
                }
        );
    }

    @Override
    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
        if (classOrObject.getParent() instanceof JetClassObject) {
            return getClassObjectDescriptor((JetClassObject) classOrObject.getParent());
        }
        JetScope resolutionScope = getScopeProvider().getResolutionScopeForDeclaration(classOrObject);
        Name name = safeNameForLazyResolve(classOrObject.getNameAsName());

        // Why not use the result here. Because it may be that there is a redeclaration:
        //     class A {} class A { fun foo(): A<completion here>}
        // and if we find the class by name only, we may b-not get the right one.
        // This call is only needed to make sure the classes are written to trace
        ClassifierDescriptor scopeDescriptor = resolutionScope.getClassifier(name);
        DeclarationDescriptor descriptor = getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);

        if (descriptor == null) {
            throw new IllegalArgumentException(
                   String.format("Could not find a classifier for %s.\n" +
                                 "Found descriptor: %s (%s).\n",
                                 JetPsiUtil.getElementTextWithContext(classOrObject),
                                 scopeDescriptor != null ? DescriptorRenderer.DEBUG_TEXT.render(scopeDescriptor) : "null",
                                 scopeDescriptor != null ? (scopeDescriptor.getContainingDeclaration().getClass()) : null));
        }

        return (ClassDescriptor) descriptor;
    }

    @NotNull
    /*package*/ LazyClassDescriptor getClassObjectDescriptor(@NotNull JetClassObject classObject) {
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
            assert classObjectInfo != null :
                    String.format("Failed to find class object info for existent class object declaration: %s",
                                  JetPsiUtil.getElementTextWithContext(classObject));

            final Name name = SpecialNames.getClassObjectName(parentClassDescriptor.getName());
            return storageManager.compute(new Function0<LazyClassDescriptor>() {
                @Override
                public LazyClassDescriptor invoke() {
                    // Create under lock to avoid premature access to published 'this'
                    return new LazyClassDescriptor(ResolveSession.this, parentClassDescriptor, name, classObjectInfo);
                }
            });
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
                JetScope scopeForDeclaration = getScopeProvider().getResolutionScopeForDeclaration(function);
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
                JetScope scopeForDeclaration = getScopeProvider().getResolutionScopeForDeclaration(property);
                scopeForDeclaration.getProperties(safeNameForLazyResolve(property));
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
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

    @NotNull
    private List<LazyPackageDescriptor> getAllPackages() {
        LazyPackageDescriptor rootPackage = getPackageFragment(FqName.ROOT);
        assert rootPackage != null : "Root package must be initialized";

        return collectAllPackages(Lists.<LazyPackageDescriptor>newArrayList(), rootPackage);
    }

    @NotNull
    private List<LazyPackageDescriptor> collectAllPackages(
            @NotNull List<LazyPackageDescriptor> result,
            @NotNull LazyPackageDescriptor current
    ) {
        result.add(current);
        for (FqName subPackage : packageFragmentProvider.getSubPackagesOf(current.getFqName())) {
            LazyPackageDescriptor fragment = getPackageFragment(subPackage);
            assert fragment != null : "Couldn't find fragment for " + subPackage;
            collectAllPackages(result, fragment);
        }
        return result;
    }

    @Override
    public void forceResolveAll() {
        for (LazyPackageDescriptor lazyPackage : getAllPackages()) {
            ForceResolveUtil.forceResolveAllContents(lazyPackage);
        }
    }

    @NotNull
    public ScopeProvider getScopeProvider() {
        return scopeProvider;
    }

    @NotNull
    public JetImportsFactory getJetImportsFactory() {
        return jetImportFactory;
    }

    @NotNull
    public AnnotationResolver getAnnotationResolver() {
        return annotationResolve;
    }

    @NotNull
    public DescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @NotNull
    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    @NotNull
    public QualifiedExpressionResolver getQualifiedExpressionResolver() {
        return qualifiedExpressionResolver;
    }
}
