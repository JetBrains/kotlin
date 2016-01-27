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

package org.jetbrains.kotlin.resolve.lazy.descriptors;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import kotlin.collections.CollectionsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassInfoUtil;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassOrObjectInfo;
import org.jetbrains.kotlin.resolve.lazy.data.KtObjectInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.NullableLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static kotlin.collections.CollectionsKt.firstOrNull;
import static org.jetbrains.kotlin.diagnostics.Errors.CYCLIC_INHERITANCE_HIERARCHY;
import static org.jetbrains.kotlin.diagnostics.Errors.TYPE_PARAMETERS_IN_ENUM;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.*;

public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes, LazyEntity {
    private static final Predicate<KotlinType> VALID_SUPERTYPE = new Predicate<KotlinType>() {
        @Override
        public boolean apply(KotlinType type) {
            assert !type.isError() : "Error types must be filtered out in DescriptorResolver";
            return TypeUtils.getClassDescriptor(type) != null;
        }
    };
    private final LazyClassContext c;

    private final ClassMemberDeclarationProvider declarationProvider;

    private final LazyClassTypeConstructor typeConstructor;
    private final Modality modality;
    private final Visibility visibility;
    private final NotNullLazyValue<ClassKind> kind;
    private final NotNullLazyValue<Boolean> isInner;
    private final NotNullLazyValue<Boolean> isData;

    private final Annotations annotations;
    private final Annotations danglingAnnotations;
    private final NullableLazyValue<LazyClassDescriptor> companionObjectDescriptor;
    private final MemoizedFunctionToNotNull<KtObjectDeclaration, ClassDescriptor> extraCompanionObjectDescriptors;

    private final LazyClassMemberScope unsubstitutedMemberScope;
    private final MemberScope staticScope;

    private final NullableLazyValue<Void> forceResolveAllContents;
    private final boolean isCompanionObject;

    private final ClassResolutionScopesSupport resolutionScopesSupport;
    private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters;

    private final NotNullLazyValue<LexicalScope> scopeForInitializerResolution;

    public LazyClassDescriptor(
            @NotNull final LazyClassContext c,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull final KtClassLikeInfo classLikeInfo
    ) {
        super(c.getStorageManager(), containingDeclaration, name,
              KotlinSourceElementKt.toSourceElement(classLikeInfo.getCorrespondingClassOrObject())
        );
        this.c = c;

        KtClassOrObject classOrObject = classLikeInfo.getCorrespondingClassOrObject();
        if (classOrObject != null) {
            this.c.getTrace().record(BindingContext.CLASS, classOrObject, this);
        }
        this.c.getTrace().record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this);

        this.declarationProvider = c.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);

        this.unsubstitutedMemberScope = createMemberScope(c, this.declarationProvider);
        this.staticScope = new StaticScopeForKotlinClass(this);

        this.typeConstructor = new LazyClassTypeConstructor();

        final ClassKind syntaxKind = classLikeInfo.getClassKind();
        this.isCompanionObject = classLikeInfo instanceof KtObjectInfo && ((KtObjectInfo) classLikeInfo).isCompanionObject();

        final KtModifierList modifierList = classLikeInfo.getModifierList();
        if (syntaxKind.isSingleton()) {
            this.modality = Modality.FINAL;
        }
        else {
            Modality defaultModality = syntaxKind == ClassKind.INTERFACE ? Modality.ABSTRACT : Modality.FINAL;
            this.modality = resolveModalityFromModifiers(modifierList, defaultModality);
        }

        boolean isLocal = classOrObject != null && KtPsiUtil.isLocal(classOrObject);
        Visibility defaultVisibility;
        if (syntaxKind == ClassKind.ENUM_ENTRY || (syntaxKind == ClassKind.OBJECT && isCompanionObject)) {
            defaultVisibility = Visibilities.PUBLIC;
        }
        else {
            defaultVisibility = Visibilities.DEFAULT_VISIBILITY;
        }
        this.visibility = isLocal ? Visibilities.LOCAL : resolveVisibilityFromModifiers(modifierList, defaultVisibility);

        StorageManager storageManager = c.getStorageManager();
        final ClassDescriptor descriptor = this;

        this.isInner = storageManager.createLazyValue(new Function0<Boolean>() {
            @Override
            public Boolean invoke() {
                return isInnerClass(modifierList) && !ModifiersChecker.isIllegalInner(descriptor);
            }
        });

        this.isData = storageManager.createLazyValue(new Function0<Boolean>() {
            @Override
            public Boolean invoke() {
                return modifierList != null && modifierList.hasModifier(KtTokens.DATA_KEYWORD);
            }
        });

        this.kind = storageManager.createLazyValue(new Function0<ClassKind>() {
            @Override
            public ClassKind invoke() {
                return (syntaxKind == ClassKind.CLASS && modifierList != null && modifierList.hasModifier(KtTokens.ANNOTATION_KEYWORD)) ? ClassKind.ANNOTATION_CLASS : syntaxKind;
            }
        });

        if (modifierList != null) {
            this.annotations = new LazyAnnotations(
                    new LazyAnnotationsContext(
                            c.getAnnotationResolver(),
                            storageManager,
                            c.getTrace()
                    ) {
                        @NotNull
                        @Override
                        public LexicalScope getScope() {
                            return getOuterScope();
                        }
                    },
                    modifierList.getAnnotationEntries()
            );
        }
        else {
            this.annotations = Annotations.Companion.getEMPTY();
        }

        List<KtAnnotationEntry> jetDanglingAnnotations = classLikeInfo.getDanglingAnnotations();
        if (jetDanglingAnnotations.isEmpty()) {
            this.danglingAnnotations = Annotations.Companion.getEMPTY();
        }
        else {
            this.danglingAnnotations = new LazyAnnotations(
                    new LazyAnnotationsContext(
                            c.getAnnotationResolver(),
                            storageManager,
                            c.getTrace()
                    ) {
                        @NotNull
                        @Override
                        public LexicalScope getScope() {
                            return getScopeForMemberDeclarationResolution();
                        }
                    },
                    jetDanglingAnnotations
            );
        }

        this.companionObjectDescriptor = storageManager.createNullableLazyValue(new Function0<LazyClassDescriptor>() {
            @Override
            public LazyClassDescriptor invoke() {
                return computeCompanionObjectDescriptor(getCompanionObjectIfAllowed());
            }
        });
        this.extraCompanionObjectDescriptors = storageManager.createMemoizedFunction(new Function1<KtObjectDeclaration, ClassDescriptor>() {
            @Override
            public ClassDescriptor invoke(KtObjectDeclaration companionObject) {
                return computeCompanionObjectDescriptor(companionObject);
            }
        });
        this.forceResolveAllContents = storageManager.createRecursionTolerantNullableLazyValue(new Function0<Void>() {
            @Override
            public Void invoke() {
                doForceResolveAllContents();
                return null;
            }
        }, null);

        this.resolutionScopesSupport = new ClassResolutionScopesSupport(this, storageManager, new Function0<LexicalScope>() {
            @Override
            public LexicalScope invoke() {
                return getOuterScope();
            }
        });

        this.parameters = c.getStorageManager().createLazyValue(new Function0<List<TypeParameterDescriptor>>() {
            @Override
            public List<TypeParameterDescriptor> invoke() {
                KtClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
                KtTypeParameterList typeParameterList = classInfo.getTypeParameterList();
                if (typeParameterList == null) return Collections.emptyList();

                if (classInfo.getClassKind() == ClassKind.ENUM_CLASS) {
                    c.getTrace().report(TYPE_PARAMETERS_IN_ENUM.on(typeParameterList));
                }

                List<KtTypeParameter> typeParameters = typeParameterList.getParameters();

                List<TypeParameterDescriptor> parameters = new ArrayList<TypeParameterDescriptor>(typeParameters.size());

                for (int i = 0; i < typeParameters.size(); i++) {
                    parameters.add(new LazyTypeParameterDescriptor(c, LazyClassDescriptor.this, typeParameters.get(i), i));
                }

                return parameters;
            }
        });

        this.scopeForInitializerResolution = storageManager.createLazyValue(new Function0<LexicalScope>() {
            @Override
            public LexicalScope invoke() {
                return ClassResolutionScopesSupportKt.scopeForInitializerResolution(LazyClassDescriptor.this,
                                                                                    createInitializerScopeParent(),
                                                                                    classLikeInfo.getPrimaryConstructorParameters());
            }
        });
    }

    @NotNull
    private DeclarationDescriptor createInitializerScopeParent() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor != null) return primaryConstructor;

        return new FunctionDescriptorImpl(LazyClassDescriptor.this, null, Annotations.Companion.getEMPTY(),
                                          Name.special("<init-blocks>"),
                                          CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE) {
            @NotNull
            @Override
            protected FunctionDescriptorImpl createSubstitutedCopy(
                    @NotNull DeclarationDescriptor newOwner,
                    @Nullable FunctionDescriptor original,
                    @NotNull Kind kind,
                    @Nullable Name newName,
                    boolean preserveSource
            ) {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public FunctionDescriptor copy(
                    DeclarationDescriptor newOwner,
                    Modality modality,
                    Visibility visibility,
                    Kind kind,
                    boolean copyOverrides
            ) {
                throw new UnsupportedOperationException();
            }
        };
    }

    // NOTE: Called from constructor!
    @NotNull
    protected LazyClassMemberScope createMemberScope(
            @NotNull LazyClassContext c,
            @NotNull ClassMemberDeclarationProvider declarationProvider
    ) {
        return new LazyClassMemberScope(c, declarationProvider, this, c.getTrace());
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope() {
        return unsubstitutedMemberScope;
    }

    @NotNull
    protected LexicalScope getOuterScope() {
        return c.getDeclarationScopeProvider().getResolutionScopeForDeclaration(declarationProvider.getOwnerInfo().getScopeAnchor());
    }

    @Override
    @NotNull
    public LexicalScope getScopeForClassHeaderResolution() {
        return resolutionScopesSupport.getScopeForClassHeaderResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForConstructorHeaderResolution() {
        return resolutionScopesSupport.getScopeForConstructorHeaderResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForCompanionObjectHeaderResolution() {
        return resolutionScopesSupport.getScopeForCompanionObjectHeaderResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForMemberDeclarationResolution() {
        return resolutionScopesSupport.getScopeForMemberDeclarationResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForStaticMemberDeclarationResolution() {
        return resolutionScopesSupport.getScopeForStaticMemberDeclarationResolution().invoke();
    }

    @Override
    @NotNull
    public LexicalScope getScopeForInitializerResolution() {
        return scopeForInitializerResolution.invoke();
    }

    @NotNull
    @Override
    public Collection<CallableMemberDescriptor> getDeclaredCallableMembers() {
        //noinspection unchecked
        return (Collection) CollectionsKt.filter(
                DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope),
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
    @Override
    public MemberScope getStaticScope() {
        return staticScope;
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
    public LazyClassDescriptor getCompanionObjectDescriptor() {
        return companionObjectDescriptor.invoke();
    }

    @NotNull
    @ReadOnly
    public List<ClassDescriptor> getDescriptorsForExtraCompanionObjects() {
        final KtObjectDeclaration allowedCompanionObject = getCompanionObjectIfAllowed();

        return CollectionsKt.map(
                CollectionsKt.filter(
                        declarationProvider.getOwnerInfo().getCompanionObjects(),
                        new Function1<KtObjectDeclaration, Boolean>() {
                            @Override
                            public Boolean invoke(KtObjectDeclaration companionObject) {
                                return companionObject != allowedCompanionObject;
                            }
                        }
                ),
                new Function1<KtObjectDeclaration, ClassDescriptor>() {
                    @Override
                    public ClassDescriptor invoke(KtObjectDeclaration companionObject) {
                        return extraCompanionObjectDescriptors.invoke(companionObject);
                    }
                }
        );
    }

    @Nullable
    private LazyClassDescriptor computeCompanionObjectDescriptor(@Nullable KtObjectDeclaration companionObject) {
        KtClassLikeInfo companionObjectInfo = getCompanionObjectInfo(companionObject);
        if (!(companionObjectInfo instanceof KtClassOrObjectInfo)) {
            return null;
        }
        Name name = ((KtClassOrObjectInfo) companionObjectInfo).getName();
        assert name != null;
        getUnsubstitutedMemberScope().getContributedClassifier(name, NoLookupLocation.WHEN_GET_COMPANION_OBJECT);
        ClassDescriptor companionObjectDescriptor = c.getTrace().get(BindingContext.CLASS, companionObject);
        if (companionObjectDescriptor instanceof LazyClassDescriptor) {
            assert DescriptorUtils.isCompanionObject(companionObjectDescriptor) : "Not a companion object: " + companionObjectDescriptor;
            return (LazyClassDescriptor) companionObjectDescriptor;
        }
        else {
            return null;
        }
    }

    @Nullable
    private static KtClassLikeInfo getCompanionObjectInfo(@Nullable KtObjectDeclaration companionObject) {
        if (companionObject != null) {
            return KtClassInfoUtil.createClassLikeInfo(companionObject);
        }

        return null;
    }

    @Nullable
    private KtObjectDeclaration getCompanionObjectIfAllowed() {
        KtObjectDeclaration companionObject = firstOrNull(declarationProvider.getOwnerInfo().getCompanionObjects());
        return (companionObject != null && isCompanionObjectAllowed()) ? companionObject : null;
    }

    private boolean isCompanionObjectAllowed() {
        return !(getKind().isSingleton() || isInner() || DescriptorUtils.isLocal(this));
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind.invoke();
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
        return isInner.invoke();
    }

    @Override
    public boolean isData() {
        return isData.invoke();
    }

    @Override
    public boolean isCompanionObject() {
        return isCompanionObject;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return annotations;
    }

    @NotNull
    public Annotations getDanglingAnnotations() {
        return danglingAnnotations;
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
        ClassDescriptor companionObjectDescriptor = getCompanionObjectDescriptor();
        if (companionObjectDescriptor != null) {
            ForceResolveUtil.forceResolveAllContents(companionObjectDescriptor);
        }

        ForceResolveUtil.forceResolveAllContents(getConstructors());
        ForceResolveUtil.forceResolveAllContents(getDescriptorsForExtraCompanionObjects());
        ForceResolveUtil.forceResolveAllContents(getUnsubstitutedMemberScope());
        ForceResolveUtil.forceResolveAllContents(getTypeConstructor());
    }

    // Note: headers of member classes' members are not resolved
    public void resolveMemberHeaders() {
        ForceResolveUtil.forceResolveAllContents(getAnnotations());
        ForceResolveUtil.forceResolveAllContents(getDanglingAnnotations());

        getCompanionObjectDescriptor();

        getDescriptorsForExtraCompanionObjects();

        getConstructors();
        getContainingDeclaration();
        getThisAsReceiverParameter();
        getKind();
        getModality();
        getName();
        getOriginal();
        getScopeForClassHeaderResolution();
        getScopeForMemberDeclarationResolution();
        DescriptorUtils.getAllDescriptors(getUnsubstitutedMemberScope());
        getScopeForInitializerResolution();
        getUnsubstitutedInnerClassesScope();
        getTypeConstructor().getSupertypes();
        for (TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters()) {
            typeParameterDescriptor.getUpperBounds();
        }
        getUnsubstitutedPrimaryConstructor();
        getVisibility();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getDeclaredTypeParameters() {
        return parameters.invoke();
    }

    private static class Supertypes {
        @Mutable
        public final Collection<KotlinType> trueSupertypes;
        @Mutable
        public final Collection<KotlinType> allSuperTypes;

        private Supertypes(@Mutable @NotNull Collection<KotlinType> allSuperTypes) {
            this.trueSupertypes = allSuperTypes;
            this.allSuperTypes = new ArrayList<KotlinType>(allSuperTypes);
        }

        @NotNull
        public Collection<KotlinType> getAllSupertypes() {
            return allSuperTypes;
        }
    }

    private class LazyClassTypeConstructor extends AbstractClassTypeConstructor implements LazyEntity {
        private final NotNullLazyValue<Supertypes> supertypes = c.getStorageManager().createLazyValueWithPostCompute(
                new Function0<Supertypes>() {
                    @Override
                    public Supertypes invoke() {
                        if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(LazyClassDescriptor.this)) {
                            return new Supertypes(Collections.<KotlinType>emptyList());
                        }

                        KtClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
                        if (classOrObject == null) {
                            return new Supertypes(Collections.singleton(c.getModuleDescriptor().getBuiltIns().getAnyType()));
                        }

                        List<KotlinType> allSupertypes = c.getDescriptorResolver()
                                .resolveSupertypes(getScopeForClassHeaderResolution(), LazyClassDescriptor.this, classOrObject,
                                                   c.getTrace());

                        return new Supertypes(Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE)));
                    }
                },
                new Function1<Boolean, Supertypes>() {
                    @Override
                    public Supertypes invoke(Boolean firstTime) {
                        return new Supertypes(Collections.<KotlinType>emptyList());
                    }
                },
                new Function1<Supertypes, Unit>() {
                    @Override
                    public Unit invoke(@NotNull Supertypes supertypes) {
                        findAndDisconnectLoopsInTypeHierarchy(supertypes.trueSupertypes);
                        return Unit.INSTANCE;
                    }
                }
        );

        private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters = c.getStorageManager().createLazyValue(new Function0<List<TypeParameterDescriptor>>() {
            @Override
            public List<TypeParameterDescriptor> invoke() {
                return TypeParameterUtilsKt.computeConstructorTypeParameters(LazyClassDescriptor.this);
            }
        });

        private final NullableLazyValue<Void> forceResolveAllContents =
                c.getStorageManager().createRecursionTolerantNullableLazyValue(new Function0<Void>() {
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
        public Collection<KotlinType> getSupertypes() {
            return supertypes.invoke().trueSupertypes;
        }

        private void findAndDisconnectLoopsInTypeHierarchy(@Mutable Collection<KotlinType> supertypes) {
            c.getSupertypeLoopChecker().findLoopsInSupertypesAndDisconnect(
                    typeConstructor, supertypes,
                    new Function1<TypeConstructor, Iterable<? extends KotlinType>>() {
                        @Override
                        public Iterable<? extends KotlinType> invoke(TypeConstructor typeConstructor) {
                            return getNeighbors(typeConstructor);
                        }
                    },
                    new Function1<KotlinType, Unit>() {
                        @Override
                        public Unit invoke(KotlinType type) {
                            ClassifierDescriptor supertypeDescriptor = type.getConstructor().getDeclarationDescriptor();
                            if (supertypeDescriptor instanceof ClassDescriptor) {
                                ClassDescriptor superclass = (ClassDescriptor) supertypeDescriptor;
                                reportCyclicInheritanceHierarchyError(c.getTrace(), LazyClassDescriptor.this, superclass);
                            }

                            return Unit.INSTANCE;
                        }
                    }
            );
        }

        private void reportCyclicInheritanceHierarchyError(
                @NotNull BindingTrace trace,
                @NotNull ClassDescriptor classDescriptor,
                @NotNull ClassDescriptor superclass
        ) {
            PsiElement psiElement = DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor);

            PsiElement elementToMark = null;
            if (psiElement instanceof KtClassOrObject) {
                KtClassOrObject classOrObject = (KtClassOrObject) psiElement;
                for (KtSuperTypeListEntry delegationSpecifier : classOrObject.getSuperTypeListEntries()) {
                    KtTypeReference typeReference = delegationSpecifier.getTypeReference();
                    if (typeReference == null) continue;
                    KotlinType supertype = trace.get(TYPE, typeReference);
                    if (supertype != null && supertype.getConstructor() == superclass.getTypeConstructor()) {
                        elementToMark = typeReference;
                    }
                }
            }
            if (elementToMark == null && psiElement instanceof PsiNameIdentifierOwner) {
                PsiNameIdentifierOwner namedElement = (PsiNameIdentifierOwner) psiElement;
                PsiElement nameIdentifier = namedElement.getNameIdentifier();
                if (nameIdentifier != null) {
                    elementToMark = nameIdentifier;
                }
            }
            if (elementToMark != null) {
                trace.report(CYCLIC_INHERITANCE_HIERARCHY.on(elementToMark));
            }
        }

        private Collection<KotlinType> getNeighbors(TypeConstructor from) {
            // Supertypes + type for container
            Collection<KotlinType> neighbours = new ArrayList<KotlinType>(
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
            return getModality() == Modality.FINAL;
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Override
        @NotNull
        public ClassifierDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @NotNull
        @Override
        public Annotations getAnnotations() {
            return Annotations.Companion.getEMPTY(); // TODO
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
}
