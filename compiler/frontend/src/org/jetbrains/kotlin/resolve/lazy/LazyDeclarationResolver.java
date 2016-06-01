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

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.KotlinLookupLocation;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.storage.LockBasedLazyResolveStorageManager;

import javax.inject.Inject;
import java.util.List;

public class LazyDeclarationResolver {

    @NotNull private final TopLevelDescriptorProvider topLevelDescriptorProvider;
    @NotNull private final AbsentDescriptorHandler absentDescriptorHandler;
    @NotNull private final BindingTrace trace;

    protected DeclarationScopeProvider scopeProvider;

    // component dependency cycle
    @Inject
    public void setDeclarationScopeProvider(@NotNull DeclarationScopeProviderImpl scopeProvider) {
        this.scopeProvider = scopeProvider;
    }

    @Deprecated
    public LazyDeclarationResolver(
            @NotNull GlobalContext globalContext,
            @NotNull BindingTrace delegationTrace,
            @NotNull TopLevelDescriptorProvider topLevelDescriptorProvider,
            @NotNull AbsentDescriptorHandler absentDescriptorHandler
    ) {
        this.topLevelDescriptorProvider = topLevelDescriptorProvider;
        this.absentDescriptorHandler = absentDescriptorHandler;
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager =
                new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());

        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
    }

    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull KtClassOrObject classOrObject, @NotNull LookupLocation location) {
        return findClassDescriptor(classOrObject, location);
    }

    @NotNull
    public ScriptDescriptor getScriptDescriptor(@NotNull KtScript script, @NotNull LookupLocation location) {
        return (ScriptDescriptor) findClassDescriptor(script, location);
    }

    @NotNull
    private ClassDescriptor findClassDescriptor(
            @NotNull KtNamedDeclaration classObjectOrScript,
            @NotNull LookupLocation location
    ) {
        MemberScope scope = getMemberScopeDeclaredIn(classObjectOrScript, location);

        // Why not use the result here. Because it may be that there is a redeclaration:
        //     class A {} class A { fun foo(): A<completion here>}
        // and if we find the class by name only, we may b-not get the right one.
        // This call is only needed to make sure the classes are written to trace
        ClassifierDescriptor scopeDescriptor = scope.getContributedClassifier(classObjectOrScript.getNameAsSafeName(), location);
        DeclarationDescriptor descriptor = getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, classObjectOrScript);

        if (descriptor == null) {
            throw new IllegalArgumentException(
                    String.format("Could not find a classifier for %s.\n" +
                                  "Found descriptor: %s (%s).\n",
                                  PsiUtilsKt.getElementTextWithContext(classObjectOrScript),
                                  scopeDescriptor != null ? DescriptorRenderer.DEBUG_TEXT.render(scopeDescriptor) : "null",
                                  scopeDescriptor != null ? (scopeDescriptor.getContainingDeclaration().getClass()) : null));
        }

        return (ClassDescriptor) descriptor;
    }

    @NotNull
    private BindingContext getBindingContext() {
        return trace.getBindingContext();
    }

    @NotNull
    public DeclarationDescriptor resolveToDescriptor(@NotNull KtDeclaration declaration) {
        return resolveToDescriptor(declaration, /*track =*/true);
    }

    @NotNull
    private DeclarationDescriptor resolveToDescriptor(@NotNull KtDeclaration declaration, final boolean track) {
        DeclarationDescriptor result = declaration.accept(new KtVisitor<DeclarationDescriptor, Void>() {
            @NotNull
            private LookupLocation lookupLocationFor(@NotNull KtDeclaration declaration, boolean isTopLevel) {
                return isTopLevel && track ? new KotlinLookupLocation(declaration) : NoLookupLocation.WHEN_RESOLVE_DECLARATION;
            }

            @Override
            public DeclarationDescriptor visitClass(@NotNull KtClass klass, Void data) {
                return getClassDescriptor(klass, lookupLocationFor(klass, klass.isTopLevel()));
            }

            @Override
            public DeclarationDescriptor visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, Void data) {
                return getClassDescriptor(declaration, lookupLocationFor(declaration, declaration.isTopLevel()));
            }

            @Override
            public DeclarationDescriptor visitTypeParameter(@NotNull KtTypeParameter parameter, Void data) {
                KtTypeParameterListOwner ownerElement = PsiTreeUtil.getParentOfType(parameter, KtTypeParameterListOwner.class);
                assert ownerElement != null : "Owner not found for type parameter: " + parameter.getText();
                DeclarationDescriptor ownerDescriptor = resolveToDescriptor(ownerElement, /*track =*/false);

                List<TypeParameterDescriptor> typeParameters;
                if (ownerDescriptor instanceof CallableDescriptor) {
                    CallableDescriptor callableDescriptor = (CallableDescriptor) ownerDescriptor;
                    typeParameters = callableDescriptor.getTypeParameters();
                }
                else if (ownerDescriptor instanceof ClassifierDescriptorWithTypeParameters) {
                    ClassifierDescriptorWithTypeParameters classifierDescriptor = (ClassifierDescriptorWithTypeParameters) ownerDescriptor;
                    typeParameters = classifierDescriptor.getTypeConstructor().getParameters();
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
            public DeclarationDescriptor visitNamedFunction(@NotNull KtNamedFunction function, Void data) {
                LookupLocation location = lookupLocationFor(function, function.isTopLevel());
                MemberScope scopeForDeclaration = getMemberScopeDeclaredIn(function, location);
                scopeForDeclaration.getContributedFunctions(function.getNameAsSafeName(), location);
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
            }

            @Override
            public DeclarationDescriptor visitParameter(@NotNull KtParameter parameter, Void data) {
                PsiElement grandFather = parameter.getParent().getParent();
                if (grandFather instanceof KtPrimaryConstructor) {
                    KtClassOrObject jetClass = ((KtPrimaryConstructor) grandFather).getContainingClassOrObject();
                    // This is a primary constructor parameter
                    ClassDescriptor classDescriptor = getClassDescriptor(jetClass, lookupLocationFor(jetClass, false));
                    if (parameter.hasValOrVar()) {
                        classDescriptor.getDefaultType().getMemberScope().getContributedVariables(parameter.getNameAsSafeName(), lookupLocationFor(parameter, false));
                        return getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                    }
                    else {
                        ConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                        assert constructor != null: "There are constructor parameters found, so a constructor should also exist";
                        constructor.getValueParameters();
                        return getBindingContext().get(BindingContext.VALUE_PARAMETER, parameter);
                    }
                }
                else if (grandFather instanceof KtNamedFunction) {
                    FunctionDescriptor function = (FunctionDescriptor) visitNamedFunction((KtNamedFunction) grandFather, data);
                    function.getValueParameters();
                    return getBindingContext().get(BindingContext.VALUE_PARAMETER, parameter);
                }
                else if (grandFather instanceof KtSecondaryConstructor) {
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) visitSecondaryConstructor(
                            (KtSecondaryConstructor) grandFather, data
                    );
                    constructorDescriptor.getValueParameters();
                    return getBindingContext().get(BindingContext.VALUE_PARAMETER, parameter);
                }
                else {
                    //TODO: support parameters in accessors and other places(?)
                    return super.visitParameter(parameter, data);
                }
            }

            @Override
            public DeclarationDescriptor visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, Void data) {
                getClassDescriptor((KtClassOrObject) constructor.getParent().getParent(), lookupLocationFor(constructor, false)).getConstructors();
                return getBindingContext().get(BindingContext.CONSTRUCTOR, constructor);
            }

            @Override
            public DeclarationDescriptor visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor, Void data) {
                getClassDescriptor(constructor.getContainingClassOrObject(), lookupLocationFor(constructor, false)).getConstructors();
                return getBindingContext().get(BindingContext.CONSTRUCTOR, constructor);
            }

            @Override
            public DeclarationDescriptor visitProperty(@NotNull KtProperty property, Void data) {
                LookupLocation location = lookupLocationFor(property, property.isTopLevel());
                MemberScope scopeForDeclaration = getMemberScopeDeclaredIn(property, location);
                scopeForDeclaration.getContributedVariables(property.getNameAsSafeName(), location);
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            }

            @Override
            public DeclarationDescriptor visitTypeAlias(@NotNull KtTypeAlias typeAlias, Void data) {
                LookupLocation location = lookupLocationFor(typeAlias, typeAlias.isTopLevel());
                MemberScope scopeForDeclaration = getMemberScopeDeclaredIn(typeAlias, location);
                scopeForDeclaration.getContributedClassifier(typeAlias.getNameAsSafeName(), location);
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, typeAlias);
            }

            @Override
            public DeclarationDescriptor visitScript(@NotNull KtScript script, Void data) {
                return getScriptDescriptor(script, lookupLocationFor(script, true));
            }

            @Override
            public DeclarationDescriptor visitKtElement(@NotNull KtElement element, Void data) {
                throw new IllegalArgumentException("Unsupported declaration type: " + element + " " +
                                                   PsiUtilsKt.getElementTextWithContext(element));
            }
        }, null);
        if (result == null) {
            return absentDescriptorHandler.diagnoseDescriptorNotFound(declaration);
        }
        return result;
    }

    @NotNull
    /*package*/ MemberScope getMemberScopeDeclaredIn(@NotNull KtDeclaration declaration, @NotNull LookupLocation location) {
        KtDeclaration parentDeclaration = KtStubbedPsiUtil.getContainingDeclaration(declaration);
        boolean isTopLevel = parentDeclaration == null;
        if (isTopLevel) { // for top level declarations we search directly in package because of possible conflicts with imports
            KtFile ktFile = (KtFile) declaration.getContainingFile();
            FqName fqName = ktFile.getPackageFqName();
            LazyPackageDescriptor packageDescriptor = topLevelDescriptorProvider.getPackageFragment(fqName);
            if (packageDescriptor == null) {
                if (topLevelDescriptorProvider instanceof LazyClassContext) {
                    ((LazyClassContext) topLevelDescriptorProvider).getDeclarationProviderFactory().diagnoseMissingPackageFragment(ktFile);
                }
                else {
                    throw new IllegalStateException("Cannot find package fragment for file " + ktFile.getName() + " with package " + fqName);
                }
            }
            return packageDescriptor.getMemberScope();
        }
        else {
            if (parentDeclaration instanceof KtClassOrObject) {
                return getClassDescriptor((KtClassOrObject) parentDeclaration, location).getUnsubstitutedMemberScope();
            }
            else if (parentDeclaration instanceof KtScript) {
                return getScriptDescriptor((KtScript) parentDeclaration, location).getUnsubstitutedMemberScope();
            }
            else {
                throw new IllegalStateException("Don't call this method for local declarations: " + declaration + "\n" +
                                                PsiUtilsKt.getElementTextWithContext(declaration));
            }
        }
    }
}
