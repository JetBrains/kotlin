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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import jet.Function0;
import jet.Function1;
import jet.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassInfoUtil;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.data.SyntheticClassObjectInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.NullableLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isSyntheticClassObject;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.*;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName;

public class LazyClassDescriptor extends ClassDescriptorBase implements LazyDescriptor, ClassDescriptor {
    private static final Predicate<JetType> VALID_SUPERTYPE = new Predicate<JetType>() {
        @Override
        public boolean apply(JetType type) {
            assert !type.isError() : "Error types must be filtered out in DescriptorResolver";
            return TypeUtils.getClassDescriptor(type) != null;
        }
    };
    private final ResolveSession resolveSession;
    private final JetClassLikeInfo originalClassInfo;
    private final ClassMemberDeclarationProvider declarationProvider;

    private final LazyClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;
    private final boolean isInner;

    private final NotNullLazyValue<List<AnnotationDescriptor>> annotations;
    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;

    private final LazyClassMemberScope unsubstitutedMemberScope;

    private final NotNullLazyValue<JetScope> scopeForClassHeaderResolution;
    private final NotNullLazyValue<JetScope> scopeForMemberDeclarationResolution;
    private final NotNullLazyValue<JetScope> scopeForPropertyInitializerResolution;

    public LazyClassDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull JetClassLikeInfo classLikeInfo
    ) {
        super(resolveSession.getStorageManager(), containingDeclaration, name);
        this.resolveSession = resolveSession;

        if (classLikeInfo.getCorrespondingClassOrObject() != null) {
            this.resolveSession.getTrace().record(BindingContext.CLASS, classLikeInfo.getCorrespondingClassOrObject(), this);
        }

        this.originalClassInfo = classLikeInfo;
        this.declarationProvider = resolveSession.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);

        this.unsubstitutedMemberScope = new LazyClassMemberScope(resolveSession, declarationProvider, this);

        this.typeConstructor = new LazyClassTypeConstructor();

        this.kind = classLikeInfo.getClassKind();

        JetModifierList modifierList = classLikeInfo.getModifierList();
        if (kind.isSingleton()) {
            this.modality = Modality.FINAL;
        }
        else {
            Modality defaultModality = kind == ClassKind.TRAIT ? Modality.ABSTRACT : Modality.FINAL;
            this.modality = resolveModalityFromModifiers(modifierList, defaultModality);
        }
        this.visibility = isSyntheticClassObject(this)
                          ? DescriptorUtils.getSyntheticClassObjectVisibility()
                          : resolveVisibilityFromModifiers(modifierList, getDefaultClassVisibility(this));
        this.isInner = isInnerClass(modifierList);

        StorageManager storageManager = resolveSession.getStorageManager();
        this.annotations = storageManager.createLazyValue(new Function0<List<AnnotationDescriptor>>() {
            @Override
            public List<AnnotationDescriptor> invoke() {
                return resolveAnnotations();
            }
        });
        this.classObjectDescriptor = storageManager.createNullableLazyValue(new Function0<ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke() {
                return computeClassObjectDescriptor();
            }
        });
        this.scopeForClassHeaderResolution = storageManager.createLazyValue(new Function0<JetScope>() {
            @Override
            public JetScope invoke() {
                return computeScopeForClassHeaderResolution();
            }
        });
        this.scopeForMemberDeclarationResolution = storageManager.createLazyValue(new Function0<JetScope>() {
            @Override
            public JetScope invoke() {
                return computeScopeForMemberDeclarationResolution();
            }
        });
        this.scopeForPropertyInitializerResolution = storageManager.createLazyValue(new Function0<JetScope>() {
            @Override
            public JetScope invoke() {
                return computeScopeForPropertyInitializerResolution();
            }
        });
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberLookup() {
        return unsubstitutedMemberScope;
    }

    @NotNull
    public JetScope getScopeForClassHeaderResolution() {
        return scopeForClassHeaderResolution.invoke();
    }

    @NotNull
    private JetScope computeScopeForClassHeaderResolution() {
        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Scope with type parameters for " + getName());
        for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
            scope.addClassifierDescriptor(typeParameterDescriptor);
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        PsiElement scopeAnchor = declarationProvider.getOwnerInfo().getScopeAnchor();

        return new ChainedScope(this, "ScopeForClassHeaderResolution: " + getName(),
                scope,
                getScopeProvider().getResolutionScopeForDeclaration(scopeAnchor));
    }

    @NotNull
    public JetScope getScopeForMemberDeclarationResolution() {
        return scopeForMemberDeclarationResolution.invoke();
    }

    @NotNull
    private JetScope computeScopeForMemberDeclarationResolution() {
        WritableScopeImpl thisScope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Scope with 'this' for " + getName());
        thisScope.addLabeledDeclaration(this);
        thisScope.changeLockLevel(WritableScope.LockLevel.READING);

        ClassDescriptor classObject = getClassObjectDescriptor();
        JetScope classObjectAdapterScope = (classObject != null) ? new ClassObjectMixinScope(classObject) : JetScope.EMPTY;

        return new ChainedScope(
                this,
                "ScopeForMemberDeclarationResolution: " + getName(),
                thisScope,
                getScopeForMemberLookup(),
                getScopeForClassHeaderResolution(),
                classObjectAdapterScope);
    }

    @NotNull
    public JetScope getScopeForPropertyInitializerResolution() {
        return scopeForPropertyInitializerResolution.invoke();
    }

    @NotNull
    private JetScope computeScopeForPropertyInitializerResolution() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor == null) return getScopeForMemberDeclarationResolution();

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Scope with constructor parameters in " + getName());
        for (ValueParameterDescriptor valueParameterDescriptor : primaryConstructor.getValueParameters()) {
            scope.addVariableDescriptor(valueParameterDescriptor);
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        return new ChainedScope(
                this,
                "ScopeForPropertyInitializerResolution: " + getName(),
                scope, getScopeForMemberDeclarationResolution());
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return unsubstitutedMemberScope.getConstructors();
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return unsubstitutedMemberScope.getPrimaryConstructor();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor.invoke();
    }

    @Nullable
    private ClassDescriptor computeClassObjectDescriptor() {
        JetClassObject classObject = declarationProvider.getOwnerInfo().getClassObject();

        JetClassLikeInfo classObjectInfo = getClassObjectInfo(classObject);
        if (classObjectInfo != null) {
            return new LazyClassDescriptor(resolveSession, this, getClassObjectName(getName()), classObjectInfo);
        }
        return null;
    }

    @Nullable
    public JetClassLikeInfo getClassObjectInfo(JetClassObject classObject) {
        if (classObject != null) {
            if (!DescriptorUtils.inStaticContext(this)) {
                return null;
            }
            JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
            if (objectDeclaration != null) {
                return JetClassInfoUtil.createClassLikeInfo(objectDeclaration);
            }
        }
        else if (getKind() == ClassKind.OBJECT || getKind() == ClassKind.ENUM_ENTRY || getKind() == ClassKind.ENUM_CLASS) {
            return new SyntheticClassObjectInfo(originalClassInfo, this);
        }
        return null;
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

    @Override
    public boolean isInner() {
        return isInner;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations.invoke();
    }

    @NotNull
    private List<AnnotationDescriptor> resolveAnnotations() {
        JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
        JetModifierList modifierList = classInfo.getModifierList();
        if (modifierList != null) {
            AnnotationResolver annotationResolver = resolveSession.getInjector().getAnnotationResolver();
            JetScope scopeForDeclaration = getScopeProvider().getResolutionScopeForDeclaration(classInfo.getScopeAnchor());
            return annotationResolver.resolveAnnotationsWithArguments(scopeForDeclaration, modifierList, resolveSession.getTrace());
        }
        else {
            return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        // not using descriptor render to preserve laziness
        return "lazy class " + getName().toString();
    }

    @Override
    public void forceResolveAllContents() {
        getAnnotations();
        getClassObjectDescriptor();
        getClassObjectType();
        getConstructors();
        getContainingDeclaration();
        getThisAsReceiverParameter();
        getKind();
        getModality();
        getName();
        getOriginal();
        getScopeForClassHeaderResolution();
        getScopeForMemberDeclarationResolution();
        ForceResolveUtil.forceResolveAllContents(getScopeForMemberLookup());
        getScopeForPropertyInitializerResolution();
        getUnsubstitutedInnerClassesScope();
        ForceResolveUtil.forceResolveAllContents(getTypeConstructor());
        getUnsubstitutedPrimaryConstructor();
        getVisibility();
    }

    private class LazyClassTypeConstructor implements LazyDescriptor, TypeConstructor {
        private final NotNullLazyValue<Collection<JetType>> supertypes = resolveSession.getStorageManager().createLazyValueWithPostCompute(
                new Function0<Collection<JetType>>() {
                    @Override
                    public Collection<JetType> invoke() {
                        if (resolveSession.isClassSpecial(DescriptorUtils.getFqName(LazyClassDescriptor.this))) {
                            return Collections.emptyList();
                        }

                        JetClassLikeInfo info = declarationProvider.getOwnerInfo();
                        if (info instanceof SyntheticClassObjectInfo) {
                            LazyClassDescriptor descriptor = ((SyntheticClassObjectInfo) info).getClassDescriptor();
                            if (descriptor.getKind().isSingleton()) {
                                return Collections.singleton(descriptor.getDefaultType());
                            }
                        }

                        JetClassOrObject classOrObject = info.getCorrespondingClassOrObject();
                        if (classOrObject == null) {
                            return Collections.emptyList();
                        }

                        List<JetType> allSupertypes = resolveSession.getInjector().getDescriptorResolver()
                                .resolveSupertypes(getScopeForClassHeaderResolution(), LazyClassDescriptor.this, classOrObject,
                                                   resolveSession.getTrace());

                        return Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE));
                    }
                },
                new Function1<Boolean, Collection<JetType>>() {
                    @Override
                    public Collection<JetType> invoke(Boolean firstTime) {
                        return Collections.emptyList();
                    }
                },
                new Function1<Collection<JetType>, Unit>() {
                    @Override
                    public Unit invoke(@NotNull Collection<JetType> supertypes) {
                        findAndDisconnectLoopsInTypeHierarchy(supertypes);
                        return Unit.VALUE;
                    }
                });

        private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters = resolveSession.getStorageManager().createLazyValue(new Function0<List<TypeParameterDescriptor>>() {
            @Override
            public List<TypeParameterDescriptor> invoke() {
                JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
                List<JetTypeParameter> typeParameters = classInfo.getTypeParameters();

                List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>(typeParameters.size());
                for (int i = 0; i < typeParameters.size(); i++) {
                    parameters.add(new LazyTypeParameterDescriptor(resolveSession, LazyClassDescriptor.this, typeParameters.get(i), i));
                }

                return parameters;
            }
        });

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters.invoke();
        }

        @NotNull
        @Override
        public Collection<JetType> getSupertypes() {
            return supertypes.invoke();
        }

        private void findAndDisconnectLoopsInTypeHierarchy(Collection<JetType> supertypes) {
            for (Iterator<JetType> iterator = supertypes.iterator(); iterator.hasNext(); ) {
                JetType supertype = iterator.next();
                if (isReachable(supertype.getConstructor(), this, new HashSet<TypeConstructor>())) {
                    iterator.remove();
                }
            }
        }

        private boolean isReachable(TypeConstructor from, TypeConstructor to, Set<TypeConstructor> visited) {
            if (!visited.add(from)) return false;
            for (JetType supertype : from.getSupertypes()) {
                TypeConstructor supertypeConstructor = supertype.getConstructor();
                if (supertypeConstructor == to) {
                    return true;
                }
                if (isReachable(supertypeConstructor, to, visited)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isFinal() {
            return !getModality().isOverridable();
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Override
        public ClassifierDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            return Collections.emptyList(); // TODO
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }

        @Override
        public void forceResolveAllContents() {
            getAnnotations();
            getSupertypes();
            getParameters();
        }
    }

    @NotNull
    private ScopeProvider getScopeProvider() {
        return resolveSession.getInjector().getScopeProvider();
    }
}
