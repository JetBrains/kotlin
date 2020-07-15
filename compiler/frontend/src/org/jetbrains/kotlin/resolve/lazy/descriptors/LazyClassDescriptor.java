/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.lazy.descriptors;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import kotlin.annotations.jvm.ReadOnly;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext;
import org.jetbrains.kotlin.resolve.lazy.LazyEntity;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassInfoUtil;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassOrObjectInfo;
import org.jetbrains.kotlin.resolve.lazy.data.KtObjectInfo;
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinEnum;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.NullableLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static kotlin.collections.CollectionsKt.firstOrNull;
import static org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE;
import static org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PUBLIC;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.KtTokens.INNER_KEYWORD;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.resolveModalityFromModifiers;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.resolveVisibilityFromModifiers;

public class LazyClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes, LazyEntity {
    private static final Function1<KotlinType, Boolean> VALID_SUPERTYPE = type -> {
        assert !KotlinTypeKt.isError(type) : "Error types must be filtered out in DescriptorResolver";
        return TypeUtils.getClassDescriptor(type) != null;
    };

    private final LazyClassContext c;

    @Nullable // can be null in KtScript
    private final KtClassOrObject classOrObject;

    private final ClassMemberDeclarationProvider declarationProvider;

    private final LazyClassTypeConstructor typeConstructor;
    private final NotNullLazyValue<Modality> modality;
    private final DescriptorVisibility visibility;
    private final ClassKind kind;
    private final boolean isInner;
    private final boolean isData;
    private final boolean isInline;
    private final boolean isExpect;
    private final boolean isActual;
    private final boolean isFun;
    private final boolean isValue;

    private final Annotations annotations;
    private final Annotations danglingAnnotations;
    private final NullableLazyValue<ClassDescriptorWithResolutionScopes> companionObjectDescriptor;
    private final MemoizedFunctionToNotNull<KtObjectDeclaration, ClassDescriptor> extraCompanionObjectDescriptors;

    private final ScopesHolderForClass<LazyClassMemberScope> scopesHolderForClass;
    private final MemberScope staticScope;

    private final NullableLazyValue<Void> forceResolveAllContents;
    private final boolean isCompanionObject;

    private final ClassResolutionScopesSupport resolutionScopesSupport;
    private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters;

    private final NotNullLazyValue<LexicalScope> scopeForInitializerResolution;

    private final NotNullLazyValue<Collection<ClassDescriptor>> sealedSubclasses;

    public LazyClassDescriptor(
            @NotNull LazyClassContext c,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull KtClassLikeInfo classLikeInfo,
            boolean isExternal
    ) {
        super(c.getStorageManager(), containingDeclaration, name,
              KotlinSourceElementKt.toSourceElement(classLikeInfo.getCorrespondingClassOrObject()),
              isExternal
        );
        this.c = c;

        classOrObject = classLikeInfo.getCorrespondingClassOrObject();
        if (classOrObject != null) {
            this.c.getTrace().record(BindingContext.CLASS, classOrObject, this);
        }
        this.c.getTrace().record(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, DescriptorUtils.getFqName(this), this);

        this.declarationProvider = c.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfo);

        StorageManager storageManager = c.getStorageManager();

        this.scopesHolderForClass = createScopesHolderForClass(c, this.declarationProvider);
        this.kind = classLikeInfo.getClassKind();
        this.staticScope = kind == ClassKind.ENUM_CLASS ? new StaticScopeForKotlinEnum(storageManager, this) : MemberScope.Empty.INSTANCE;

        this.typeConstructor = new LazyClassTypeConstructor();

        this.isCompanionObject = classLikeInfo instanceof KtObjectInfo && ((KtObjectInfo) classLikeInfo).isCompanionObject();

        KtModifierList modifierList = classLikeInfo.getModifierList();
        if (kind.isSingleton()) {
            this.modality = storageManager.createLazyValue(() -> Modality.FINAL);
        }
        else {
            Modality defaultModality = kind == ClassKind.INTERFACE ? Modality.ABSTRACT : Modality.FINAL;
            this.modality = storageManager.createLazyValue(
                    () -> resolveModalityFromModifiers(classOrObject, defaultModality, c.getTrace().getBindingContext(),
                                                       null, /* allowSealed = */ true));
        }

        boolean isLocal = classOrObject != null && KtPsiUtil.isLocal(classOrObject);
        this.visibility = isLocal ? DescriptorVisibilities.LOCAL : resolveVisibilityFromModifiers(modifierList, DescriptorVisibilities.DEFAULT_VISIBILITY);

        this.isInner = modifierList != null && modifierList.hasModifier(INNER_KEYWORD) && !isIllegalInner(this);
        this.isData = modifierList != null && modifierList.hasModifier(KtTokens.DATA_KEYWORD);
        this.isInline = modifierList != null && modifierList.hasModifier(KtTokens.INLINE_KEYWORD);
        this.isActual = modifierList != null && PsiUtilsKt.hasActualModifier(modifierList);

        this.isExpect = modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) ||
                        containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).isExpect();

        this.isFun = modifierList != null && PsiUtilsKt.hasFunModifier(modifierList);
        this.isValue = modifierList != null && PsiUtilsKt.hasValueModifier(modifierList);

        // Annotation entries are taken from both own annotations (if any) and object literal annotations (if any)
        List<KtAnnotationEntry> annotationEntries = new ArrayList<>();
        if (classOrObject != null && classOrObject.getParent() instanceof KtObjectLiteralExpression) {
            // TODO: it would be better to have separate ObjectLiteralDescriptor without so much magic
            annotationEntries.addAll(KtPsiUtilKt.getAnnotationEntries((KtObjectLiteralExpression) classOrObject.getParent()));
        }
        if (modifierList != null) {
            annotationEntries.addAll(modifierList.getAnnotationEntries());
        }
        if (!annotationEntries.isEmpty()) {
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
                    annotationEntries
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

        this.companionObjectDescriptor = storageManager.createNullableLazyValue(
                () -> computeCompanionObjectDescriptor(getCompanionObjectIfAllowed())
        );
        this.extraCompanionObjectDescriptors = storageManager.createMemoizedFunction(this::computeCompanionObjectDescriptor);
        this.forceResolveAllContents = storageManager.createRecursionTolerantNullableLazyValue(() -> {
            doForceResolveAllContents();
            return null;
        }, null);

        this.resolutionScopesSupport = new ClassResolutionScopesSupport(
                this,
                storageManager,
                c.getLanguageVersionSettings(),
                this::getOuterScope
        );

        this.parameters = c.getStorageManager().createLazyValue(() -> {
            KtClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
            KtTypeParameterList typeParameterList = classInfo.getTypeParameterList();
            if (typeParameterList == null) return Collections.emptyList();

            boolean isAnonymousObject = (classInfo.getClassKind() == ClassKind.CLASS) && (classInfo.getCorrespondingClassOrObject() instanceof KtObjectDeclaration);

            if (classInfo.getClassKind() == ClassKind.ENUM_CLASS) {
                c.getTrace().report(TYPE_PARAMETERS_IN_ENUM.on(typeParameterList));
            }
            if (classInfo.getClassKind() == ClassKind.OBJECT) {
                c.getTrace().report(TYPE_PARAMETERS_IN_OBJECT.on(typeParameterList));
            }
            if (isAnonymousObject) {
                DiagnosticFactory0<KtTypeParameterList> diagnosticFactory;
                if (c.getLanguageVersionSettings().supportsFeature(LanguageFeature.ProhibitTypeParametersInAnonymousObjects)) {
                    diagnosticFactory = TYPE_PARAMETERS_IN_OBJECT;
                } else {
                    diagnosticFactory = TYPE_PARAMETERS_IN_ANONYMOUS_OBJECT;
                }
                c.getTrace().report(diagnosticFactory.on(typeParameterList));
            }

            List<KtTypeParameter> typeParameters = typeParameterList.getParameters();
            if (typeParameters.isEmpty()) return Collections.emptyList();

            boolean supportClassTypeParameterAnnotations = c.getLanguageVersionSettings().supportsFeature(LanguageFeature.ClassTypeParameterAnnotations);
            List<TypeParameterDescriptor> parameters = new ArrayList<>(typeParameters.size());
            
            for (int i = 0; i < typeParameters.size(); i++) {
                KtTypeParameter parameter = typeParameters.get(i);
                Annotations lazyAnnotations;
                if (supportClassTypeParameterAnnotations) {
                    lazyAnnotations = new LazyAnnotations(
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
                            parameter.getAnnotationEntries()
                    );
                } else {
                    lazyAnnotations = Annotations.Companion.getEMPTY();
                }

                parameters.add(new LazyTypeParameterDescriptor(c, this, parameter, lazyAnnotations, i));
            }

            return parameters;
        });

        this.scopeForInitializerResolution = storageManager.createLazyValue(
                () -> ClassResolutionScopesSupportKt.scopeForInitializerResolution(
                        this, createInitializerScopeParent(), classLikeInfo.getPrimaryConstructorParameters()
                )
        );

        boolean freedomForSealedInterfacesSupported = c.getLanguageVersionSettings().supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage);
        this.sealedSubclasses =
            storageManager.createLazyValue(() -> {
                if (getModality() == Modality.SEALED) {
                    return c.getSealedClassInheritorsProvider().computeSealedSubclasses(this, freedomForSealedInterfacesSupported);
                } else {
                    return Collections.emptyList();
                }
        });
    }

    private static boolean isIllegalInner(@NotNull DeclarationDescriptor descriptor) {
        if (!DescriptorUtils.isClass(descriptor)) return true;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        return !(containingDeclaration instanceof ClassDescriptor) ||
               DescriptorUtils.isInterface(containingDeclaration) ||
               DescriptorUtils.isObject(containingDeclaration);
    }

    @NotNull
    private DeclarationDescriptor createInitializerScopeParent() {
        ConstructorDescriptor primaryConstructor = getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor != null) return primaryConstructor;

        return new FunctionDescriptorImpl(
                LazyClassDescriptor.this, null, Annotations.Companion.getEMPTY(), Name.special("<init-blocks>"),
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
        ) {
            {
                initialize(null, null, CollectionsKt.emptyList(), Collections.emptyList(), Collections.emptyList(),
                           null, Modality.FINAL, DescriptorVisibilities.PRIVATE);
            }

            @NotNull
            @Override
            protected FunctionDescriptorImpl createSubstitutedCopy(
                    @NotNull DeclarationDescriptor newOwner,
                    @Nullable FunctionDescriptor original,
                    @NotNull Kind kind,
                    @Nullable Name newName,
                    @NotNull Annotations annotations,
                    @NotNull SourceElement source
            ) {
                throw new UnsupportedOperationException();
            }
        };
    }

    // NOTE: Called from constructor!
    @NotNull
    protected ScopesHolderForClass<LazyClassMemberScope> createScopesHolderForClass(
            @NotNull LazyClassContext c,
            @NotNull ClassMemberDeclarationProvider declarationProvider
    ) {
        return ScopesHolderForClass.Companion.create(
                this,
                c.getStorageManager(),
                c.getKotlinTypeCheckerOfOwnerModule().getKotlinTypeRefiner(),
                kotlinTypeRefinerForDependentModule -> {
                    LazyClassMemberScope scopeForDeclaredMembers =
                            !kotlinTypeRefinerForDependentModule.isRefinementNeededForModule(c.getModuleDescriptor())
                                ? null
                                : scopesHolderForClass.getScope(c.getKotlinTypeCheckerOfOwnerModule().getKotlinTypeRefiner()); // essentially, a scope for owner-module

                    return new LazyClassMemberScope(
                            c, declarationProvider, this, c.getTrace(), kotlinTypeRefinerForDependentModule,
                            scopeForDeclaredMembers
                    );
                }
        );
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull KotlinTypeRefiner kotlinTypeRefiner) {
        return scopesHolderForClass.getScope(kotlinTypeRefiner);
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
    @SuppressWarnings("unchecked")
    public Collection<CallableMemberDescriptor> getDeclaredCallableMembers() {
        return (Collection) CollectionsKt.filter(
                DescriptorUtils.getAllDescriptors(getUnsubstitutedMemberScope()),
                descriptor -> descriptor instanceof CallableMemberDescriptor
                              && ((CallableMemberDescriptor) descriptor).getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        );
    }

    @NotNull
    @Override
    public MemberScope getStaticScope() {
        return staticScope;
    }

    @NotNull
    @Override
    public Collection<ClassConstructorDescriptor> getConstructors() {
        return ((LazyClassMemberScope) getUnsubstitutedMemberScope()).getConstructors();
    }

    @Override
    public ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return ((LazyClassMemberScope) getUnsubstitutedMemberScope()).getPrimaryConstructor();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public ClassDescriptorWithResolutionScopes getCompanionObjectDescriptor() {
        return companionObjectDescriptor.invoke();
    }

    @Nullable
    @Override
    public SimpleType getDefaultFunctionTypeForSamInterface() {
        return c.getSamConversionResolver().resolveFunctionTypeIfSamInterface(this);
    }

    @Override
    public boolean isDefinitelyNotSamInterface() {
        return !isFun();
    }

    @NotNull
    @ReadOnly
    public List<ClassDescriptor> getDescriptorsForExtraCompanionObjects() {
        KtObjectDeclaration allowedCompanionObject = getCompanionObjectIfAllowed();

        return CollectionsKt.map(
                CollectionsKt.filter(
                        declarationProvider.getOwnerInfo().getCompanionObjects(),
                        companionObject -> companionObject != allowedCompanionObject
                ),
                extraCompanionObjectDescriptors
        );
    }

    @Nullable
    private ClassDescriptorWithResolutionScopes computeCompanionObjectDescriptor(@Nullable KtObjectDeclaration companionObject) {
        if (companionObject == null)
            return createSyntheticCompanionObjectDescriptor();
        KtClassLikeInfo companionObjectInfo = getCompanionObjectInfo(companionObject);
        if (!(companionObjectInfo instanceof KtClassOrObjectInfo)) {
            return null;
        }
        Name name = ((KtClassOrObjectInfo) companionObjectInfo).getName();
        assert name != null;
        getUnsubstitutedMemberScope().getContributedClassifier(name, NoLookupLocation.WHEN_GET_COMPANION_OBJECT);
        ClassDescriptor companionObjectDescriptor = c.getTrace().get(BindingContext.CLASS, companionObject);
        if (companionObjectDescriptor instanceof ClassDescriptorWithResolutionScopes) {
            assert DescriptorUtils.isCompanionObject(companionObjectDescriptor) : "Not a companion object: " + companionObjectDescriptor;
            return (ClassDescriptorWithResolutionScopes)companionObjectDescriptor;
        }
        else {
            return null;
        }
    }

    private ClassDescriptorWithResolutionScopes createSyntheticCompanionObjectDescriptor() {
        Name syntheticCompanionName = c.getSyntheticResolveExtension().getSyntheticCompanionObjectNameIfNeeded(this);
        if (syntheticCompanionName == null)
            return null;
        SyntheticClassOrObjectDescriptor companionDescriptor = new SyntheticClassOrObjectDescriptor(c,
                /* parentClassOrObject= */ classOrObject,
                this, syntheticCompanionName, getSource(),
                /* outerScope= */ getOuterScope(),
                Modality.FINAL, PUBLIC, Annotations.Companion.getEMPTY(), PRIVATE, ClassKind.OBJECT, true);
        companionDescriptor.initialize();
        return companionDescriptor;
    }

    @Nullable
    private static KtClassLikeInfo getCompanionObjectInfo(@Nullable KtObjectDeclaration companionObject) {
        if (companionObject != null) {
            return KtClassInfoUtil.createClassOrObjectInfo(companionObject);
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
        return kind;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality.invoke();
    }

    @NotNull
    @Override
    public DescriptorVisibility getVisibility() {
        return visibility;
    }

    @Override
    public boolean isInner() {
        return isInner;
    }

    @Override
    public boolean isData() {
        return isData;
    }

    @Override
    public boolean isInline() {
        return isInline;
    }

    @Override
    public boolean isCompanionObject() {
        return isCompanionObject;
    }

    @Override
    public boolean isExpect() {
        return isExpect;
    }

    @Override
    public boolean isActual() {
        return isActual;
    }

    @Override
    public boolean isFun() {
        return isFun;
    }

    @Override
    public boolean isValue() {
        return isValue;
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

    @NotNull
    @Override
    public Collection<ClassDescriptor> getSealedSubclasses() {
        return sealedSubclasses.invoke();
    }

    @Nullable
    @Override
    public InlineClassRepresentation<SimpleType> getInlineClassRepresentation() {
        if (!InlineClassesUtilsKt.isInlineClass(this)) return null;

        ClassConstructorDescriptor constructor = getUnsubstitutedPrimaryConstructor();
        if (constructor != null) {
            ValueParameterDescriptor parameter = firstOrNull(constructor.getValueParameters());
            if (parameter != null) {
                return new InlineClassRepresentation<>(parameter.getName(), (SimpleType) parameter.getType());
            }
        }

        // Don't crash on invalid code.
        return new InlineClassRepresentation<>(
                SpecialNames.SAFE_IDENTIFIER_FOR_NO_NAME, c.getModuleDescriptor().getBuiltIns().getAnyType()
        );
    }

    @Override
    public String toString() {
        // not using DescriptorRenderer to preserve laziness
        return (isExpect ? "expect " : isActual ? "actual " : "") + "class " + getName().toString();
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

    private class LazyClassTypeConstructor extends AbstractClassTypeConstructor {
        private final NotNullLazyValue<List<TypeParameterDescriptor>> parameters = c.getStorageManager().createLazyValue(
                () -> TypeParameterUtilsKt.computeConstructorTypeParameters(LazyClassDescriptor.this)
        );

        public LazyClassTypeConstructor() {
            super(LazyClassDescriptor.this.c.getStorageManager());
        }

        @NotNull
        @Override
        protected Collection<KotlinType> computeSupertypes() {
            return LazyClassDescriptor.this.computeSupertypes();
        }

        @Override
        protected void reportSupertypeLoopError(@NotNull KotlinType type) {
            ClassifierDescriptor supertypeDescriptor = type.getConstructor().getDeclarationDescriptor();
            if (supertypeDescriptor instanceof ClassDescriptor) {
                ClassDescriptor superclass = (ClassDescriptor) supertypeDescriptor;
                reportCyclicInheritanceHierarchyError(c.getTrace(), LazyClassDescriptor.this, superclass);
            }
        }

        @Override
        protected boolean getShouldReportCyclicScopeWithCompanionWarning() {
            return !c.getLanguageVersionSettings()
                    .supportsFeature(LanguageFeature.ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion);
        }

        @Override
        protected void reportScopesLoopError(@NotNull KotlinType type) {
            PsiElement reportOn = DescriptorToSourceUtils.getSourceFromDescriptor(type.getConstructor().getDeclarationDescriptor());

            if (reportOn instanceof KtClass) {
                reportOn = ((KtClass) reportOn).getNameIdentifier();
            }

            if (reportOn != null) {
                c.getTrace().report(CYCLIC_SCOPES_WITH_COMPANION.on(reportOn));
            }
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

        @NotNull
        @Override
        protected SupertypeLoopChecker getSupertypeLoopChecker() {
            return c.getSupertypeLoopChecker();
        }

        @NotNull
        @Override
        public List<TypeParameterDescriptor> getParameters() {
            return parameters.invoke();
        }

        @Override
        public boolean isDenotable() {
            return true;
        }

        @Override
        @NotNull
        public ClassDescriptor getDeclarationDescriptor() {
            return LazyClassDescriptor.this;
        }

        @Override
        public String toString() {
            return LazyClassDescriptor.this.getName().toString();
        }
    }

    @NotNull
    protected Collection<KotlinType> computeSupertypes() {
        if (KotlinBuiltIns.isSpecialClassWithNoSupertypes(this)) {
            return Collections.emptyList();
        }

        KtClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
        if (classOrObject == null) {
            return Collections.singleton(c.getModuleDescriptor().getBuiltIns().getAnyType());
        }

        List<KotlinType> allSupertypes =
                c.getDescriptorResolver().resolveSupertypes(getScopeForClassHeaderResolution(), this, classOrObject, c.getTrace());

        return new ArrayList<>(CollectionsKt.filter(allSupertypes, VALID_SUPERTYPE));
    }
}
