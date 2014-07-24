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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import kotlin.Unit;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.jet.lang.descriptors.impl.EnumClassObjectDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.TypeHierarchyResolver;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyEntity;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassInfoUtil;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.data.SyntheticClassObjectInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.types.AbstractClassTypeConstructor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.NullableLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.CLASS_OBJECT_NOT_ALLOWED;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isSyntheticClassObject;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.*;
import static org.jetbrains.jet.lang.resolve.name.SpecialNames.getClassObjectName;
import static org.jetbrains.jet.lang.resolve.source.SourcePackage.toSourceElement;

public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes, LazyEntity {
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

    private final Annotations annotations;
    private final NullableLazyValue<ClassDescriptor> classObjectDescriptor;
    private final MemoizedFunctionToNotNull<JetClassObject, ClassDescriptor> extraClassObjectDescriptors;

    private final LazyClassMemberScope unsubstitutedMemberScope;

    private final NotNullLazyValue<JetScope> scopeForClassHeaderResolution;
    private final NotNullLazyValue<JetScope> scopeForMemberDeclarationResolution;
    private final NotNullLazyValue<JetScope> scopeForPropertyInitializerResolution;

    private final NullableLazyValue<Void> forceResolveAllContents;

    public LazyClassDescriptor(
            @NotNull final ResolveSession resolveSession,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull JetClassLikeInfo classLikeInfo
    ) {
        super(resolveSession.getStorageManager(), containingDeclaration, name,
              toSourceElement(classLikeInfo.getCorrespondingClassOrObject())
        );
        this.resolveSession = resolveSession;

        if (classLikeInfo.getCorrespondingClassOrObject() != null) {
            this.resolveSession.getTrace().record(BindingContext.CLASS, classLikeInfo.getCorrespondingClassOrObject(), this);
        }
        this.resolveSession.getTrace().record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this);

        this.originalClassInfo = classLikeInfo;
        this.declarationProvider = resolveSession.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);

        this.unsubstitutedMemberScope = createMemberScope(resolveSession, this.declarationProvider);

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


        if (modifierList != null) {
            this.annotations = new LazyAnnotations(
                    new LazyAnnotationsContext(
                            resolveSession.getAnnotationResolver(),
                            resolveSession.getStorageManager(),
                            resolveSession.getTrace()
                    ) {
                        @NotNull
                        @Override
                        public JetScope getScope() {
                            JetClassLikeInfo ownerInfo = declarationProvider.getOwnerInfo();
                            return resolveSession.getScopeProvider().getResolutionScopeForDeclaration(ownerInfo.getScopeAnchor());
                        }
                    },
                    modifierList.getAnnotationEntries()
                );

        }
        else {
            this.annotations = Annotations.EMPTY;
        }

        this.classObjectDescriptor = storageManager.createNullableLazyValue(new Function0<ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke() {
                return computeClassObjectDescriptor(declarationProvider.getOwnerInfo().getClassObject());
            }
        });
        this.extraClassObjectDescriptors = storageManager.createMemoizedFunction(new Function1<JetClassObject, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(JetClassObject classObject) {
                return computeClassObjectDescriptor(classObject);
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
        this.forceResolveAllContents = storageManager.createRecursionTolerantNullableLazyValue(new Function0<Void>() {
            @Override
            public Void invoke() {
                doForceResolveAllContents();
                return null;
            }
        }, null);
    }

    // NOTE: Called from constructor!
    @NotNull
    protected LazyClassMemberScope createMemberScope(
            @NotNull ResolveSession resolveSession,
            @NotNull ClassMemberDeclarationProvider declarationProvider
    ) {
        return new LazyClassMemberScope(resolveSession, declarationProvider, this, resolveSession.getTrace());
    }

    @NotNull
    @Override
    public JetScope getScopeForMemberLookup() {
        return unsubstitutedMemberScope;
    }

    @Override
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

    @Override
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

    @Override
    @NotNull
    public JetScope getScopeForInitializerResolution() {
        return scopeForPropertyInitializerResolution.invoke();
    }

    @NotNull
    @Override
    public Collection<CallableMemberDescriptor> getDeclaredCallableMembers() {
        //noinspection unchecked
        return (Collection) KotlinPackage.filter(
                unsubstitutedMemberScope.getAllDescriptors(),
                new Function1<DeclarationDescriptor, Boolean>() {
                    @Override
                    public Boolean invoke(DeclarationDescriptor descriptor) {
                        return descriptor instanceof CallableMemberDescriptor
                               && ((CallableMemberDescriptor) descriptor).getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
                    }
                }
        );
    }

    @NotNull
    private JetScope computeScopeForPropertyInitializerResolution() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor == null) return getScopeForMemberDeclarationResolution();

        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, primaryConstructor, RedeclarationHandler.DO_NOTHING, "Scope with constructor parameters in " + getName());
        for (int i = 0; i < originalClassInfo.getPrimaryConstructorParameters().size(); i++) {
            JetParameter jetParameter = originalClassInfo.getPrimaryConstructorParameters().get(i);
            if (!jetParameter.hasValOrVarNode()) {
                scope.addVariableDescriptor(primaryConstructor.getValueParameters().get(i));
            }
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        return new ChainedScope(
                primaryConstructor,
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

    @NotNull
    @ReadOnly
    public List<ClassDescriptor> getDescriptorsForExtraClassObjects() {
        return KotlinPackage.map(
                KotlinPackage.filter(
                        declarationProvider.getOwnerInfo().getClassObjects(),
                        new Function1<JetClassObject, Boolean>() {
                            @Override
                            public Boolean invoke(JetClassObject classObject) {
                                return classObject != declarationProvider.getOwnerInfo().getClassObject();
                            }
                        }
                ),
                new Function1<JetClassObject, ClassDescriptor>() {
                    @Override
                    public ClassDescriptor invoke(JetClassObject classObject) {
                        return extraClassObjectDescriptors.invoke(classObject);
                    }
                }
        );
    }

    @Nullable
    private ClassDescriptor computeClassObjectDescriptor(@Nullable JetClassObject classObject) {
        if (getKind() == ClassKind.ENUM_CLASS) {
            return new EnumClassObjectDescriptor(resolveSession.getStorageManager(), this);
        }
        JetClassLikeInfo classObjectInfo = getClassObjectInfo(classObject);
        if (classObjectInfo != null) {
            return new LazyClassDescriptor(resolveSession, this, getClassObjectName(getName()), classObjectInfo);
        }
        return null;
    }

    @Nullable
    private JetClassLikeInfo getClassObjectInfo(@Nullable JetClassObject classObject) {
        if (classObject != null) {
            if (getKind() != ClassKind.CLASS && getKind() != ClassKind.TRAIT && getKind() != ClassKind.ANNOTATION_CLASS || isInner()) {
                resolveSession.getTrace().report(CLASS_OBJECT_NOT_ALLOWED.on(classObject));
            }

            JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
            return JetClassInfoUtil.createClassLikeInfo(objectDeclaration);
        }
        else if (getKind() == ClassKind.OBJECT || getKind() == ClassKind.ENUM_ENTRY) {
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
    public Annotations getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        // not using descriptor render to preserve laziness
        return "lazy class " + getName().toString();
    }

    @Override
    public void forceResolveAllContents() {
        forceResolveAllContents.invoke();
    }

    private void doForceResolveAllContents() {
        resolveMemberHeaders();
        ClassDescriptor classObjectDescriptor = getClassObjectDescriptor();
        if (classObjectDescriptor != null) {
            ForceResolveUtil.forceResolveAllContents(classObjectDescriptor);
        }

        ForceResolveUtil.forceResolveAllContents(getConstructors());
        ForceResolveUtil.forceResolveAllContents(getDescriptorsForExtraClassObjects());
        ForceResolveUtil.forceResolveAllContents(getScopeForMemberLookup());
        ForceResolveUtil.forceResolveAllContents(getTypeConstructor());
    }

    // Note: headers of member classes' members are not resolved
    public void resolveMemberHeaders() {
        ForceResolveUtil.forceResolveAllContents(getAnnotations());

        getClassObjectDescriptor();

        getDescriptorsForExtraClassObjects();

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
        getScopeForMemberLookup().getAllDescriptors();
        getScopeForInitializerResolution();
        getUnsubstitutedInnerClassesScope();
        getTypeConstructor().getSupertypes();
        for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
            typeParameterDescriptor.getUpperBounds();
            typeParameterDescriptor.getLowerBounds();
        }
        getUnsubstitutedPrimaryConstructor();
        getVisibility();
    }

    private static class Supertypes {
        @Mutable
        public final Collection<JetType> trueSupertypes;
        @Mutable
        public final Collection<JetType> cyclicSupertypes;

        private Supertypes(@Mutable @NotNull Collection<JetType> trueSupertypes) {
            this(trueSupertypes, new ArrayList<JetType>(0));
        }

        private Supertypes(@Mutable @NotNull Collection<JetType> trueSupertypes, @Mutable @NotNull Collection<JetType> cyclicSupertypes) {
            this.trueSupertypes = trueSupertypes;
            this.cyclicSupertypes = cyclicSupertypes;
        }

        @NotNull
        public Collection<JetType> getAllSupertypes() {
            return KotlinPackage.plus(trueSupertypes, cyclicSupertypes);
        }
    }

    private class LazyClassTypeConstructor extends AbstractClassTypeConstructor implements LazyEntity {
        private final NotNullLazyValue<Supertypes> supertypes = resolveSession.getStorageManager().createLazyValueWithPostCompute(
                new Function0<Supertypes>() {
                    @Override
                    public Supertypes invoke() {
                        if (KotlinBuiltIns.getInstance().isSpecialClassWithNoSupertypes(LazyClassDescriptor.this)) {
                            return new Supertypes(Collections.<JetType>emptyList());
                        }

                        JetClassLikeInfo info = declarationProvider.getOwnerInfo();
                        if (info instanceof SyntheticClassObjectInfo) {
                            LazyClassDescriptor descriptor = ((SyntheticClassObjectInfo) info).getClassDescriptor();
                            if (descriptor.getKind().isSingleton()) {
                                return new Supertypes(Collections.singleton(descriptor.getDefaultType()));
                            }
                        }

                        JetClassOrObject classOrObject = info.getCorrespondingClassOrObject();
                        if (classOrObject == null) {
                            return new Supertypes(Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()));
                        }

                        List<JetType> allSupertypes = resolveSession.getDescriptorResolver()
                                .resolveSupertypes(getScopeForClassHeaderResolution(), LazyClassDescriptor.this, classOrObject,
                                                   resolveSession.getTrace());

                        return new Supertypes(Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE)));
                    }
                },
                new Function1<Boolean, Supertypes>() {
                    @Override
                    public Supertypes invoke(Boolean firstTime) {
                        return new Supertypes(Collections.<JetType>emptyList());
                    }
                },
                new Function1<Supertypes, Unit>() {
                    @Override
                    public Unit invoke(@NotNull Supertypes supertypes) {
                        findAndDisconnectLoopsInTypeHierarchy(supertypes);
                        return Unit.VALUE;
                    }
                }
        );

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

        private final NullableLazyValue<Void> forceResolveAllContents =
                resolveSession.getStorageManager().createRecursionTolerantNullableLazyValue(new Function0<Void>() {
                    @Override
                    public Void invoke() {
                        doForceResolveAllContents();
                        return null;
                    }
                }, null);

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters.invoke();
        }

        @NotNull
        @Override
        public Collection<JetType> getSupertypes() {
            return supertypes.invoke().trueSupertypes;
        }

        private void findAndDisconnectLoopsInTypeHierarchy(Supertypes supertypes) {
            for (Iterator<JetType> iterator = supertypes.trueSupertypes.iterator(); iterator.hasNext(); ) {
                JetType supertype = iterator.next();
                if (isReachable(supertype.getConstructor(), this, new HashSet<TypeConstructor>())) {
                    iterator.remove();
                    supertypes.cyclicSupertypes.add(supertype);

                    ClassifierDescriptor supertypeDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                    if (supertypeDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor superclass = (ClassDescriptor) supertypeDescriptor;
                        TypeHierarchyResolver.reportCyclicInheritanceHierarchyError(resolveSession.getTrace(), LazyClassDescriptor.this,
                                                                                    superclass);
                    }
                }
            }
        }

        private boolean isReachable(TypeConstructor from, TypeConstructor to, Set<TypeConstructor> visited) {
            if (!visited.add(from)) return false;
            for (JetType supertype : getNeighbors(from)) {
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

        private Collection<JetType> getNeighbors(TypeConstructor from) {
            // Supertypes + type for container
            Collection<JetType> neighbours = new ArrayList<JetType>(
                    from instanceof LazyClassTypeConstructor
                             ? ((LazyClassTypeConstructor) from).supertypes.invoke().getAllSupertypes()
                             : from.getSupertypes()
            );

            ClassifierDescriptor fromDescriptor = from.getDeclarationDescriptor();
            if (fromDescriptor != null) {
                DeclarationDescriptor container = fromDescriptor.getContainingDeclaration();
                if (container instanceof ClassDescriptor) {
                    neighbours.add(((ClassDescriptor) container).getDefaultType());
                }
            }
            return neighbours;
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
        public Annotations getAnnotations() {
            return Annotations.EMPTY; // TODO
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }

        @Override
        public void forceResolveAllContents() {
            forceResolveAllContents.invoke();
        }

        private void doForceResolveAllContents() {
            ForceResolveUtil.forceResolveAllContents(getAnnotations());
            ForceResolveUtil.forceResolveAllContents(getSupertypes());
            ForceResolveUtil.forceResolveAllContents(getParameters());
        }
    }

    @NotNull
    private ScopeProvider getScopeProvider() {
        return resolveSession.getScopeProvider();
    }
}
