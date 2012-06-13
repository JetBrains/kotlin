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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptor {

    private final ResolveSession resolveSession;
    private final ClassMemberDeclarationProvider declarationProvider;

    private final Name name;
    private final DeclarationDescriptor containingDeclaration;
    private final TypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;

    private ClassReceiver implicitReceiver;
    private List<AnnotationDescriptor> annotations;
    private ClassDescriptor classObjectDescriptor;
    private boolean classObjectDescriptorResolved = false;

    private final LazyClassMemberScope unsubstitutedMemberScope;
    private final JetScope unsubstitutedInnerClassesScope;

    private JetScope scopeForClassHeaderResolution;
    private JetScope scopeForMemberDeclarationResolution;


    public LazyClassDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull ClassMemberDeclarationProvider memberDeclarationProvider
    ) {
        this.declarationProvider = memberDeclarationProvider;
        this.resolveSession = resolveSession;
        this.name = name;
        this.containingDeclaration = containingDeclaration;
        this.unsubstitutedMemberScope = new LazyClassMemberScope(resolveSession, memberDeclarationProvider, this);
        this.unsubstitutedInnerClassesScope = new InnerClassesScopeWrapper(unsubstitutedMemberScope);

        this.typeConstructor = new LazyClassTypeConstructor();

        JetClassOrObject classOrObject = memberDeclarationProvider.getOwnerClassOrObject();
        this.kind = getClassKind(classOrObject);
        Modality defaultModality = kind == ClassKind.TRAIT ? Modality.ABSTRACT : Modality.FINAL;
        JetModifierList modifierList = classOrObject.getModifierList();
        this.modality = DescriptorResolver.resolveModalityFromModifiers(modifierList, defaultModality);
        this.visibility = DescriptorResolver.resolveVisibilityFromModifiers(modifierList);
    }

    @NotNull
    private static ClassKind getClassKind(@NotNull JetClassOrObject jetClassOrObject) {
        if (jetClassOrObject instanceof JetClass) {
            JetClass jetClass = (JetClass) jetClassOrObject;
            if (jetClass.isTrait()) return ClassKind.TRAIT;
            if (jetClass.hasModifier(JetTokens.ANNOTATION_KEYWORD)) return ClassKind.ANNOTATION_CLASS;
            if (jetClass.hasModifier(JetTokens.ENUM_KEYWORD)) return ClassKind.ENUM_CLASS;
            return ClassKind.CLASS;
        }
        if (jetClassOrObject instanceof JetObjectDeclaration) {
            return ClassKind.OBJECT;
        }
        throw new IllegalArgumentException("Unknown class or object kind: " + jetClassOrObject);
    }

    @Override
    protected JetScope getScopeForMemberLookup() {
        return unsubstitutedMemberScope;
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return unsubstitutedInnerClassesScope;
    }

    @NotNull
    public JetScope getScopeForClassHeaderResolution() {
        if (scopeForClassHeaderResolution == null) {
            WritableScopeImpl scope = new WritableScopeImpl(
                    resolveSession.getResolutionScope(declarationProvider.getOwnerClassOrObject()), this, RedeclarationHandler.DO_NOTHING, "Class Header Resolution");
            for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
                scope.addClassifierDescriptor(typeParameterDescriptor);
            }
            scope.changeLockLevel(WritableScope.LockLevel.READING);
            scopeForClassHeaderResolution = scope;
        }
        return scopeForClassHeaderResolution;
    }

    public JetScope getScopeForMemberDeclarationResolution() {
        if (scopeForMemberDeclarationResolution == null) {
            WritableScopeImpl scope = new WritableScopeImpl(
                    getScopeForClassHeaderResolution(), this, RedeclarationHandler.DO_NOTHING, "Member Declaration Resolution");
            scope.importScope(getScopeForMemberLookup());

            scope.changeLockLevel(WritableScope.LockLevel.READING);
            scopeForMemberDeclarationResolution = scope;
        }
        return scopeForMemberDeclarationResolution;
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        return unsubstitutedMemberScope.getConstructors();
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return unsubstitutedMemberScope.getPrimaryConstructor();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public JetType getClassObjectType() {
        ClassDescriptor classObjectDescriptor = getClassObjectDescriptor();
        return classObjectDescriptor == null ? null : classObjectDescriptor.getDefaultType();
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }

    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        if (!classObjectDescriptorResolved) {
            JetClassObject classObject = declarationProvider.getClassObject();
            if (classObject != null) {
                JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
                if (objectDeclaration != null) {
                    ClassMemberDeclarationProvider classMemberDeclarationProvider = resolveSession.getDeclarationProviderFactory()
                            .getClassMemberDeclarationProvider(objectDeclaration);
                    classObjectDescriptor = new LazyClassDescriptor(resolveSession, this, JetPsiUtil.NO_NAME_PROVIDED,
                                                                    classMemberDeclarationProvider);
                }
            }
            classObjectDescriptorResolved = true;
        }
        return classObjectDescriptor;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        if (implicitReceiver == null) {
            implicitReceiver = new ClassReceiver(this);
        }
        return implicitReceiver;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        if (annotations == null) {
            JetClassOrObject classOrObject = declarationProvider.getOwnerClassOrObject();
            JetModifierList modifierList = classOrObject.getModifierList();
            if (modifierList != null) {
                AnnotationResolver annotationResolver = resolveSession.getInjector().getAnnotationResolver();
                annotations = annotationResolver
                        .resolveAnnotations(resolveSession.getResolutionScope(classOrObject), modifierList, resolveSession.getTrace());
            }
            else {
                annotations = Collections.emptyList();
            }
        }
        return annotations;
    }

    @NotNull
    @Override
    public Name getName() {
        return name;
    }

    @Override
    public String toString() {
        return "lazy class " + getName().toString();
    }

    private class LazyClassTypeConstructor implements TypeConstructor {
        private Collection<JetType> supertypes = null;
        private List<TypeParameterDescriptor> parameters = null;

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            if (parameters == null) {
                JetClassOrObject declaration = declarationProvider.getOwnerClassOrObject();
                if (declaration instanceof JetClass) {
                    JetClass jetClass = (JetClass) declaration;

                    List<JetTypeParameter> typeParameters = jetClass.getTypeParameters();
                    parameters = new ArrayList<TypeParameterDescriptor>(typeParameters.size());

                    for (int i = 0; i < typeParameters.size(); i++) {
                        parameters.add(new LazyTypeParameterDescriptor(resolveSession, LazyClassDescriptor.this, typeParameters.get(i), i));
                    }
                }
                else {
                    // It is an object declaration, no type parameters
                    parameters = Collections.emptyList();
                }
            }
            return parameters;
        }

        @NotNull
        @Override
        public Collection<? extends JetType> getSupertypes() {
            if (supertypes == null) {
                JetClassOrObject declaration = declarationProvider.getOwnerClassOrObject();
                this.supertypes = resolveSession.getInjector().getDescriptorResolver()
                        .resolveSupertypes(getScopeForClassHeaderResolution(),
                                           declaration,
                                           resolveSession.getTrace());
            }
            return supertypes;
        }

        @Override
        public boolean isSealed() {
            return !getModality().isOverridable();
        }

        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList(); // TODO
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }
    }
}
