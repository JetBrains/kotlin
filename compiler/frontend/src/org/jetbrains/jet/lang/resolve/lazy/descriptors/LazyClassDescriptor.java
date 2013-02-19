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
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.lazy.data.FilteringClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassInfoUtil;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.lazy.storage.NotNullLazyValue;
import org.jetbrains.jet.lang.resolve.lazy.storage.NullableLazyValue;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.*;

public class LazyClassDescriptor extends ClassDescriptorBase implements LazyDescriptor, ClassDescriptor {

    private static final Predicate<Object> ONLY_ENUM_ENTRIES = Predicates.instanceOf(JetEnumEntry.class);
    private static final Predicate<JetType> VALID_SUPERTYPE = new Predicate<JetType>() {
        @Override
        public boolean apply(JetType type) {
            assert !ErrorUtils.isErrorType(type) : "Error types must be filtered out in DescriptorResolver";
            return TypeUtils.getClassDescriptor(type) != null;
        }
    };
    private final ResolveSession resolveSession;
    private final JetClassLikeInfo originalClassInfo;
    private final ClassMemberDeclarationProvider declarationProvider;

    private final Name name;
    private final DeclarationDescriptor containingDeclaration;
    private final LazyClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final ClassKind kind;
    private final boolean isInner;

    private final NotNullLazyValue<ReceiverParameterDescriptor> thisAsReceiverParameter;
    private final NotNullLazyValue<List<AnnotationDescriptor>> annotations;
    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;

    private final LazyClassMemberScope unsubstitutedMemberScope;
    private final JetScope unsubstitutedInnerClassesScope;

    private final NotNullLazyValue<JetScope> scopeForClassHeaderResolution;
    private final NotNullLazyValue<JetScope> scopeForMemberDeclarationResolution;
    private final NotNullLazyValue<JetScope> scopeForPropertyInitializerResolution;


    public LazyClassDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull JetClassLikeInfo classLikeInfo
    ) {
        this.resolveSession = resolveSession;
        this.name = name;

        if (classLikeInfo.getCorrespondingClassOrObject() != null) {
            this.resolveSession.getTrace().record(BindingContext.CLASS, classLikeInfo.getCorrespondingClassOrObject(), this);
        }

        this.originalClassInfo = classLikeInfo;
        JetClassLikeInfo classLikeInfoForMembers =
                classLikeInfo.getClassKind() != ClassKind.ENUM_CLASS ? classLikeInfo : noEnumEntries(classLikeInfo);
        this.declarationProvider = resolveSession.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfoForMembers);
        this.containingDeclaration = containingDeclaration;
        this.unsubstitutedMemberScope = new LazyClassMemberScope(resolveSession, declarationProvider, this);
        this.unsubstitutedInnerClassesScope = new InnerClassesScopeWrapper(unsubstitutedMemberScope);

        this.typeConstructor = new LazyClassTypeConstructor();

        JetModifierList modifierList = classLikeInfo.getModifierList();
        this.kind = classLikeInfo.getClassKind();
        if (kind.isObject()) {
            this.modality = Modality.FINAL;
        }
        else {
            Modality defaultModality = kind == ClassKind.TRAIT ? Modality.ABSTRACT : Modality.FINAL;
            this.modality = resolveModalityFromModifiers(modifierList, defaultModality);
        }
        this.visibility = resolveVisibilityFromModifiers(modifierList, getDefaultClassVisibility(this));
        this.isInner = isInnerClass(modifierList);

        StorageManager storageManager = resolveSession.getStorageManager();
        this.thisAsReceiverParameter = storageManager.createLazyValue(new Computable<ReceiverParameterDescriptor>() {
            @Override
            public ReceiverParameterDescriptor compute() {
                return DescriptorResolver.createLazyReceiverParameterDescriptor(LazyClassDescriptor.this);
            }
        });
        this.annotations = storageManager.createLazyValue(new Computable<List<AnnotationDescriptor>>() {
            @Override
            public List<AnnotationDescriptor> compute() {
                return resolveAnnotations();
            }
        });
        this.classObjectDescriptor = storageManager.createNullableLazyValue(new Computable<ClassDescriptor>() {
            @Override
            public ClassDescriptor compute() {
                return computeClassObjectDescriptor();
            }
        });
        this.scopeForClassHeaderResolution = storageManager.createLazyValue(new Computable<JetScope>() {
            @Override
            public JetScope compute() {
                return computeScopeForClassHeaderResolution();
            }
        });
        this.scopeForMemberDeclarationResolution = storageManager.createLazyValue(new Computable<JetScope>() {
            @Override
            public JetScope compute() {
                return computeScopeForMemberDeclarationResolution();
            }
        });
        this.scopeForPropertyInitializerResolution = storageManager.createLazyValue(new Computable<JetScope>() {
            @Override
            public JetScope compute() {
                return computeScopeForPropertyInitializerResolution();
            }
        });
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
        return scopeForClassHeaderResolution.compute();
    }

    @NotNull
    private JetScope computeScopeForClassHeaderResolution() {
        WritableScopeImpl scope = new WritableScopeImpl(
                JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Class Header Resolution");
        for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
            scope.addClassifierDescriptor(typeParameterDescriptor);
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        PsiElement scopeAnchor = declarationProvider.getOwnerInfo().getScopeAnchor();
        return new ChainedScope(
                this,
                "ScopeForClassHeaderResolution: " + getName(),
                scope, getScopeProvider().getResolutionScopeForDeclaration(scopeAnchor));
    }

    @NotNull
    public JetScope getScopeForMemberDeclarationResolution() {
        return scopeForMemberDeclarationResolution.compute();
    }

    @NotNull
    private JetScope computeScopeForMemberDeclarationResolution() {
        WritableScopeImpl scope = new WritableScopeImpl(
                JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Member Declaration Resolution");
        scope.addLabeledDeclaration(this);
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        return new ChainedScope(
                this,
                "ScopeForMemberDeclarationResolution: " + getName(),
                scope, getScopeForMemberLookup(), getScopeForClassHeaderResolution());
    }

    @NotNull
    public JetScope getScopeForPropertyInitializerResolution() {
        return scopeForPropertyInitializerResolution.compute();
    }

    @NotNull
    private JetScope computeScopeForPropertyInitializerResolution() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor == null) return getScopeForMemberDeclarationResolution();

        WritableScopeImpl scope = new WritableScopeImpl(
                JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Property Initializer Resolution");

        List<ValueParameterDescriptor> parameters = primaryConstructor.getValueParameters();
        for (ValueParameterDescriptor valueParameterDescriptor : parameters) {
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
        return classObjectDescriptor.compute();
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
    private JetClassLikeInfo getClassObjectInfo(JetClassObject classObject) {
        if (classObject != null) {
            if (!DescriptorUtils.inStaticContext(this)) {
                return null;
            }
            JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
            if (objectDeclaration != null) {
                return JetClassInfoUtil.createClassLikeInfo(objectDeclaration);
            }
        }
        else {
            if (getKind() == ClassKind.ENUM_CLASS) {
                // Enum classes always have class objects, and enum constants are their members
                return enumClassObjectInfo(originalClassInfo);
            }
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
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return thisAsReceiverParameter.compute();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations.compute();
    }

    @NotNull
    private List<AnnotationDescriptor> resolveAnnotations() {
        JetClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
        JetModifierList modifierList = classInfo.getModifierList();
        if (modifierList != null) {
            AnnotationResolver annotationResolver = resolveSession.getInjector().getAnnotationResolver();
            JetScope scopeForDeclaration = getScopeProvider().getResolutionScopeForDeclaration(classInfo.getScopeAnchor());
            return annotationResolver.resolveAnnotations(scopeForDeclaration, modifierList, resolveSession.getTrace());
        }
        else {
            return Collections.emptyList();
        }
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
        isClassObjectAValue();
    }

    private class LazyClassTypeConstructor implements LazyDescriptor, TypeConstructor {
        private final NotNullLazyValue<Collection<JetType>> supertypes = resolveSession.getStorageManager().createLazyValueWithPostCompute(
                new Computable<Collection<JetType>>() {
                    @Override
                    public Collection<JetType> compute() {
                        if (resolveSession.isClassSpecial(DescriptorUtils.getFQName(LazyClassDescriptor.this))) {
                            return Collections.emptyList();
                        }
                        else {
                            JetClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
                            if (classOrObject == null) {
                                return Collections.emptyList();
                            }
                            else {
                                List<JetType> allSupertypes = resolveSession.getInjector().getDescriptorResolver()
                                        .resolveSupertypes(getScopeForClassHeaderResolution(),
                                                           LazyClassDescriptor.this, classOrObject,
                                                           resolveSession.getTrace());

                                return Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE));
                            }
                        }
                    }
                },
                new Consumer<Collection<JetType>>() {
                    @Override
                    public void consume(@NotNull Collection<JetType> supertypes) {
                        findAndDisconnectLoopsInTypeHierarchy(supertypes);
                    }
                });

        private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters = resolveSession.getStorageManager().createLazyValue(new Computable<List<TypeParameterDescriptor>>() {
            @Override
            public List<TypeParameterDescriptor> compute() {
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
            return parameters.compute();
        }

        @NotNull
        @Override
        public Collection<JetType> getSupertypes() {
            return supertypes.compute();
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

        @Override
        public void forceResolveAllContents() {
            getAnnotations();
            getSupertypes();
            getParameters();
        }
    }

    private JetClassLikeInfo noEnumEntries(JetClassLikeInfo classLikeInfo) {
        return new FilteringClassLikeInfo(resolveSession.getStorageManager(), classLikeInfo, Predicates.not(ONLY_ENUM_ENTRIES));
    }

    private JetClassLikeInfo enumClassObjectInfo(JetClassLikeInfo classLikeInfo) {
        return new FilteringClassLikeInfo(resolveSession.getStorageManager(), classLikeInfo, ONLY_ENUM_ENTRIES) {
            @Override
            public JetClassOrObject getCorrespondingClassOrObject() {
                return null;
            }

            @NotNull
            @Override
            public ClassKind getClassKind() {
                return ClassKind.CLASS_OBJECT;
            }

            @NotNull
            @Override
            public List<? extends JetParameter> getPrimaryConstructorParameters() {
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public List<JetTypeParameter> getTypeParameters() {
                return Collections.emptyList();
            }
        };
    }

    private ScopeProvider getScopeProvider() {
        return resolveSession.getInjector().getScopeProvider();
    }

}
