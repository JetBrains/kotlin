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

package org.jetbrains.kotlin.resolve.lazy;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.data.JetScriptInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.kotlin.resolve.lazy.descriptors.*;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ResolveSession implements KotlinCodeAnalyzer {
    private final LazyResolveStorageManager storageManager;
    private final ExceptionTracker exceptionTracker;

    private final ModuleDescriptor module;

    private final BindingTrace trace;
    private final DeclarationProviderFactory declarationProviderFactory;

    private final MemoizedFunctionToNullable<FqName, LazyPackageDescriptor> packages;
    private final PackageFragmentProvider packageFragmentProvider;

    private final MemoizedFunctionToNotNull<JetScript, LazyScriptDescriptor> scriptDescriptors;

    private final MemoizedFunctionToNotNull<JetFile, LazyAnnotations> fileAnnotations;
    private final MemoizedFunctionToNotNull<JetFile, LazyAnnotations> danglingAnnotations;

    private ScopeProvider scopeProvider;

    private JetImportsFactory jetImportFactory;
    private AnnotationResolver annotationResolve;
    private DescriptorResolver descriptorResolver;
    private TypeResolver typeResolver;
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    private ScriptBodyResolver scriptBodyResolver;

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

    @Inject
    public void setScriptBodyResolver(ScriptBodyResolver scriptBodyResolver) {
        this.scriptBodyResolver = scriptBodyResolver;
    }

    // Only calls from injectors expected
    @Deprecated
    public ResolveSession(
            @NotNull Project project,
            @NotNull GlobalContext globalContext,
            @NotNull ModuleDescriptorImpl rootDescriptor,
            @NotNull DeclarationProviderFactory declarationProviderFactory,
            @NotNull BindingTrace delegationTrace
    ) {
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager =
                new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());

        this.storageManager = lockBasedLazyResolveStorageManager;
        this.exceptionTracker = globalContext.getExceptionTracker();
        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
        this.module = rootDescriptor;

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
            public Collection<FqName> getSubPackagesOf(
                    @NotNull FqName fqName, @NotNull Function1<? super Name, ? extends Boolean> nameFilter) {
                LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
                if (packageDescriptor == null) {
                    return Collections.emptyList();
                }
                return packageDescriptor.getDeclarationProvider().getAllDeclaredSubPackages();
            }
        };

        this.scriptDescriptors = storageManager.createMemoizedFunction(
                new Function1<JetScript, LazyScriptDescriptor>() {
                    @Override
                    public LazyScriptDescriptor invoke(JetScript script) {
                        return new LazyScriptDescriptor(
                                ResolveSession.this,
                                scriptBodyResolver,
                                script,
                                ScriptHeaderResolver.getScriptPriority(script)
                        );
                    }
                }
        );

        fileAnnotations = storageManager.createMemoizedFunction(new Function1<JetFile, LazyAnnotations>() {
            @Override
            public LazyAnnotations invoke(JetFile file) {
                return createAnnotations(file, file.getAnnotationEntries());
            }
        });

        danglingAnnotations = storageManager.createMemoizedFunction(new Function1<JetFile, LazyAnnotations>() {
            @Override
            public LazyAnnotations invoke(JetFile file) {
                return createAnnotations(file, file.getDanglingAnnotations());
            }
        });
    }

    private LazyAnnotations createAnnotations(JetFile file, List<JetAnnotationEntry> annotationEntries) {
        JetScope scope = getScopeProvider().getFileScope(file);
        LazyAnnotationsContextImpl lazyAnnotationContext = new LazyAnnotationsContextImpl(annotationResolve, storageManager, trace, scope);
        return new LazyAnnotations(lazyAnnotationContext, annotationEntries);
    }

    @Override
    @NotNull
    public PackageFragmentProvider getPackageFragmentProvider() {
        return packageFragmentProvider;
    }

    @Override
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

    @Override
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
                        if (classLikeInfo instanceof JetScriptInfo) {
                            return getClassDescriptorForScript(((JetScriptInfo) classLikeInfo).getScript());
                        }
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
        if (classOrObject instanceof JetObjectDeclaration) {
            JetObjectDeclaration objectDeclaration = (JetObjectDeclaration) classOrObject;
            JetClassObject classObjectElement = objectDeclaration.getClassObjectElement();
            if (classObjectElement != null) {
                return getClassObjectDescriptor(classObjectElement);
            }
        }
        JetScope resolutionScope = resolutionScopeToResolveDeclaration(classOrObject);

        // Why not use the result here. Because it may be that there is a redeclaration:
        //     class A {} class A { fun foo(): A<completion here>}
        // and if we find the class by name only, we may b-not get the right one.
        // This call is only needed to make sure the classes are written to trace
        ClassifierDescriptor scopeDescriptor = resolutionScope.getClassifier(classOrObject.getNameAsSafeName());
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
    public ClassDescriptor getClassDescriptorForScript(@NotNull JetScript script) {
        JetScope resolutionScope = resolutionScopeToResolveDeclaration(script);
        FqName fqName = ScriptNameUtil.classNameForScript(script);
        ClassifierDescriptor classifier = resolutionScope.getClassifier(fqName.shortName());
        assert classifier != null : "No descriptor for " + fqName + " in file " + script.getContainingFile();
        return (ClassDescriptor) classifier;
    }

    @Override
    @NotNull
    public ScriptDescriptor getScriptDescriptor(@NotNull JetScript script) {
        return scriptDescriptors.invoke(script);
    }

    @NotNull
    /*package*/ LazyClassDescriptor getClassObjectDescriptor(@NotNull JetClassObject classObject) {
        JetClass aClass = JetStubbedPsiUtil.getContainingDeclaration(classObject, JetClass.class);

        LazyClassDescriptor parentClassDescriptor;

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
        parentClassDescriptor.getDescriptorsForExtraClassObjects();
        DeclarationDescriptor classObjectDescriptor = getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classObject.getObjectDeclaration());
        assert classObjectDescriptor != null : "No descriptor found for " + JetPsiUtil.getElementTextWithContext(classObject);

        return (LazyClassDescriptor) classObjectDescriptor;
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
    public DeclarationDescriptor resolveToDescriptor(@NotNull JetDeclaration declaration) {
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

                Name name = parameter.getNameAsSafeName();
                for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
                    if (typeParameterDescriptor.getName().equals(name)) {
                        return typeParameterDescriptor;
                    }
                }

                throw new IllegalStateException("Type parameter " + name + " not found for " + ownerDescriptor);
            }

            @Override
            public DeclarationDescriptor visitNamedFunction(@NotNull JetNamedFunction function, Void data) {
                JetScope scopeForDeclaration = resolutionScopeToResolveDeclaration(function);
                scopeForDeclaration.getFunctions(function.getNameAsSafeName());
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
            }

            @Override
            public DeclarationDescriptor visitParameter(@NotNull JetParameter parameter, Void data) {
                PsiElement grandFather = parameter.getParent().getParent();
                if (grandFather instanceof JetClass) {
                    JetClass jetClass = (JetClass) grandFather;
                    // This is a primary constructor parameter
                    ClassDescriptor classDescriptor = getClassDescriptor(jetClass);
                    if (parameter.hasValOrVarNode()) {
                        classDescriptor.getDefaultType().getMemberScope().getProperties(parameter.getNameAsSafeName());
                        return getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                    }
                    else {
                        ConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                        assert constructor != null: "There are constructor parameters found, so a constructor should also exist";
                        constructor.getValueParameters();
                        return getBindingContext().get(BindingContext.VALUE_PARAMETER, parameter);
                    }
                }
                else if (grandFather instanceof JetNamedFunction) {
                    FunctionDescriptor function = (FunctionDescriptor) visitNamedFunction((JetNamedFunction) grandFather, data);
                    function.getValueParameters();
                    return getBindingContext().get(BindingContext.VALUE_PARAMETER, parameter);
                }
                else {
                    //TODO: support parameters in accessors and other places(?)
                    return super.visitParameter(parameter, data);
                }
            }

            @Override
            public DeclarationDescriptor visitProperty(@NotNull JetProperty property, Void data) {
                JetScope scopeForDeclaration = resolutionScopeToResolveDeclaration(property);
                scopeForDeclaration.getProperties(property.getNameAsSafeName());
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            }

            @Override
            public DeclarationDescriptor visitScript(@NotNull JetScript script, Void data) {
                return scriptDescriptors.invoke(script);
            }

            @Override
            public DeclarationDescriptor visitJetElement(@NotNull JetElement element, Void data) {
                throw new IllegalArgumentException("Unsupported declaration type: " + element + " " +
                                                   JetPsiUtil.getElementTextWithContext(element));
            }
        }, null);
        if (result == null) {
            throw new IllegalStateException("No descriptor resolved for " + declaration + ":\n" +
                                            JetPsiUtil.getElementTextWithContext(declaration));
        }
        return result;
    }

    @NotNull
    public Annotations getFileAnnotations(@NotNull JetFile file) {
        return fileAnnotations.invoke(file);
    }

    @NotNull
    public Annotations getDanglingAnnotations(@NotNull JetFile file) {
        return danglingAnnotations.invoke(file);
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
        for (FqName subPackage : packageFragmentProvider.getSubPackagesOf(current.getFqName(), JetScope.ALL_NAME_FILTER)) {
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

    @Override
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

    @NotNull
    private JetScope resolutionScopeToResolveDeclaration(@NotNull JetDeclaration declaration) {
        boolean isTopLevel = JetStubbedPsiUtil.getContainingDeclaration(declaration) == null;
        if (isTopLevel) { // for top level declarations we search directly in package because of possible conflicts with imports
            FqName fqName = ((JetFile) declaration.getContainingFile()).getPackageFqName();
            LazyPackageDescriptor packageDescriptor = getPackageFragment(fqName);
            assert packageDescriptor != null;
            return packageDescriptor.getMemberScope();
        }
        else {
            return getScopeProvider().getResolutionScopeForDeclaration(declaration);
        }
    }
}
