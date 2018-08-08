/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationSplitter;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations;
import org.jetbrains.kotlin.descriptors.impl.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.util.UnderscoreUtilKt;
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyTypeAliasDescriptor;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.*;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.*;

import static org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.resolve.BindingContext.CONSTRUCTOR;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE_ALIAS;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.resolveMemberModalityFromModifiers;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.resolveVisibilityFromModifiers;

public class DescriptorResolver {
    private final TypeResolver typeResolver;
    private final AnnotationResolver annotationResolver;
    private final StorageManager storageManager;
    private final KotlinBuiltIns builtIns;
    private final SupertypeLoopChecker supertypeLoopsResolver;
    private final VariableTypeAndInitializerResolver variableTypeAndInitializerResolver;
    private final ExpressionTypingServices expressionTypingServices;
    private final OverloadChecker overloadChecker;
    private final LanguageVersionSettings languageVersionSettings;
    private final FunctionsTypingVisitor functionsTypingVisitor;
    private final DestructuringDeclarationResolver destructuringDeclarationResolver;
    private final ModifiersChecker modifiersChecker;
    private final WrappedTypeFactory wrappedTypeFactory;
    private final SyntheticResolveExtension syntheticResolveExtension;
    private final TypeApproximator typeApproximator;
    private final DeclarationReturnTypeSanitizer declarationReturnTypeSanitizer;
    private final DataFlowValueFactory dataFlowValueFactory;
    private final Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers;

    public DescriptorResolver(
            @NotNull AnnotationResolver annotationResolver,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull StorageManager storageManager,
            @NotNull TypeResolver typeResolver,
            @NotNull SupertypeLoopChecker supertypeLoopsResolver,
            @NotNull VariableTypeAndInitializerResolver variableTypeAndInitializerResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull OverloadChecker overloadChecker,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull FunctionsTypingVisitor functionsTypingVisitor,
            @NotNull DestructuringDeclarationResolver destructuringDeclarationResolver,
            @NotNull ModifiersChecker modifiersChecker,
            @NotNull WrappedTypeFactory wrappedTypeFactory,
            @NotNull Project project,
            @NotNull TypeApproximator approximator,
            @NotNull DeclarationReturnTypeSanitizer declarationReturnTypeSanitizer,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers
    ) {
        this.annotationResolver = annotationResolver;
        this.builtIns = builtIns;
        this.storageManager = storageManager;
        this.typeResolver = typeResolver;
        this.supertypeLoopsResolver = supertypeLoopsResolver;
        this.variableTypeAndInitializerResolver = variableTypeAndInitializerResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.overloadChecker = overloadChecker;
        this.languageVersionSettings = languageVersionSettings;
        this.functionsTypingVisitor = functionsTypingVisitor;
        this.destructuringDeclarationResolver = destructuringDeclarationResolver;
        this.modifiersChecker = modifiersChecker;
        this.wrappedTypeFactory = wrappedTypeFactory;
        this.syntheticResolveExtension = SyntheticResolveExtension.Companion.getInstance(project);
        typeApproximator = approximator;
        this.declarationReturnTypeSanitizer = declarationReturnTypeSanitizer;
        this.dataFlowValueFactory = dataFlowValueFactory;
        this.anonymousTypeTransformers = anonymousTypeTransformers;
    }

    public List<KotlinType> resolveSupertypes(
            @NotNull LexicalScope scope,
            @NotNull ClassDescriptor classDescriptor,
            @Nullable KtPureClassOrObject correspondingClassOrObject,
            BindingTrace trace
    ) {
        List<KotlinType> supertypes = Lists.newArrayList();
        List<KtSuperTypeListEntry> delegationSpecifiers =
                correspondingClassOrObject == null ? Collections.emptyList() : correspondingClassOrObject.getSuperTypeListEntries();
        Collection<KotlinType> declaredSupertypes = resolveSuperTypeListEntries(
                scope,
                delegationSpecifiers,
                typeResolver, trace, false);

        for (KotlinType declaredSupertype : declaredSupertypes) {
            addValidSupertype(supertypes, declaredSupertype);
        }

        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS && !containsClass(supertypes)) {
            supertypes.add(0, builtIns.getEnumType(classDescriptor.getDefaultType()));
        }

        syntheticResolveExtension.addSyntheticSupertypes(classDescriptor, supertypes);

        if (supertypes.isEmpty()) {
            addValidSupertype(supertypes, getDefaultSupertype(classDescriptor));
        }

        return supertypes;
    }

    private static void addValidSupertype(List<KotlinType> supertypes, KotlinType declaredSupertype) {
        if (!KotlinTypeKt.isError(declaredSupertype)) {
            supertypes.add(declaredSupertype);
        }
    }

    private static boolean containsClass(Collection<KotlinType> result) {
        for (KotlinType type : result) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.INTERFACE) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private KotlinType getDefaultSupertype(@NotNull ClassDescriptor classDescriptor) {
        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return ((ClassDescriptor) classDescriptor.getContainingDeclaration()).getDefaultType();
        }
        else if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            return builtIns.getAnnotationType();
        }
        return builtIns.getAnyType();
    }

    private static Collection<KotlinType> resolveSuperTypeListEntries(
            LexicalScope extensibleScope,
            List<KtSuperTypeListEntry> delegationSpecifiers,
            @NotNull TypeResolver resolver,
            BindingTrace trace,
            boolean checkBounds
    ) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<KotlinType> result = Lists.newArrayList();
        for (KtSuperTypeListEntry delegationSpecifier : delegationSpecifiers) {
            KtTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                KotlinType supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds);
                if (DynamicTypesKt.isDynamic(supertype)) {
                    trace.report(DYNAMIC_SUPERTYPE.on(typeReference));
                }
                else {
                    result.add(supertype);
                    KtTypeElement bareSuperType = checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.getTypeElement());
                    checkProjectionsInImmediateArguments(trace, bareSuperType, supertype);
                }
            }
            else {
                result.add(ErrorUtils.createErrorType("No type reference"));
            }
        }
        return result;
    }

    @Nullable
    private static KtTypeElement checkNullableSupertypeAndStripQuestionMarks(@NotNull BindingTrace trace, @Nullable KtTypeElement typeElement) {
        while (typeElement instanceof KtNullableType) {
            KtNullableType nullableType = (KtNullableType) typeElement;
            typeElement = nullableType.getInnerType();
            // report only for innermost '?', the rest gets a 'redundant' warning
            if (!(typeElement instanceof KtNullableType) && typeElement != null) {
                trace.report(NULLABLE_SUPERTYPE.on(nullableType));
            }
        }
        return typeElement;
    }

    private static void checkProjectionsInImmediateArguments(
            @NotNull BindingTrace trace,
            @Nullable KtTypeElement typeElement,
            @NotNull KotlinType type
    ) {
        if (typeElement == null) return;

        boolean hasProjectionsInWrittenArguments = false;
        if (typeElement instanceof KtUserType) {
            KtUserType userType = (KtUserType) typeElement;
            List<KtTypeProjection> typeArguments = userType.getTypeArguments();
            for (KtTypeProjection typeArgument : typeArguments) {
                if (typeArgument.getProjectionKind() != KtProjectionKind.NONE) {
                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
                    hasProjectionsInWrittenArguments = true;
                }
            }
        }

        // If we have an abbreviated type (written with a type alias), it still can contain type projections in top-level arguments.
        if (!KotlinTypeKt.isError(type) && SpecialTypesKt.getAbbreviatedType(type) != null && !hasProjectionsInWrittenArguments) {
            // Only interface inheritance should be checked here.
            // Corresponding check for classes is performed for type alias constructor calls in CandidateResolver.
            if (TypeUtilsKt.isInterface(type) && TypeUtilsKt.containsTypeProjectionsInTopLevelArguments(type)) {
                trace.report(EXPANDED_TYPE_CANNOT_BE_INHERITED.on(typeElement, type));
            }
        }
    }

    public static Visibility getDefaultVisibility(KtModifierListOwner modifierListOwner, DeclarationDescriptor containingDescriptor) {
        Visibility defaultVisibility;
        if (containingDescriptor instanceof ClassDescriptor) {
            KtModifierList modifierList = modifierListOwner.getModifierList();
            defaultVisibility = modifierList != null && modifierList.hasModifier(OVERRIDE_KEYWORD)
                                           ? Visibilities.INHERITED
                                           : Visibilities.DEFAULT_VISIBILITY;
        }
        else if (containingDescriptor instanceof FunctionDescriptor || containingDescriptor instanceof PropertyDescriptor) {
            defaultVisibility = Visibilities.LOCAL;
        }
        else {
            defaultVisibility = Visibilities.DEFAULT_VISIBILITY;
        }
        return defaultVisibility;
    }

    public static Modality getDefaultModality(DeclarationDescriptor containingDescriptor, Visibility visibility, boolean isBodyPresent) {
        Modality defaultModality;
        if (containingDescriptor instanceof ClassDescriptor) {
            boolean isTrait = ((ClassDescriptor) containingDescriptor).getKind() == ClassKind.INTERFACE;
            boolean isDefinitelyAbstract = isTrait && !isBodyPresent;
            Modality basicModality = isTrait && !Visibilities.isPrivate(visibility) ? Modality.OPEN : Modality.FINAL;
            defaultModality = isDefinitelyAbstract ? Modality.ABSTRACT : basicModality;
        }
        else {
            defaultModality = Modality.FINAL;
        }
        return defaultModality;
    }

    @NotNull
    public ValueParameterDescriptorImpl resolveValueParameterDescriptor(
            @NotNull LexicalScope scope,
            @NotNull FunctionDescriptor owner,
            @NotNull KtParameter valueParameter,
            int index,
            @NotNull KotlinType type,
            @NotNull BindingTrace trace
    ) {
        KotlinType varargElementType = null;
        KotlinType variableType = type;
        if (valueParameter.hasModifier(VARARG_KEYWORD)) {
            varargElementType = type;
            variableType = getVarargParameterType(type);
        }

        KtModifierList modifierList = valueParameter.getModifierList();

        Annotations allAnnotations =
                annotationResolver.resolveAnnotationsWithoutArguments(scope, valueParameter.getModifierList(), trace);
        Annotations valueParameterAnnotations = Annotations.Companion.getEMPTY();

        if (modifierList != null) {
            if (valueParameter.hasValOrVar()) {
                AnnotationSplitter annotationSplitter = AnnotationSplitter.create(
                        storageManager, allAnnotations, SetsKt.setOf(CONSTRUCTOR_PARAMETER));
                valueParameterAnnotations = annotationSplitter.getAnnotationsForTarget(CONSTRUCTOR_PARAMETER);
            }
            else {
                valueParameterAnnotations = allAnnotations;
            }
        }

        KtDestructuringDeclaration destructuringDeclaration = valueParameter.getDestructuringDeclaration();

        Function0<List<VariableDescriptor>> destructuringVariables;
        if (destructuringDeclaration != null) {
            if (!languageVersionSettings.supportsFeature(LanguageFeature.DestructuringLambdaParameters)) {
                trace.report(Errors.UNSUPPORTED_FEATURE.on(valueParameter,
                                                           TuplesKt.to(LanguageFeature.DestructuringLambdaParameters, languageVersionSettings)));
            }

            destructuringVariables = () -> {
                assert owner.getDispatchReceiverParameter() == null
                        : "Destructuring declarations are only be parsed for lambdas, and they must not have a dispatch receiver";
                LexicalScope scopeForDestructuring =
                        ScopeUtilsKt.createScopeForDestructuring(scope, owner.getExtensionReceiverParameter());

                List<VariableDescriptor> result =
                        destructuringDeclarationResolver.resolveLocalVariablesFromDestructuringDeclaration(
                                scope,
                                destructuringDeclaration, new TransientReceiver(type), /* initializer = */ null,
                                ExpressionTypingContext.newContext(
                                        trace, scopeForDestructuring, DataFlowInfoFactory.EMPTY, TypeUtils.NO_EXPECTED_TYPE,
                                        languageVersionSettings, dataFlowValueFactory
                                )
                        );

                modifiersChecker.withTrace(trace).checkModifiersForDestructuringDeclaration(destructuringDeclaration);
                return result;
            };
        }
        else {
            destructuringVariables = null;
        }

        Name parameterName;

        if (destructuringDeclaration == null) {
            // NB: val/var for parameter is only allowed in primary constructors where single underscore names are still prohibited.
            // The problem with val/var is that when lazy resolve try to find their descriptor, it searches through the member scope
            // of containing class where, it can not find a descriptor with special name.
            // Thus, to preserve behavior, we don't use a special name for val/var.
            parameterName = !valueParameter.hasValOrVar() && UnderscoreUtilKt.isSingleUnderscore(valueParameter)
                            ? Name.special("<anonymous parameter " + index + ">")
                            : KtPsiUtil.safeName(valueParameter.getName());
        }
        else {
            parameterName = Name.special("<name for destructuring parameter " + index + ">");
        }

        ValueParameterDescriptorImpl valueParameterDescriptor = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                owner,
                null,
                index,
                valueParameterAnnotations,
                parameterName,
                variableType,
                valueParameter.hasDefaultValue(),
                valueParameter.hasModifier(CROSSINLINE_KEYWORD),
                valueParameter.hasModifier(NOINLINE_KEYWORD),
                varargElementType,
                KotlinSourceElementKt.toSourceElement(valueParameter),
                destructuringVariables
        );

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    @NotNull
    private KotlinType getVarargParameterType(@NotNull KotlinType elementType) {
        KotlinType primitiveArrayType = builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(elementType);
        if (primitiveArrayType != null) {
            return primitiveArrayType;
        }
        return builtIns.getArrayType(Variance.OUT_VARIANCE, elementType);
    }

    public List<TypeParameterDescriptorImpl> resolveTypeParametersForDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalWritableScope extensibleScope,
            LexicalScope scopeForAnnotationsResolve,
            List<KtTypeParameter> typeParameters,
            BindingTrace trace
    ) {
        List<TypeParameterDescriptorImpl> descriptors =
                resolveTypeParametersForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameters, trace);
        for (TypeParameterDescriptorImpl descriptor : descriptors) {
            extensibleScope.addClassifierDescriptor(descriptor);
        }
        return descriptors;
    }

    private List<TypeParameterDescriptorImpl> resolveTypeParametersForDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalScope scopeForAnnotationsResolve,
            List<KtTypeParameter> typeParameters,
            BindingTrace trace
    ) {
        assert containingDescriptor instanceof FunctionDescriptor ||
               containingDescriptor instanceof PropertyDescriptor ||
               containingDescriptor instanceof TypeAliasDescriptor
                : "This method should be called for functions, properties, or type aliases, got " + containingDescriptor;

        List<TypeParameterDescriptorImpl> result = new ArrayList<>();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            KtTypeParameter typeParameter = typeParameters.get(i);
            result.add(resolveTypeParameterForDescriptor(containingDescriptor, scopeForAnnotationsResolve, typeParameter, i, trace));
        }
        return result;
    }

    private TypeParameterDescriptorImpl resolveTypeParameterForDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalScope scopeForAnnotationsResolve,
            KtTypeParameter typeParameter,
            int index,
            BindingTrace trace
    ) {
        if (typeParameter.getVariance() != Variance.INVARIANT) {
            trace.report(VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED.on(typeParameter));
        }

        Annotations annotations =
                annotationResolver.resolveAnnotationsWithArguments(scopeForAnnotationsResolve, typeParameter.getModifierList(), trace);

        TypeParameterDescriptorImpl typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDescriptor,
                annotations,
                typeParameter.hasModifier(KtTokens.REIFIED_KEYWORD),
                typeParameter.getVariance(),
                KtPsiUtil.safeName(typeParameter.getName()),
                index,
                KotlinSourceElementKt.toSourceElement(typeParameter),
                type -> {
                    if (!(containingDescriptor instanceof TypeAliasDescriptor)) {
                        trace.report(Errors.CYCLIC_GENERIC_UPPER_BOUND.on(typeParameter));
                    }
                    return null;
                },
                supertypeLoopsResolver
        );
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    @NotNull
    public static ClassConstructorDescriptorImpl createAndRecordPrimaryConstructorForObject(
            @Nullable KtPureClassOrObject object,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        ClassConstructorDescriptorImpl constructorDescriptor =
                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, KotlinSourceElementKt.toSourceElement(object));
        if (object instanceof PsiElement) {
            KtPrimaryConstructor primaryConstructor = object.getPrimaryConstructor();
            trace.record(CONSTRUCTOR, primaryConstructor != null ? primaryConstructor : (PsiElement)object, constructorDescriptor);
        }
        return constructorDescriptor;
    }

    static final class UpperBoundCheckRequest {
        public final Name typeParameterName;
        public final KtTypeReference upperBound;
        public final KotlinType upperBoundType;

        UpperBoundCheckRequest(Name typeParameterName, KtTypeReference upperBound, KotlinType upperBoundType) {
            this.typeParameterName = typeParameterName;
            this.upperBound = upperBound;
            this.upperBoundType = upperBoundType;
        }
    }

    public void resolveGenericBounds(
            @NotNull KtTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            LexicalScope scope,
            List<TypeParameterDescriptorImpl> parameters,
            BindingTrace trace
    ) {
        List<UpperBoundCheckRequest> upperBoundCheckRequests = Lists.newArrayList();

        List<KtTypeParameter> typeParameters = declaration.getTypeParameters();
        Map<Name, TypeParameterDescriptorImpl> parameterByName = new HashMap<>();
        for (int i = 0; i < typeParameters.size(); i++) {
            KtTypeParameter ktTypeParameter = typeParameters.get(i);
            TypeParameterDescriptorImpl typeParameterDescriptor = parameters.get(i);

            parameterByName.put(typeParameterDescriptor.getName(), typeParameterDescriptor);

            KtTypeReference extendsBound = ktTypeParameter.getExtendsBound();
            if (extendsBound != null) {
                KotlinType type = typeResolver.resolveType(scope, extendsBound, trace, false);
                typeParameterDescriptor.addUpperBound(type);
                upperBoundCheckRequests.add(new UpperBoundCheckRequest(ktTypeParameter.getNameAsName(), extendsBound, type));
            }
        }
        for (KtTypeConstraint constraint : declaration.getTypeConstraints()) {
            KtSimpleNameExpression subjectTypeParameterName = constraint.getSubjectTypeParameterName();
            if (subjectTypeParameterName == null) {
                continue;
            }
            Name referencedName = subjectTypeParameterName.getReferencedNameAsName();
            TypeParameterDescriptorImpl typeParameterDescriptor = parameterByName.get(referencedName);
            KtTypeReference boundTypeReference = constraint.getBoundTypeReference();
            KotlinType bound = null;
            if (boundTypeReference != null) {
                bound = typeResolver.resolveType(scope, boundTypeReference, trace, false);
                upperBoundCheckRequests.add(new UpperBoundCheckRequest(referencedName, boundTypeReference, bound));
            }

            if (typeParameterDescriptor != null) {
                trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor);
                if (bound != null) {
                    typeParameterDescriptor.addUpperBound(bound);
                }
            }
        }

        for (TypeParameterDescriptorImpl parameter : parameters) {
            parameter.addDefaultUpperBound();
            parameter.setInitialized();
        }

        for (TypeParameterDescriptorImpl parameter : parameters) {
            checkConflictingUpperBounds(trace, parameter, typeParameters.get(parameter.getIndex()));
        }

        if (!(declaration instanceof KtClass)) {
            checkUpperBoundTypes(trace, upperBoundCheckRequests);
            checkNamesInConstraints(declaration, descriptor, scope, trace);
        }
    }

    public static void checkUpperBoundTypes(@NotNull BindingTrace trace, @NotNull List<UpperBoundCheckRequest> requests) {
        if (requests.isEmpty()) return;

        Set<Name> classBoundEncountered = new HashSet<>();
        Set<Pair<Name, TypeConstructor>> allBounds = new HashSet<>();

        for (UpperBoundCheckRequest request : requests) {
            Name typeParameterName = request.typeParameterName;
            KotlinType upperBound = request.upperBoundType;
            KtTypeReference upperBoundElement = request.upperBound;

            if (!KotlinTypeKt.isError(upperBound)) {
                if (!allBounds.add(new Pair<>(typeParameterName, upperBound.getConstructor()))) {
                    trace.report(REPEATED_BOUND.on(upperBoundElement));
                }
                else {
                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(upperBound);
                    if (classDescriptor != null) {
                        ClassKind kind = classDescriptor.getKind();
                        if (kind == ClassKind.CLASS || kind == ClassKind.ENUM_CLASS || kind == ClassKind.OBJECT) {
                            if (!classBoundEncountered.add(typeParameterName)) {
                                trace.report(ONLY_ONE_CLASS_BOUND_ALLOWED.on(upperBoundElement));
                            }
                        }
                    }
                }
            }

            checkUpperBoundType(upperBoundElement, upperBound, trace);
        }
    }

    public static void checkConflictingUpperBounds(
            @NotNull BindingTrace trace,
            @NotNull TypeParameterDescriptor parameter,
            @NotNull KtTypeParameter typeParameter
    ) {
        if (KotlinBuiltIns.isNothing(TypeIntersector.getUpperBoundsAsType(parameter))) {
            trace.report(CONFLICTING_UPPER_BOUNDS.on(typeParameter, parameter));
        }
    }

    public void checkNamesInConstraints(
            @NotNull KtTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace
    ) {
        for (KtTypeConstraint constraint : declaration.getTypeConstraints()) {
            KtSimpleNameExpression nameExpression = constraint.getSubjectTypeParameterName();
            if (nameExpression == null) continue;

            Name name = nameExpression.getReferencedNameAsName();

            ClassifierDescriptor classifier = ScopeUtilsKt.findClassifier(scope, name, NoLookupLocation.FOR_NON_TRACKED_SCOPE);
            if (classifier instanceof TypeParameterDescriptor && classifier.getContainingDeclaration() == descriptor) continue;

            if (classifier != null) {
                // To tell the user that we look only for locally defined type parameters
                trace.report(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(nameExpression, constraint, declaration));
                trace.record(BindingContext.REFERENCE_TARGET, nameExpression, classifier);
            }
            else {
                trace.report(UNRESOLVED_REFERENCE.on(nameExpression, nameExpression));
            }

            KtTypeReference boundTypeReference = constraint.getBoundTypeReference();
            if (boundTypeReference != null) {
                typeResolver.resolveType(scope, boundTypeReference, trace, true);
            }
        }
    }

    public static void checkUpperBoundType(
            KtTypeReference upperBound,
            @NotNull KotlinType upperBoundType,
            BindingTrace trace
    ) {
        if (!TypeUtils.canHaveSubtypes(KotlinTypeChecker.DEFAULT, upperBoundType)) {
            trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
        }
        if (DynamicTypesKt.isDynamic(upperBoundType)) {
            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
        }
        if (FunctionTypesKt.isExtensionFunctionType(upperBoundType)) {
            trace.report(UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE.on(upperBound));
        }
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(
            @NotNull LexicalScope scope,
            @NotNull KtParameter parameter,
            BindingTrace trace
    ) {
        KotlinType type = resolveParameterType(scope, parameter, trace);
        return resolveLocalVariableDescriptor(parameter, type, trace, scope);
    }

    private KotlinType resolveParameterType(LexicalScope scope, KtParameter parameter, BindingTrace trace) {
        KtTypeReference typeReference = parameter.getTypeReference();
        KotlinType type;
        if (typeReference != null) {
            type = typeResolver.resolveType(scope, typeReference, trace, true);
        }
        else {
            // Error is reported by the parser
            type = ErrorUtils.createErrorType("Annotation is absent");
        }
        if (parameter.hasModifier(VARARG_KEYWORD)) {
            return getVarargParameterType(type);
        }
        return type;
    }

    public VariableDescriptor resolveLocalVariableDescriptor(
            @NotNull KtParameter parameter,
            @NotNull KotlinType type,
            BindingTrace trace,
            @NotNull LexicalScope scope
    ) {
        UnwrappedType approximatedType = typeApproximator.approximateDeclarationType(type, true, languageVersionSettings);
        VariableDescriptor variableDescriptor = new LocalVariableDescriptor(
                scope.getOwnerDescriptor(),
                annotationResolver.resolveAnnotationsWithArguments(scope, parameter.getModifierList(), trace),
                KtPsiUtil.safeName(parameter.getName()),
                approximatedType,
                KotlinSourceElementKt.toSourceElement(parameter)
        );
        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
        // Type annotations also should be resolved
        ForceResolveUtil.forceResolveAllContents(type.getAnnotations());
        return variableDescriptor;
    }

    @NotNull
    public TypeAliasDescriptor resolveTypeAliasDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scope,
            @NotNull KtTypeAlias typeAlias,
            @NotNull BindingTrace trace
    ) {
        if (!(containingDeclaration instanceof PackageFragmentDescriptor) &&
            !(containingDeclaration instanceof ScriptDescriptor)) {
            trace.report(TOPLEVEL_TYPEALIASES_ONLY.on(typeAlias));
        }

        KtModifierList modifierList = typeAlias.getModifierList();
        Visibility visibility = resolveVisibilityFromModifiers(typeAlias, getDefaultVisibility(typeAlias, containingDeclaration));

        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithArguments(scope, modifierList, trace);
        Name name = KtPsiUtil.safeName(typeAlias.getName());
        SourceElement sourceElement = KotlinSourceElementKt.toSourceElement(typeAlias);
        LazyTypeAliasDescriptor typeAliasDescriptor = LazyTypeAliasDescriptor.create(
                storageManager, trace, containingDeclaration, allAnnotations, name, sourceElement, visibility);

        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
        LexicalScope scopeWithTypeParameters;
        {
            List<KtTypeParameter> typeParameters = typeAlias.getTypeParameters();
            if (typeParameters.isEmpty()) {
                scopeWithTypeParameters = scope;
                typeParameterDescriptors = Collections.emptyList();
            }
            else {
                LexicalWritableScope writableScope = new LexicalWritableScope(
                        scope, containingDeclaration, false, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                        LexicalScopeKind.TYPE_ALIAS_HEADER);
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                        typeAliasDescriptor, writableScope, scope, typeParameters, trace);
                writableScope.freeze();
                checkNoGenericBoundsOnTypeAliasParameters(typeAlias, trace);
                resolveGenericBounds(typeAlias, typeAliasDescriptor, writableScope, typeParameterDescriptors, trace);
                scopeWithTypeParameters = writableScope;
            }
        }

        KtTypeReference typeReference = typeAlias.getTypeReference();
        if (typeReference == null) {
            typeAliasDescriptor.initialize(
                    typeParameterDescriptors,
                    ErrorUtils.createErrorType(name.asString()),
                    ErrorUtils.createErrorType(name.asString()));
        }
        else if (!languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)) {
            typeResolver.resolveAbbreviatedType(scopeWithTypeParameters, typeReference, trace);
            PsiElement typeAliasKeyword = typeAlias.getTypeAliasKeyword();
            trace.report(UNSUPPORTED_FEATURE.on(typeAliasKeyword != null ? typeAliasKeyword : typeAlias,
                                                TuplesKt.to(LanguageFeature.TypeAliases, languageVersionSettings)));
            typeAliasDescriptor.initialize(
                    typeParameterDescriptors,
                    ErrorUtils.createErrorType(name.asString()),
                    ErrorUtils.createErrorType(name.asString()));
        }
        else {
            typeAliasDescriptor.initialize(
                    typeParameterDescriptors,
                    storageManager.createRecursionTolerantLazyValue(
                            () -> typeResolver.resolveAbbreviatedType(scopeWithTypeParameters, typeReference, trace),
                            ErrorUtils.createErrorType("Recursive type alias expansion for " + typeAliasDescriptor.getName().asString())
                    ),
                    storageManager.createRecursionTolerantLazyValue(
                            () -> typeResolver.resolveExpandedTypeForTypeAlias(typeAliasDescriptor),
                            ErrorUtils.createErrorType("Recursive type alias expansion for " + typeAliasDescriptor.getName().asString())
                    )
            );
        }

        trace.record(TYPE_ALIAS, typeAlias, typeAliasDescriptor);
        return typeAliasDescriptor;
    }

    private static void checkNoGenericBoundsOnTypeAliasParameters(@NotNull KtTypeAlias typeAlias, @NotNull BindingTrace trace) {
        for (KtTypeParameter typeParameter : typeAlias.getTypeParameters()) {
            KtTypeReference bound = typeParameter.getExtendsBound();
            if (bound != null) {
                trace.report(BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED.on(bound));
            }
        }
    }

    @NotNull
    public PropertyDescriptor resolveDestructuringDeclarationEntryAsProperty(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scopeForDeclarationResolution,
            @NotNull LexicalScope scopeForInitializerResolution,
            @NotNull KtDestructuringDeclarationEntry entry,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        KtDestructuringDeclaration destructuringDeclaration = (KtDestructuringDeclaration) entry.getParent();
        KtExpression initializer = destructuringDeclaration.getInitializer();

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, scopeForDeclarationResolution, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE, languageVersionSettings, dataFlowValueFactory
        );

        ExpressionReceiver receiver = createReceiverForDestructuringDeclaration(destructuringDeclaration, context);

        int componentIndex = destructuringDeclaration.getEntries().indexOf(entry);
        KotlinType componentType = destructuringDeclarationResolver.resolveInitializer(entry, receiver, initializer, context, componentIndex);

        return resolveAsPropertyDescriptor(
                containingDeclaration,
                scopeForDeclarationResolution,
                scopeForInitializerResolution,
                entry,
                trace,
                dataFlowInfo,
                VariableAsPropertyInfo.Companion.createFromDestructuringDeclarationEntry(componentType));
    }

    private ExpressionReceiver createReceiverForDestructuringDeclaration(
            @NotNull KtDestructuringDeclaration destructuringDeclaration,
            @NotNull ExpressionTypingContext context
    ) {
        KtExpression initializer = destructuringDeclaration.getInitializer();
        if (initializer == null) return null;

        KotlinType initializerType = expressionTypingServices.getTypeInfo(initializer, context).getType();
        if (initializerType == null) return null;

        return ExpressionReceiver.Companion.create(initializer, initializerType, context.trace.getBindingContext());
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scopeForDeclarationResolution,
            @NotNull LexicalScope scopeForInitializerResolution,
            @NotNull KtProperty property,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        return resolveAsPropertyDescriptor(
                containingDeclaration,
                scopeForDeclarationResolution,
                scopeForInitializerResolution,
                property,
                trace,
                dataFlowInfo,
                VariableAsPropertyInfo.Companion.createFromProperty(property));
    }

    @NotNull
    private PropertyDescriptor resolveAsPropertyDescriptor(
            @NotNull DeclarationDescriptor container,
            @NotNull LexicalScope scopeForDeclarationResolution,
            @NotNull LexicalScope scopeForInitializerResolution,
            @NotNull KtVariableDeclaration variableDeclaration,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull VariableAsPropertyInfo propertyInfo
    ) {
        KtModifierList modifierList = variableDeclaration.getModifierList();
        boolean isVar = variableDeclaration.isVar();

        Visibility visibility = resolveVisibilityFromModifiers(variableDeclaration, getDefaultVisibility(variableDeclaration, container));
        Modality modality = container instanceof ClassDescriptor
                            ? resolveMemberModalityFromModifiers(variableDeclaration,
                                                                 getDefaultModality(container, visibility, propertyInfo.getHasBody()),
                                                                 trace.getBindingContext(), container)
                            : Modality.FINAL;

        AnnotationSplitter.PropertyWrapper wrapper = new AnnotationSplitter.PropertyWrapper(variableDeclaration);

        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scopeForDeclarationResolution, modifierList, trace);
        AnnotationSplitter annotationSplitter =
                new AnnotationSplitter(storageManager, allAnnotations,
                                       () -> AnnotationSplitter.getTargetSet(false, trace.getBindingContext(), wrapper));

        Annotations propertyAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
                annotationSplitter.getAnnotationsForTargets(PROPERTY, FIELD, PROPERTY_DELEGATE_FIELD),
                annotationSplitter.getOtherAnnotations()));

        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                container,
                propertyAnnotations,
                modality,
                visibility,
                isVar,
                KtPsiUtil.safeName(variableDeclaration.getName()),
                CallableMemberDescriptor.Kind.DECLARATION,
                KotlinSourceElementKt.toSourceElement(variableDeclaration),
                modifierList != null && modifierList.hasModifier(KtTokens.LATEINIT_KEYWORD),
                modifierList != null && modifierList.hasModifier(KtTokens.CONST_KEYWORD),
                modifierList != null && PsiUtilsKt.hasExpectModifier(modifierList) && container instanceof PackageFragmentDescriptor ||
                container instanceof ClassDescriptor && ((ClassDescriptor) container).isExpect(),
                modifierList != null && PsiUtilsKt.hasActualModifier(modifierList),
                modifierList != null && modifierList.hasModifier(KtTokens.EXTERNAL_KEYWORD),
                propertyInfo.getHasDelegate()
        );
        wrapper.setDescriptor(propertyDescriptor);

        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
        LexicalScope scopeForDeclarationResolutionWithTypeParameters;
        LexicalScope scopeForInitializerResolutionWithTypeParameters;
        KotlinType receiverType = null;

        {
            List<KtTypeParameter> typeParameters = variableDeclaration.getTypeParameters();
            if (typeParameters.isEmpty()) {
                scopeForDeclarationResolutionWithTypeParameters = scopeForDeclarationResolution;
                scopeForInitializerResolutionWithTypeParameters = scopeForInitializerResolution;
                typeParameterDescriptors = Collections.emptyList();
            }
            else {
                LexicalWritableScope writableScopeForDeclarationResolution = new LexicalWritableScope(
                        scopeForDeclarationResolution, container, false, new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                        LexicalScopeKind.PROPERTY_HEADER);
                LexicalWritableScope writableScopeForInitializerResolution = new LexicalWritableScope(
                        scopeForInitializerResolution, container, false, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                        LexicalScopeKind.PROPERTY_HEADER);
                typeParameterDescriptors = resolveTypeParametersForDescriptor(
                        propertyDescriptor,
                        scopeForDeclarationResolution, typeParameters, trace);
                for (TypeParameterDescriptor descriptor : typeParameterDescriptors) {
                    writableScopeForDeclarationResolution.addClassifierDescriptor(descriptor);
                    writableScopeForInitializerResolution.addClassifierDescriptor(descriptor);
                }
                writableScopeForDeclarationResolution.freeze();
                writableScopeForInitializerResolution.freeze();
                resolveGenericBounds(variableDeclaration, propertyDescriptor, writableScopeForDeclarationResolution, typeParameterDescriptors, trace);
                scopeForDeclarationResolutionWithTypeParameters = writableScopeForDeclarationResolution;
                scopeForInitializerResolutionWithTypeParameters = writableScopeForInitializerResolution;
            }

            KtTypeReference receiverTypeRef = variableDeclaration.getReceiverTypeReference();
            if (receiverTypeRef != null) {
                receiverType = typeResolver.resolveType(scopeForDeclarationResolutionWithTypeParameters, receiverTypeRef, trace, true);
            }
        }

        ReceiverParameterDescriptor receiverDescriptor;
        if (receiverType != null) {
            receiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(
                    propertyDescriptor, receiverType, Annotations.Companion.getEMPTY()
            );
        }
        else {
            receiverDescriptor = null;
        }

        LexicalScope scopeForInitializer = ScopeUtils.makeScopeForPropertyInitializer(scopeForInitializerResolutionWithTypeParameters, propertyDescriptor);
        KotlinType propertyType = propertyInfo.getVariableType();
        KotlinType typeIfKnown = propertyType != null ? propertyType : variableTypeAndInitializerResolver.resolveTypeNullable(
                propertyDescriptor, scopeForInitializer,
                variableDeclaration, dataFlowInfo, /* local = */ trace, false
        );

        PropertyGetterDescriptorImpl getter = resolvePropertyGetterDescriptor(
                scopeForDeclarationResolutionWithTypeParameters,
                variableDeclaration,
                propertyDescriptor,
                annotationSplitter,
                trace,
                typeIfKnown,
                propertyInfo.getPropertyGetter(),
                propertyInfo.getHasDelegate());

        KotlinType type = typeIfKnown != null ? typeIfKnown : getter.getReturnType();

        assert type != null : "At least getter type must be initialized via resolvePropertyGetterDescriptor";

        variableTypeAndInitializerResolver.setConstantForVariableIfNeeded(
                propertyDescriptor, scopeForInitializer, variableDeclaration, dataFlowInfo, type, trace
        );

        propertyDescriptor.setType(type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(container), receiverDescriptor);

        PropertySetterDescriptor setter = resolvePropertySetterDescriptor(
                scopeForDeclarationResolutionWithTypeParameters,
                variableDeclaration,
                propertyDescriptor,
                annotationSplitter,
                trace,
                propertyInfo.getPropertySetter(),
                propertyInfo.getHasDelegate());

        propertyDescriptor.initialize(getter, setter);

        trace.record(BindingContext.VARIABLE, variableDeclaration, propertyDescriptor);
        return propertyDescriptor;
    }

    @NotNull
    /*package*/ static KotlinType transformAnonymousTypeIfNeeded(
            @NotNull DeclarationDescriptorWithVisibility descriptor,
            @NotNull KtDeclaration declaration,
            @NotNull KotlinType type,
            @NotNull BindingTrace trace,
            @NotNull Iterable<DeclarationSignatureAnonymousTypeTransformer> anonymousTypeTransformers
    ) {
        for (DeclarationSignatureAnonymousTypeTransformer transformer : anonymousTypeTransformers) {
            KotlinType transformedType = transformer.transformAnonymousType(descriptor, type);
            if (transformedType != null) {
                return transformedType;
            }
        }

        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        if (classifier == null || !DescriptorUtils.isAnonymousObject(classifier) || DescriptorUtils.isLocal(descriptor)) {
            return type;
        }

        if (!Visibilities.isPrivate(descriptor.getVisibility())) {
            if (type.getConstructor().getSupertypes().size() == 1) {
                return type.getConstructor().getSupertypes().iterator().next();
            }
            else {
                trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.getConstructor().getSupertypes()));
            }
        }

        return type;
    }

    @Nullable
    private PropertySetterDescriptor resolvePropertySetterDescriptor(
            @NotNull LexicalScope scopeWithTypeParameters,
            @NotNull KtVariableDeclaration property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull AnnotationSplitter annotationSplitter,
            @NotNull BindingTrace trace,
            @Nullable KtPropertyAccessor setter,
            boolean hasDelegate
    ) {
        PropertySetterDescriptorImpl setterDescriptor = null;
        if (setter != null) {
            Annotations annotations = new CompositeAnnotations(CollectionsKt.listOf(
                    annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER),
                    annotationResolver.resolveAnnotationsWithoutArguments(scopeWithTypeParameters, setter.getModifierList(), trace)));
            KtParameter parameter = setter.getParameter();

            setterDescriptor = new PropertySetterDescriptorImpl(
                    propertyDescriptor, annotations,
                    resolveMemberModalityFromModifiers(setter, propertyDescriptor.getModality(),
                                                       trace.getBindingContext(), propertyDescriptor.getContainingDeclaration()),
                    resolveVisibilityFromModifiers(setter, propertyDescriptor.getVisibility()),
                    /* isDefault = */ false, setter.hasModifier(EXTERNAL_KEYWORD),
                    property.hasModifier(KtTokens.INLINE_KEYWORD) || setter.hasModifier(KtTokens.INLINE_KEYWORD),
                    CallableMemberDescriptor.Kind.DECLARATION, null, KotlinSourceElementKt.toSourceElement(setter)
            );
            KtTypeReference returnTypeReference = setter.getReturnTypeReference();
            if (returnTypeReference != null) {
                KotlinType returnType = typeResolver.resolveType(scopeWithTypeParameters, returnTypeReference, trace, true);
                if (!KotlinBuiltIns.isUnit(returnType)) {
                    trace.report(WRONG_SETTER_RETURN_TYPE.on(returnTypeReference));
                }
            }

            if (parameter != null) {

                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case
                if (parameter.hasDefaultValue()) {
                    trace.report(SETTER_PARAMETER_WITH_DEFAULT_VALUE.on(parameter.getDefaultValue()));
                }

                KotlinType type;
                KtTypeReference typeReference = parameter.getTypeReference();
                if (typeReference == null) {
                    type = propertyDescriptor.getType(); // TODO : this maybe unknown at this point
                }
                else {
                    type = typeResolver.resolveType(scopeWithTypeParameters, typeReference, trace, true);
                    KotlinType inType = propertyDescriptor.getType();
                    if (!TypeUtils.equalTypes(type, inType)) {
                        trace.report(WRONG_SETTER_PARAMETER_TYPE.on(typeReference, inType, type));
                    }
                }

                ValueParameterDescriptorImpl valueParameterDescriptor =
                        resolveValueParameterDescriptor(scopeWithTypeParameters, setterDescriptor, parameter, 0, type, trace);
                setterDescriptor.initialize(valueParameterDescriptor);
            }
            else {
                setterDescriptor.initializeDefault();
            }

            trace.record(BindingContext.PROPERTY_ACCESSOR, setter, setterDescriptor);
        }
        else if (property.isVar()) {
            Annotations setterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
            setterDescriptor = DescriptorFactory.createSetter(propertyDescriptor, setterAnnotations, !hasDelegate,
                                                              /* isExternal = */ false, property.hasModifier(KtTokens.INLINE_KEYWORD),
                                                              propertyDescriptor.getSource());
        }

        if (!property.isVar()) {
            if (setter != null) {
                //                trace.getErrorHandler().genericError(setter.asElement().getNode(), "A 'val'-property cannot have a setter");
                trace.report(VAL_WITH_SETTER.on(setter));
            }
        }
        return setterDescriptor;
    }

    @NotNull
    private PropertyGetterDescriptorImpl resolvePropertyGetterDescriptor(
            @NotNull LexicalScope scopeForDeclarationResolution,
            @NotNull KtVariableDeclaration property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull AnnotationSplitter annotationSplitter,
            @NotNull BindingTrace trace,
            @Nullable KotlinType propertyTypeIfKnown,
            @Nullable KtPropertyAccessor getter,
            boolean hasDelegate
    ) {
        PropertyGetterDescriptorImpl getterDescriptor;
        KotlinType getterType;
        if (getter != null) {
            Annotations getterAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
                    annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER),
                    annotationResolver.resolveAnnotationsWithoutArguments(scopeForDeclarationResolution, getter.getModifierList(), trace)));

            getterDescriptor = new PropertyGetterDescriptorImpl(
                    propertyDescriptor, getterAnnotations,
                    resolveMemberModalityFromModifiers(getter, propertyDescriptor.getModality(),
                                                       trace.getBindingContext(), propertyDescriptor.getContainingDeclaration()),
                    resolveVisibilityFromModifiers(getter, propertyDescriptor.getVisibility()),
                    /* isDefault = */ false, getter.hasModifier(EXTERNAL_KEYWORD),
                    property.hasModifier(KtTokens.INLINE_KEYWORD) || getter.hasModifier(KtTokens.INLINE_KEYWORD),
                    CallableMemberDescriptor.Kind.DECLARATION, null, KotlinSourceElementKt.toSourceElement(getter)
            );
            getterType = determineGetterReturnType(scopeForDeclarationResolution, trace, getterDescriptor, getter, propertyTypeIfKnown);
            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor);
        }
        else {
            Annotations getterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER);
            getterDescriptor = DescriptorFactory.createGetter(propertyDescriptor, getterAnnotations, !hasDelegate,
                                                              /* isExternal = */ false, property.hasModifier(KtTokens.INLINE_KEYWORD));
            getterType = propertyTypeIfKnown;
        }

        getterDescriptor.initialize(getterType != null ? getterType : VariableTypeAndInitializerResolver.STUB_FOR_PROPERTY_WITHOUT_TYPE);

        return getterDescriptor;
    }

    @Nullable
    private KotlinType determineGetterReturnType(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            @NotNull PropertyGetterDescriptor getterDescriptor,
            @NotNull KtPropertyAccessor getter,
            @Nullable KotlinType propertyTypeIfKnown
    ) {
        KtTypeReference returnTypeReference = getter.getReturnTypeReference();
        if (returnTypeReference != null) {
            KotlinType explicitReturnType = typeResolver.resolveType(scope, returnTypeReference, trace, true);
            if (propertyTypeIfKnown != null && !TypeUtils.equalTypes(explicitReturnType, propertyTypeIfKnown)) {
                trace.report(WRONG_GETTER_RETURN_TYPE.on(returnTypeReference, propertyTypeIfKnown, explicitReturnType));
            }
            return explicitReturnType;
        }

        // If a property has no type specified in the PSI but the getter does (or has an initializer e.g. "val x get() = ..."),
        // infer the correct type for the getter but leave the error type for the property.
        // This is useful for an IDE quick fix which would add the type to the property
        KtProperty property = getter.getProperty();
        if (!property.hasDelegateExpressionOrInitializer() && property.getTypeReference() == null &&
            getter.hasBody() && !getter.hasBlockBody()) {
            return inferReturnTypeFromExpressionBody(trace, scope, DataFlowInfoFactory.EMPTY, getter, getterDescriptor);
        }

        return propertyTypeIfKnown;
    }

    @NotNull
    /*package*/ KotlinType inferReturnTypeFromExpressionBody(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KtDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor
    ) {
        return wrappedTypeFactory.createRecursionIntolerantDeferredType(trace, () -> {
            PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
            KotlinType type = expressionTypingServices.getBodyExpressionType(trace, scope, dataFlowInfo, function, functionDescriptor);
            KotlinType publicType = transformAnonymousTypeIfNeeded(functionDescriptor, function, type, trace, anonymousTypeTransformers);
            UnwrappedType approximatedType = typeApproximator.approximateDeclarationType(publicType, false, languageVersionSettings);
            KotlinType sanitizedType = declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings);
            functionsTypingVisitor.checkTypesForReturnStatements(function, trace, sanitizedType);
            return sanitizedType;
        });
    }

    @NotNull
    public PropertyDescriptor resolvePrimaryConstructorParameterToAProperty(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ValueParameterDescriptor valueParameter,
            @NotNull LexicalScope scope,
            @NotNull KtParameter parameter,
            BindingTrace trace
    ) {
        KotlinType type = resolveParameterType(scope, parameter, trace);
        Name name = parameter.getNameAsSafeName();
        boolean isMutable = parameter.isMutable();
        KtModifierList modifierList = parameter.getModifierList();

        if (modifierList != null) {
            if (modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                trace.report(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.on(parameter));
            }
        }

        AnnotationSplitter.PropertyWrapper propertyWrapper = new AnnotationSplitter.PropertyWrapper(parameter);
        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, parameter.getModifierList(), trace);
        AnnotationSplitter annotationSplitter =
                new AnnotationSplitter(storageManager, allAnnotations,
                                       () -> AnnotationSplitter.getTargetSet(true, trace.getBindingContext(), propertyWrapper));

        Annotations propertyAnnotations = new CompositeAnnotations(
                annotationSplitter.getAnnotationsForTargets(PROPERTY, FIELD),
                annotationSplitter.getOtherAnnotations());

        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                classDescriptor,
                propertyAnnotations,
                resolveMemberModalityFromModifiers(parameter, Modality.FINAL, trace.getBindingContext(), classDescriptor),
                resolveVisibilityFromModifiers(parameter, getDefaultVisibility(parameter, classDescriptor)),
                isMutable,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                KotlinSourceElementKt.toSourceElement(parameter),
                false,
                false,
                classDescriptor.isExpect(),
                modifierList != null && PsiUtilsKt.hasActualModifier(modifierList),
                false,
                false
        );
        propertyWrapper.setDescriptor(propertyDescriptor);
        propertyDescriptor.setType(
                type, Collections.emptyList(), getDispatchReceiverParameterIfNeeded(classDescriptor), (ReceiverParameterDescriptor) null
        );

        Annotations setterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
        Annotations getterAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
                annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER)));

        PropertyGetterDescriptorImpl getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, getterAnnotations);
        PropertySetterDescriptor setter =
                propertyDescriptor.isVar() ? DescriptorFactory.createDefaultSetter(propertyDescriptor, setterAnnotations) : null;

        propertyDescriptor.initialize(getter, setter);
        getter.initialize(propertyDescriptor.getType());

        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, propertyDescriptor);
        trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter, propertyDescriptor);
        return propertyDescriptor;
    }

    public static void checkBounds(@NotNull KtTypeReference typeReference, @NotNull KotlinType type, @NotNull BindingTrace trace) {
        if (KotlinTypeKt.isError(type)) return;

        KtTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement == null) return;

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        assert parameters.size() == arguments.size();

        List<KtTypeReference> ktTypeArguments = typeElement.getTypeArgumentsAsTypes();

        // A type reference from Kotlin code can yield a flexible type only if it's `ft<T1, T2>`, whose bounds should not be checked
        if (FlexibleTypesKt.isFlexible(type) && !DynamicTypesKt.isDynamic(type)) {
            assert ktTypeArguments.size() == 2
                    : "Flexible type cannot be denoted in Kotlin otherwise than as ft<T1, T2>, but was: "
                      + PsiUtilsKt.getElementTextWithContext(typeReference);
            // it's really ft<Foo, Bar>
            FlexibleType flexibleType = FlexibleTypesKt.asFlexibleType(type);
            checkBounds(ktTypeArguments.get(0), flexibleType.getLowerBound(), trace);
            checkBounds(ktTypeArguments.get(1), flexibleType.getUpperBound(), trace);
            return;
        }

        // If the numbers of type arguments do not match, the error has been already reported in TypeResolver
        if (ktTypeArguments.size() != arguments.size()) return;

        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        for (int i = 0; i < ktTypeArguments.size(); i++) {
            KtTypeReference ktTypeArgument = ktTypeArguments.get(i);
            if (ktTypeArgument == null) continue;

            KotlinType typeArgument = arguments.get(i).getType();
            checkBounds(ktTypeArgument, typeArgument, trace);

            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
            checkBounds(ktTypeArgument, typeArgument, typeParameterDescriptor, substitutor, trace);
        }
    }

    public static void checkBounds(
            @NotNull KtTypeReference jetTypeArgument,
            @NotNull KotlinType typeArgument,
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull TypeSubstitutor substitutor,
            @NotNull BindingTrace trace
    ) {
        for (KotlinType bound : typeParameterDescriptor.getUpperBounds()) {
            KotlinType substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT);
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(typeArgument, substitutedBound)) {
                trace.report(UPPER_BOUND_VIOLATED.on(jetTypeArgument, substitutedBound, typeArgument));
            }
        }
    }

    public static void checkBoundsInTypeAlias(
            @NotNull TypeAliasExpansionReportStrategy reportStrategy,
            @NotNull KotlinType unsubstitutedArgument,
            @NotNull KotlinType typeArgument,
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull TypeSubstitutor substitutor
    ) {
        for (KotlinType bound : typeParameterDescriptor.getUpperBounds()) {
            KotlinType substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT);
            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(typeArgument, substitutedBound)) {
                reportStrategy.boundsViolationInSubstitution(substitutedBound, unsubstitutedArgument, typeArgument, typeParameterDescriptor);
            }
        }
    }

    public static boolean checkHasOuterClassInstance(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            @NotNull PsiElement reportErrorsOn,
            @NotNull ClassDescriptor target
    ) {
        ClassDescriptor classDescriptor = getContainingClass(scope);

        if (!isInsideOuterClassOrItsSubclass(classDescriptor, target)) {
            return true;
        }

        while (classDescriptor != null) {
            if (isSubclass(classDescriptor, target)) {
                return true;
            }

            if (isStaticNestedClass(classDescriptor)) {
                trace.report(INACCESSIBLE_OUTER_CLASS_EXPRESSION.on(reportErrorsOn, classDescriptor));
                return false;
            }
            classDescriptor = getParentOfType(classDescriptor, ClassDescriptor.class, true);
        }
        return true;
    }

    private static boolean isInsideOuterClassOrItsSubclass(@Nullable DeclarationDescriptor nested, @NotNull ClassDescriptor outer) {
        if (nested == null) return false;

        if (nested instanceof ClassDescriptor && isSubclass((ClassDescriptor) nested, outer)) return true;

        return isInsideOuterClassOrItsSubclass(nested.getContainingDeclaration(), outer);
    }

    @Nullable
    public static ClassDescriptor getContainingClass(@NotNull LexicalScope scope) {
        return getParentOfType(scope.getOwnerDescriptor(), ClassDescriptor.class, false);
    }
}
