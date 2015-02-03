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
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.LockBasedLazyResolveStorageManager;

import javax.inject.Inject;
import java.util.List;

public class LazyDeclarationResolver {

    private final BindingTrace trace;

    protected DeclarationScopeProvider scopeProvider;
    private TopLevelDescriptorProvider topLevelDescriptorProvider;

    @Inject
    public void setDeclarationScopeProvider(@NotNull DeclarationScopeProviderImpl scopeProvider) {
        this.scopeProvider = scopeProvider;
    }

    @Inject
    public void setTopLevelDescriptorProvider(@NotNull TopLevelDescriptorProvider topLevelDescriptorProvider) {
        this.topLevelDescriptorProvider = topLevelDescriptorProvider;
    }

    @Deprecated
    public LazyDeclarationResolver(
            @NotNull GlobalContext globalContext,
            @NotNull BindingTrace delegationTrace
    ) {
        LockBasedLazyResolveStorageManager lockBasedLazyResolveStorageManager =
                new LockBasedLazyResolveStorageManager(globalContext.getStorageManager());

        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace);
    }

    @NotNull
    public ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject) {
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
    private BindingContext getBindingContext() {
        return trace.getBindingContext();
    }

    @NotNull
    public DeclarationDescriptor resolveToDescriptor(@NotNull JetDeclaration declaration) {
        DeclarationDescriptor result = declaration.accept(new JetVisitor<DeclarationDescriptor, Void>() {
            @Override
            public DeclarationDescriptor visitClass(@NotNull JetClass klass, Void data) {
                return getClassDescriptor(klass);
            }

            @Override
            public DeclarationDescriptor visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, Void data) {
                return getClassDescriptor(declaration);
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
                else if (grandFather instanceof JetSecondaryConstructor) {
                    ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) visitSecondaryConstructor(
                            (JetSecondaryConstructor) grandFather, data
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
            public DeclarationDescriptor visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor, Void data) {
                getClassDescriptor((JetClassOrObject) constructor.getParent().getParent()).getConstructors();
                return getBindingContext().get(BindingContext.CONSTRUCTOR, constructor);
            }

            @Override
            public DeclarationDescriptor visitProperty(@NotNull JetProperty property, Void data) {
                JetScope scopeForDeclaration = resolutionScopeToResolveDeclaration(property);
                scopeForDeclaration.getProperties(property.getNameAsSafeName());
                return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            }

            @Override
            public DeclarationDescriptor visitScript(@NotNull JetScript script, Void data) {
                return topLevelDescriptorProvider.getScriptDescriptor(script);
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
    /*package*/ JetScope resolutionScopeToResolveDeclaration(@NotNull JetDeclaration declaration) {
        boolean isTopLevel = JetStubbedPsiUtil.getContainingDeclaration(declaration) == null;
        if (isTopLevel) { // for top level declarations we search directly in package because of possible conflicts with imports
            FqName fqName = ((JetFile) declaration.getContainingFile()).getPackageFqName();
            LazyPackageDescriptor packageDescriptor = topLevelDescriptorProvider.getPackageFragment(fqName);
            assert packageDescriptor != null;
            return packageDescriptor.getMemberScope();
        }
        else {
            return scopeProvider.getResolutionScopeForDeclaration(declaration);
        }
    }
}
