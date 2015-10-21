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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import kotlin.CollectionsKt;
import kotlin.SetsKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationSplitter;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations;
import org.jetbrains.kotlin.descriptors.impl.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.dataClassUtils.DataClassUtilsKt;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor;

import java.util.*;

import static org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.resolveModalityFromModifiers;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.resolveVisibilityFromModifiers;

public class DescriptorResolver {
    public static final Name COPY_METHOD_NAME = Name.identifier("copy");

    @NotNull private final TypeResolver typeResolver;
    @NotNull private final AnnotationResolver annotationResolver;
    @NotNull private final ExpressionTypingServices expressionTypingServices;
    @NotNull private final DelegatedPropertyResolver delegatedPropertyResolver;
    @NotNull private final StorageManager storageManager;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;

    public DescriptorResolver(
            @NotNull AnnotationResolver annotationResolver,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull DelegatedPropertyResolver delegatedPropertyResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull StorageManager storageManager,
            @NotNull TypeResolver typeResolver,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator
    ) {
        this.annotationResolver = annotationResolver;
        this.builtIns = builtIns;
        this.delegatedPropertyResolver = delegatedPropertyResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.storageManager = storageManager;
        this.typeResolver = typeResolver;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
    }

    public List<KotlinType> resolveSupertypes(
            @NotNull LexicalScope scope,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull KtClassOrObject jetClass,
            BindingTrace trace
    ) {
        List<KotlinType> supertypes = Lists.newArrayList();
        List<KtDelegationSpecifier> delegationSpecifiers = jetClass.getDelegationSpecifiers();
        Collection<KotlinType> declaredSupertypes = resolveDelegationSpecifiers(
                scope,
                delegationSpecifiers,
                typeResolver, trace, false);

        for (KotlinType declaredSupertype : declaredSupertypes) {
            addValidSupertype(supertypes, declaredSupertype);
        }

        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS && !containsClass(supertypes)) {
            supertypes.add(0, builtIns.getEnumType(classDescriptor.getDefaultType()));
        }

        if (supertypes.isEmpty()) {
            KotlinType defaultSupertype = getDefaultSupertype(jetClass, trace, classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS);
            addValidSupertype(supertypes, defaultSupertype);
        }

        return supertypes;
    }

    private static void addValidSupertype(List<KotlinType> supertypes, KotlinType declaredSupertype) {
        if (!declaredSupertype.isError()) {
            supertypes.add(declaredSupertype);
        }
    }

    private boolean containsClass(Collection<KotlinType> result) {
        for (KotlinType type : result) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.INTERFACE) {
                return true;
            }
        }
        return false;
    }

    private KotlinType getDefaultSupertype(KtClassOrObject jetClass, BindingTrace trace, boolean isAnnotation) {
        // TODO : beautify
        if (jetClass instanceof KtEnumEntry) {
            KtClassOrObject parent = KtStubbedPsiUtil.getContainingDeclaration(jetClass, KtClassOrObject.class);
            ClassDescriptor parentDescriptor = trace.getBindingContext().get(BindingContext.CLASS, parent);
            if (parentDescriptor.getTypeConstructor().getParameters().isEmpty()) {
                return parentDescriptor.getDefaultType();
            }
            else {
                trace.report(NO_GENERICS_IN_SUPERTYPE_SPECIFIER.on(jetClass.getNameIdentifier()));
                return ErrorUtils.createErrorType("Supertype not specified");
            }
        }
        else if (isAnnotation) {
            return builtIns.getAnnotationType();
        }
        return builtIns.getAnyType();
    }

    public Collection<KotlinType> resolveDelegationSpecifiers(
            LexicalScope extensibleScope,
            List<KtDelegationSpecifier> delegationSpecifiers,
            @NotNull TypeResolver resolver,
            BindingTrace trace,
            boolean checkBounds
    ) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<KotlinType> result = Lists.newArrayList();
        for (KtDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            KtTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                KotlinType supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds);
                if (DynamicTypesKt.isDynamic(supertype)) {
                    trace.report(DYNAMIC_SUPERTYPE.on(typeReference));
                }
                else {
                    result.add(supertype);
                    KtTypeElement bareSuperType = checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.getTypeElement());
                    checkProjectionsInImmediateArguments(trace, bareSuperType);
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

    private static void checkProjectionsInImmediateArguments(@NotNull BindingTrace trace, @Nullable KtTypeElement typeElement) {
        if (typeElement instanceof KtUserType) {
            KtUserType userType = (KtUserType) typeElement;
            List<KtTypeProjection> typeArguments = userType.getTypeArguments();
            for (KtTypeProjection typeArgument : typeArguments) {
                if (typeArgument.getProjectionKind() != KtProjectionKind.NONE) {
                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
                }
            }
        }
    }

    @NotNull
    public static SimpleFunctionDescriptor createComponentFunctionDescriptor(
            int parameterIndex,
            @NotNull PropertyDescriptor property,
            @NotNull ValueParameterDescriptor parameter,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        Name functionName = DataClassUtilsKt.createComponentName(parameterIndex);
        KotlinType returnType = property.getType();

        SimpleFunctionDescriptorImpl functionDescriptor = SimpleFunctionDescriptorImpl.create(
                classDescriptor,
                Annotations.Companion.getEMPTY(),
                functionName,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                parameter.getSource()
        );

        functionDescriptor.initialize(
                null,
                classDescriptor.getThisAsReceiverParameter(),
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.<ValueParameterDescriptor>emptyList(),
                returnType,
                Modality.FINAL,
                property.getVisibility()
        );
        functionDescriptor.setOperator(true);

        trace.record(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter, functionDescriptor);

        return functionDescriptor;
    }

    @NotNull
    public static SimpleFunctionDescriptor createCopyFunctionDescriptor(
            @NotNull Collection<ValueParameterDescriptor> constructorParameters,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        KotlinType returnType = classDescriptor.getDefaultType();

        SimpleFunctionDescriptorImpl functionDescriptor = SimpleFunctionDescriptorImpl.create(
                classDescriptor,
                Annotations.Companion.getEMPTY(),
                COPY_METHOD_NAME,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                classDescriptor.getSource()
        );

        List<ValueParameterDescriptor> parameterDescriptors = Lists.newArrayList();

        for (ValueParameterDescriptor parameter : constructorParameters) {
            PropertyDescriptor propertyDescriptor = trace.getBindingContext().get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter);
            // If parameter hasn't corresponding property, so it mustn't have default value as a parameter in copy function for data class
            boolean declaresDefaultValue = propertyDescriptor != null;
            ValueParameterDescriptorImpl parameterDescriptor =
                    new ValueParameterDescriptorImpl(functionDescriptor, null, parameter.getIndex(), parameter.getAnnotations(),
                                                     parameter.getName(), parameter.getType(),
                                                     declaresDefaultValue,
                                                     parameter.isCrossinline(),
                                                     parameter.isNoinline(),
                                                     parameter.getVarargElementType(), parameter.getSource());
            parameterDescriptors.add(parameterDescriptor);
            if (declaresDefaultValue) {
                trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameterDescriptor, propertyDescriptor);
            }
        }

        functionDescriptor.initialize(
                null,
                classDescriptor.getThisAsReceiverParameter(),
                Collections.<TypeParameterDescriptor>emptyList(),
                parameterDescriptors,
                returnType,
                Modality.FINAL,
                Visibilities.PUBLIC
        );

        trace.record(BindingContext.DATA_CLASS_COPY_FUNCTION, classDescriptor, functionDescriptor);
        return functionDescriptor;
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
            LexicalScope scope, FunctionDescriptor owner, KtParameter valueParameter, int index, KotlinType type, BindingTrace trace
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

        ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                owner,
                null,
                index,
                valueParameterAnnotations,
                KtPsiUtil.safeName(valueParameter.getName()),
                variableType,
                valueParameter.hasDefaultValue(),
                valueParameter.hasModifier(CROSSINLINE_KEYWORD),
                valueParameter.hasModifier(NOINLINE_KEYWORD),
                varargElementType,
                KotlinSourceElementKt.toSourceElement(valueParameter)
        );

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    @NotNull
    private KotlinType getVarargParameterType(@NotNull KotlinType elementType) {
        KotlinType primitiveArrayType = builtIns.getPrimitiveArrayJetTypeByPrimitiveJetType(elementType);
        if (primitiveArrayType != null) {
            return primitiveArrayType;
        }
        return builtIns.getArrayType(Variance.OUT_VARIANCE, elementType);
    }

    public List<TypeParameterDescriptorImpl> resolveTypeParametersForCallableDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalWritableScope extensibleScope,
            LexicalScope scopeForAnnotationsResolve,
            List<KtTypeParameter> typeParameters,
            BindingTrace trace
    ) {
        List<TypeParameterDescriptorImpl> result = new ArrayList<TypeParameterDescriptorImpl>();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            KtTypeParameter typeParameter = typeParameters.get(i);
            result.add(resolveTypeParameterForCallableDescriptor(
                    containingDescriptor, extensibleScope, scopeForAnnotationsResolve, typeParameter, i, trace));
        }
        return result;
    }

    private TypeParameterDescriptorImpl resolveTypeParameterForCallableDescriptor(
            DeclarationDescriptor containingDescriptor,
            LexicalWritableScope extensibleScope,
            LexicalScope scopeForAnnotationsResolve,
            KtTypeParameter typeParameter,
            int index,
            BindingTrace trace
    ) {
        if (typeParameter.getVariance() != Variance.INVARIANT) {
            assert !(containingDescriptor instanceof ClassifierDescriptor) : "This method is intended for functions/properties";
            trace.report(VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY.on(typeParameter));
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
                KotlinSourceElementKt.toSourceElement(typeParameter)
        );
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
        extensibleScope.addClassifierDescriptor(typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    @NotNull
    public static ConstructorDescriptorImpl createAndRecordPrimaryConstructorForObject(
            @Nullable KtClassOrObject object,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        ConstructorDescriptorImpl constructorDescriptor =
                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, KotlinSourceElementKt.toSourceElement(object));
        if (object != null) {
            KtPrimaryConstructor primaryConstructor = object.getPrimaryConstructor();
            trace.record(CONSTRUCTOR, primaryConstructor != null ? primaryConstructor : object, constructorDescriptor);
        }
        return constructorDescriptor;
    }

    static final class UpperBoundCheckerTask {
        KtTypeReference upperBound;
        KotlinType upperBoundType;

        private UpperBoundCheckerTask(KtTypeReference upperBound, KotlinType upperBoundType) {
            this.upperBound = upperBound;
            this.upperBoundType = upperBoundType;
        }
    }

    public KotlinType resolveTypeParameterExtendsBound(
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull KtTypeReference extendsBound,
            LexicalScope scope,
            BindingTrace trace
    ) {
        KotlinType type = typeResolver.resolveType(scope, extendsBound, trace, false);
        if (type.getConstructor().equals(typeParameterDescriptor.getTypeConstructor())) {
            trace.report(Errors.CYCLIC_GENERIC_UPPER_BOUND.on(extendsBound));
            type = ErrorUtils.createErrorType("Cyclic upper bound: " + type);
        }
        return type;
    }

    public void resolveGenericBounds(
            @NotNull KtTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            LexicalScope scope,
            List<TypeParameterDescriptorImpl> parameters,
            BindingTrace trace
    ) {
        List<UpperBoundCheckerTask> deferredUpperBoundCheckerTasks = Lists.newArrayList();

        List<KtTypeParameter> typeParameters = declaration.getTypeParameters();
        Map<Name, TypeParameterDescriptorImpl> parameterByName = Maps.newHashMap();
        for (int i = 0; i < typeParameters.size(); i++) {
            KtTypeParameter jetTypeParameter = typeParameters.get(i);
            TypeParameterDescriptorImpl typeParameterDescriptor = parameters.get(i);

            parameterByName.put(typeParameterDescriptor.getName(), typeParameterDescriptor);

            KtTypeReference extendsBound = jetTypeParameter.getExtendsBound();
            if (extendsBound != null) {
                KotlinType type = resolveTypeParameterExtendsBound(typeParameterDescriptor, extendsBound, scope, trace);
                typeParameterDescriptor.addUpperBound(type);
                deferredUpperBoundCheckerTasks.add(new UpperBoundCheckerTask(extendsBound, type));
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
                deferredUpperBoundCheckerTasks.add(new UpperBoundCheckerTask(boundTypeReference, bound));
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

            checkConflictingUpperBounds(trace, parameter, typeParameters.get(parameter.getIndex()));
        }

        if (!(declaration instanceof KtClass)) {
            for (UpperBoundCheckerTask checkerTask : deferredUpperBoundCheckerTasks) {
                checkUpperBoundType(checkerTask.upperBound, checkerTask.upperBoundType, trace);
            }

            checkNamesInConstraints(declaration, descriptor, scope, trace);
        }
    }

    public static void checkConflictingUpperBounds(
            @NotNull BindingTrace trace,
            @NotNull TypeParameterDescriptor parameter,
            @NotNull KtTypeParameter typeParameter
    ) {
        if (KotlinBuiltIns.isNothing(parameter.getUpperBoundsAsType())) {
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

            ClassifierDescriptor classifier = ScopeUtilsKt.asJetScope(scope).getClassifier(name, NoLookupLocation.UNSORTED);
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
            ClassifierDescriptor descriptor = upperBoundType.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor) {
                if (((ClassDescriptor) descriptor).getModality() == Modality.SEALED) return;
            }
            trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
        }
        if (DynamicTypesKt.isDynamic(upperBoundType)) {
            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
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
        VariableDescriptor variableDescriptor = new LocalVariableDescriptor(
                scope.getOwnerDescriptor(),
                annotationResolver.resolveAnnotationsWithArguments(scope, parameter.getModifierList(), trace),
                KtPsiUtil.safeName(parameter.getName()),
                type,
                false,
                KotlinSourceElementKt.toSourceElement(parameter)
        );
        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
        // Type annotations also should be resolved
        ForceResolveUtil.forceResolveAllContents(type.getAnnotations());
        return variableDescriptor;
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(
            LexicalScope scope,
            KtVariableDeclaration variable,
            DataFlowInfo dataFlowInfo,
            BindingTrace trace
    ) {
        DeclarationDescriptor containingDeclaration = scope.getOwnerDescriptor();
        VariableDescriptor result;
        KotlinType type;
        // SCRIPT: Create property descriptors
        if (KtPsiUtil.isScriptDeclaration(variable)) {
            PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                    containingDeclaration,
                    annotationResolver.resolveAnnotationsWithArguments(scope, variable.getModifierList(), trace),
                    Modality.FINAL,
                    Visibilities.INTERNAL,
                    variable.isVar(),
                    KtPsiUtil.safeName(variable.getName()),
                    CallableMemberDescriptor.Kind.DECLARATION,
                    KotlinSourceElementKt.toSourceElement(variable),
                    /* lateInit = */ false,
                    /* isConst = */ false
            );
            // For a local variable the type must not be deferred
            type = getVariableType(propertyDescriptor, scope, variable, dataFlowInfo, false, trace);

            ReceiverParameterDescriptor receiverParameter = ((ScriptDescriptor) containingDeclaration).getThisAsReceiverParameter();
            propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(), receiverParameter, (KotlinType) null);
            initializeWithDefaultGetterSetter(propertyDescriptor);
            trace.record(BindingContext.VARIABLE, variable, propertyDescriptor);
            result = propertyDescriptor;
        }
        else {
            LocalVariableDescriptor variableDescriptor =
                    resolveLocalVariableDescriptorWithType(scope, variable, null, trace);
            // For a local variable the type must not be deferred
            type = getVariableType(variableDescriptor, scope, variable, dataFlowInfo, false, trace);
            variableDescriptor.setOutType(type);
            result = variableDescriptor;
        }
        // Type annotations also should be resolved
        ForceResolveUtil.forceResolveAllContents(type.getAnnotations());
        return result;
    }

    private static void initializeWithDefaultGetterSetter(PropertyDescriptorImpl propertyDescriptor) {
        PropertyGetterDescriptorImpl getter = propertyDescriptor.getGetter();
        if (getter == null && !Visibilities.isPrivate(propertyDescriptor.getVisibility())) {
            getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.Companion.getEMPTY());
            getter.initialize(propertyDescriptor.getType());
        }

        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        if (setter == null && propertyDescriptor.isVar()) {
            setter = DescriptorFactory.createDefaultSetter(propertyDescriptor, Annotations.Companion.getEMPTY());
        }
        propertyDescriptor.initialize(getter, setter);
    }

    @NotNull
    public LocalVariableDescriptor resolveLocalVariableDescriptorWithType(
            @NotNull LexicalScope scope,
            @NotNull KtVariableDeclaration variable,
            @Nullable KotlinType type,
            @NotNull BindingTrace trace
    ) {
        LocalVariableDescriptor variableDescriptor = new LocalVariableDescriptor(
                scope.getOwnerDescriptor(),
                annotationResolver.resolveAnnotationsWithArguments(scope, variable.getModifierList(), trace),
                KtPsiUtil.safeName(variable.getName()),
                type,
                variable.isVar(),
                KotlinSourceElementKt.toSourceElement(variable)
        );
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull LexicalScope scope,
            @NotNull KtProperty property,
            @NotNull final BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        KtModifierList modifierList = property.getModifierList();
        boolean isVar = property.isVar();

        boolean hasBody = hasBody(property);
        Visibility visibility = resolveVisibilityFromModifiers(property, getDefaultVisibility(property, containingDeclaration));
        Modality modality = containingDeclaration instanceof ClassDescriptor
                            ? resolveModalityFromModifiers(property, getDefaultModality(containingDeclaration, visibility, hasBody))
                            : Modality.FINAL;

        final AnnotationSplitter.PropertyWrapper wrapper = new AnnotationSplitter.PropertyWrapper();

        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace);
        AnnotationSplitter annotationSplitter =
                new AnnotationSplitter(storageManager, allAnnotations, new Function0<Set<AnnotationUseSiteTarget>>() {
            @Override
            public Set<AnnotationUseSiteTarget> invoke() {
                return AnnotationSplitter.getTargetSet(false, trace.getBindingContext(), wrapper);
            }
        });

        Annotations propertyAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
                annotationSplitter.getAnnotationsForTargets(PROPERTY, FIELD),
                annotationSplitter.getOtherAnnotations()));

        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                containingDeclaration,
                propertyAnnotations,
                modality,
                visibility,
                isVar,
                KtPsiUtil.safeName(property.getName()),
                CallableMemberDescriptor.Kind.DECLARATION,
                KotlinSourceElementKt.toSourceElement(property),
                modifierList != null && modifierList.hasModifier(KtTokens.LATEINIT_KEYWORD),
                modifierList != null && modifierList.hasModifier(KtTokens.CONST_KEYWORD)
        );
        wrapper.setProperty(propertyDescriptor);

        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
        LexicalScope scopeWithTypeParameters;
        KotlinType receiverType = null;

        {
            List<KtTypeParameter> typeParameters = property.getTypeParameters();
            if (typeParameters.isEmpty()) {
                scopeWithTypeParameters = scope;
                typeParameterDescriptors = Collections.emptyList();
            }
            else {
                LexicalWritableScope writableScope = new LexicalWritableScope(
                        scope, containingDeclaration, false, null, new TraceBasedRedeclarationHandler(trace),
                        "Scope with type parameters of a property");
                typeParameterDescriptors = resolveTypeParametersForCallableDescriptor(
                        propertyDescriptor, writableScope, scope, typeParameters, trace);
                writableScope.changeLockLevel(WritableScope.LockLevel.READING);
                resolveGenericBounds(property, propertyDescriptor, writableScope, typeParameterDescriptors, trace);
                scopeWithTypeParameters = writableScope;
            }

            KtTypeReference receiverTypeRef = property.getReceiverTypeReference();
            if (receiverTypeRef != null) {
                receiverType = typeResolver.resolveType(scopeWithTypeParameters, receiverTypeRef, trace, true);
            }
        }

        ReceiverParameterDescriptor receiverDescriptor =
                DescriptorFactory.createExtensionReceiverParameterForCallable(propertyDescriptor, receiverType);

        ReceiverParameterDescriptor implicitInitializerReceiver = property.hasDelegate() ? null : receiverDescriptor;

        LexicalScope propertyScope = JetScopeUtils.getPropertyDeclarationInnerScope(propertyDescriptor, scope, typeParameterDescriptors,
                                                                                    implicitInitializerReceiver, trace);

        KotlinType type = getVariableType(propertyDescriptor, propertyScope, property, dataFlowInfo, true, trace);

        propertyDescriptor.setType(type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(containingDeclaration),
                                   receiverDescriptor);

        PropertyGetterDescriptorImpl getter = resolvePropertyGetterDescriptor(
                scopeWithTypeParameters, property, propertyDescriptor, annotationSplitter, trace);
        PropertySetterDescriptor setter = resolvePropertySetterDescriptor(
                scopeWithTypeParameters, property, propertyDescriptor, annotationSplitter, trace);

        propertyDescriptor.initialize(getter, setter);

        trace.record(BindingContext.VARIABLE, property, propertyDescriptor);
        return propertyDescriptor;
    }

    /*package*/
    static boolean hasBody(KtProperty property) {
        boolean hasBody = property.hasDelegateExpressionOrInitializer();
        if (!hasBody) {
            KtPropertyAccessor getter = property.getGetter();
            if (getter != null && getter.hasBody()) {
                hasBody = true;
            }
            KtPropertyAccessor setter = property.getSetter();
            if (!hasBody && setter != null && setter.hasBody()) {
                hasBody = true;
            }
        }
        return hasBody;
    }

    @NotNull
    private KotlinType getVariableType(
            @NotNull final VariableDescriptorWithInitializerImpl variableDescriptor,
            @NotNull final LexicalScope scope,
            @NotNull final KtVariableDeclaration variable,
            @NotNull final DataFlowInfo dataFlowInfo,
            boolean notLocal,
            @NotNull final BindingTrace trace
    ) {
        KtTypeReference propertyTypeRef = variable.getTypeReference();

        boolean hasDelegate = variable instanceof KtProperty && ((KtProperty) variable).hasDelegateExpression();
        if (propertyTypeRef == null) {
            if (!variable.hasInitializer()) {
                if (hasDelegate && variableDescriptor instanceof PropertyDescriptor) {
                    final KtProperty property = (KtProperty) variable;
                    if (property.hasDelegateExpression()) {
                        return DeferredType.createRecursionIntolerant(
                                storageManager,
                                trace,
                                new Function0<KotlinType>() {
                                    @Override
                                    public KotlinType invoke() {
                                        return resolveDelegatedPropertyType(property, (PropertyDescriptor) variableDescriptor, scope,
                                                                            property.getDelegateExpression(), dataFlowInfo, trace);
                                    }
                                });
                    }
                }
                if (!notLocal) {
                    trace.report(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.on(variable));
                }
                return ErrorUtils.createErrorType("No type, no body");
            }
            else {
                if (notLocal) {
                    return DeferredType.createRecursionIntolerant(
                            storageManager,
                            trace,
                            new Function0<KotlinType>() {
                                @Override
                                public KotlinType invoke() {
                                    PreliminaryDeclarationVisitor.Companion.createForDeclaration(variable, trace);
                                    KotlinType
                                            initializerType = resolveInitializerType(scope, variable.getInitializer(), dataFlowInfo, trace);
                                    setConstantForVariableIfNeeded(variableDescriptor, scope, variable, dataFlowInfo, initializerType, trace);
                                    return transformAnonymousTypeIfNeeded(variableDescriptor, variable, initializerType, trace);
                                }
                            }
                    );
                }
                else {
                    KotlinType initializerType = resolveInitializerType(scope, variable.getInitializer(), dataFlowInfo, trace);
                    setConstantForVariableIfNeeded(variableDescriptor, scope, variable, dataFlowInfo, initializerType, trace);
                    return initializerType;
                }
            }
        }
        else {
            KotlinType type = typeResolver.resolveType(scope, propertyTypeRef, trace, true);
            setConstantForVariableIfNeeded(variableDescriptor, scope, variable, dataFlowInfo, type, trace);
            return type;
        }
    }

    private void setConstantForVariableIfNeeded(
            @NotNull final VariableDescriptorWithInitializerImpl variableDescriptor,
            @NotNull final LexicalScope scope,
            @NotNull final KtVariableDeclaration variable,
            @NotNull final DataFlowInfo dataFlowInfo,
            @NotNull final KotlinType variableType,
            @NotNull final BindingTrace trace
    ) {
        if (!shouldRecordInitializerForProperty(variableDescriptor, variableType)) return;

        if (!variable.hasInitializer()) return;

        variableDescriptor.setCompileTimeInitializer(
            storageManager.createRecursionTolerantNullableLazyValue(new Function0<ConstantValue<?>>() {
                @Nullable
                @Override
                public ConstantValue<?> invoke() {
                    KtExpression initializer = variable.getInitializer();
                    KotlinType initializerType = expressionTypingServices.safeGetType(scope, initializer, variableType, dataFlowInfo, trace);
                    CompileTimeConstant<?> constant = constantExpressionEvaluator.evaluateExpression(initializer, trace, initializerType);

                    if (constant == null) return null;

                    if (constant.getUsesNonConstValAsConstant() && variableDescriptor.isConst()) {
                        trace.report(Errors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION.on(initializer));
                    }

                    return constant.toConstantValue(initializerType);
                }
            }, null)
        );
    }

    @NotNull
    private KotlinType resolveDelegatedPropertyType(
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull LexicalScope scope,
            @NotNull KtExpression delegateExpression,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        LexicalScope accessorScope = JetScopeUtils.makeScopeForPropertyAccessor(propertyDescriptor, scope, trace);

        KotlinType type = delegatedPropertyResolver.resolveDelegateExpression(
                delegateExpression, property, propertyDescriptor, scope, accessorScope, trace, dataFlowInfo);

        if (type != null) {
            KotlinType getterReturnType = delegatedPropertyResolver
                    .getDelegatedPropertyGetMethodReturnType(propertyDescriptor, delegateExpression, type, trace, accessorScope);
            if (getterReturnType != null) {
                return getterReturnType;
            }
        }
        return ErrorUtils.createErrorType("Type from delegate");
    }

    @Nullable
    /*package*/ static KotlinType transformAnonymousTypeIfNeeded(
            @NotNull DeclarationDescriptorWithVisibility descriptor,
            @NotNull KtNamedDeclaration declaration,
            @NotNull KotlinType type,
            @NotNull BindingTrace trace
    ) {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        if (classifier == null || !DescriptorUtils.isAnonymousObject(classifier) || DescriptorUtils.isLocal(descriptor)) {
            return type;
        }

        boolean definedInClass = DescriptorUtils.getParentOfType(descriptor, ClassDescriptor.class) != null;
        if (!definedInClass || !Visibilities.isPrivate(descriptor.getVisibility())) {
            if (type.getConstructor().getSupertypes().size() == 1) {
                return type.getConstructor().getSupertypes().iterator().next();
            }
            else {
                trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.getConstructor().getSupertypes()));
            }
        }

        return type;
    }

    @NotNull
    private KotlinType resolveInitializerType(
            @NotNull LexicalScope scope,
            @NotNull KtExpression initializer,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        return expressionTypingServices.safeGetType(scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, trace);
    }

    @Nullable
    private PropertySetterDescriptor resolvePropertySetterDescriptor(
            @NotNull LexicalScope scope,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull AnnotationSplitter annotationSplitter,
            BindingTrace trace
    ) {
        KtPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptorImpl setterDescriptor = null;
        if (setter != null) {
            Annotations annotations = new CompositeAnnotations(CollectionsKt.listOf(
                    annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER),
                    annotationResolver.resolveAnnotationsWithoutArguments(scope, setter.getModifierList(), trace)));
            KtParameter parameter = setter.getParameter();

            setterDescriptor = new PropertySetterDescriptorImpl(propertyDescriptor, annotations,
                                                                resolveModalityFromModifiers(setter, propertyDescriptor.getModality()),
                                                                resolveVisibilityFromModifiers(setter, propertyDescriptor.getVisibility()),
                                                                setter.hasBody(), false, setter.hasModifier(EXTERNAL_KEYWORD),
                                                                CallableMemberDescriptor.Kind.DECLARATION, null, KotlinSourceElementKt
                                                                        .toSourceElement(setter));
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
                    type = typeResolver.resolveType(scope, typeReference, trace, true);
                    KotlinType inType = propertyDescriptor.getType();
                    if (inType != null) {
                        if (!TypeUtils.equalTypes(type, inType)) {
                            trace.report(WRONG_SETTER_PARAMETER_TYPE.on(typeReference, inType, type));
                        }
                    }
                    else {
                        // TODO : the same check may be needed later???
                    }
                }

                ValueParameterDescriptorImpl valueParameterDescriptor =
                        resolveValueParameterDescriptor(scope, setterDescriptor, parameter, 0, type, trace);
                setterDescriptor.initialize(valueParameterDescriptor);
            }
            else {
                setterDescriptor.initializeDefault();
            }

            trace.record(BindingContext.PROPERTY_ACCESSOR, setter, setterDescriptor);
        }
        else if (property.isVar()) {
            Annotations setterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_SETTER);
            setterDescriptor = DescriptorFactory.createSetter(propertyDescriptor, setterAnnotations, !property.hasDelegate(),
                                                              /* isExternal = */ false, propertyDescriptor.getSource());
        }

        if (!property.isVar()) {
            if (setter != null) {
                //                trace.getErrorHandler().genericError(setter.asElement().getNode(), "A 'val'-property cannot have a setter");
                trace.report(VAL_WITH_SETTER.on(setter));
            }
        }
        return setterDescriptor;
    }

    @Nullable
    private PropertyGetterDescriptorImpl resolvePropertyGetterDescriptor(
            @NotNull LexicalScope scope,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull AnnotationSplitter annotationSplitter,
            BindingTrace trace
    ) {
        PropertyGetterDescriptorImpl getterDescriptor;
        KtPropertyAccessor getter = property.getGetter();
        if (getter != null) {
            Annotations getterAnnotations = new CompositeAnnotations(CollectionsKt.listOf(
                    annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER),
                    annotationResolver.resolveAnnotationsWithoutArguments(scope, getter.getModifierList(), trace)));

            KotlinType outType = propertyDescriptor.getType();
            KotlinType returnType = outType;
            KtTypeReference returnTypeReference = getter.getReturnTypeReference();
            if (returnTypeReference != null) {
                returnType = typeResolver.resolveType(scope, returnTypeReference, trace, true);
                if (outType != null && !TypeUtils.equalTypes(returnType, outType)) {
                    trace.report(WRONG_GETTER_RETURN_TYPE.on(returnTypeReference, propertyDescriptor.getReturnType(), outType));
                }
            }

            getterDescriptor = new PropertyGetterDescriptorImpl(propertyDescriptor, getterAnnotations,
                                                                resolveModalityFromModifiers(getter, propertyDescriptor.getModality()),
                                                                resolveVisibilityFromModifiers(getter, propertyDescriptor.getVisibility()),
                                                                getter.hasBody(), false, getter.hasModifier(EXTERNAL_KEYWORD),
                                                                CallableMemberDescriptor.Kind.DECLARATION, null, KotlinSourceElementKt
                                                                        .toSourceElement(getter));
            getterDescriptor.initialize(returnType);
            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor);
        }
        else {
            Annotations getterAnnotations = annotationSplitter.getAnnotationsForTarget(PROPERTY_GETTER);
            getterDescriptor = DescriptorFactory.createGetter(propertyDescriptor, getterAnnotations, !property.hasDelegate(),
                                                              /* isExternal = */ false);
            getterDescriptor.initialize(propertyDescriptor.getType());
        }
        return getterDescriptor;
    }

    @NotNull
    public PropertyDescriptor resolvePrimaryConstructorParameterToAProperty(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ValueParameterDescriptor valueParameter,
            @NotNull LexicalScope scope,
            @NotNull KtParameter parameter, final BindingTrace trace
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

        final AnnotationSplitter.PropertyWrapper propertyWrapper = new AnnotationSplitter.PropertyWrapper();
        Annotations allAnnotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, parameter.getModifierList(), trace);
        AnnotationSplitter annotationSplitter =
                new AnnotationSplitter(storageManager, allAnnotations, new Function0<Set<AnnotationUseSiteTarget>>() {
                    @Override
                    public Set<AnnotationUseSiteTarget> invoke() {
                        return AnnotationSplitter.getTargetSet(true, trace.getBindingContext(), propertyWrapper);
                    }
                });

        Annotations propertyAnnotations = new CompositeAnnotations(
                annotationSplitter.getAnnotationsForTargets(PROPERTY, FIELD),
                annotationSplitter.getOtherAnnotations());

        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                classDescriptor,
                propertyAnnotations,
                resolveModalityFromModifiers(parameter, Modality.FINAL),
                resolveVisibilityFromModifiers(parameter, getDefaultVisibility(parameter, classDescriptor)),
                isMutable,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                KotlinSourceElementKt.toSourceElement(parameter),
                /* lateInit = */ false,
                /* isConst = */ false
        );
        propertyWrapper.setProperty(propertyDescriptor);
        propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(),
                                   getDispatchReceiverParameterIfNeeded(classDescriptor), (ReceiverParameterDescriptor) null);

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
        if (type.isError()) return;

        KtTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement == null) return;

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        assert parameters.size() == arguments.size();

        List<KtTypeReference> jetTypeArguments = typeElement.getTypeArgumentsAsTypes();

        // A type reference from Kotlin code can yield a flexible type only if it's `ft<T1, T2>`, whose bounds should not be checked
        if (FlexibleTypesKt.isFlexible(type) && !DynamicTypesKt.isDynamic(type)) {
            assert jetTypeArguments.size() == 2
                    : "Flexible type cannot be denoted in Kotlin otherwise than as ft<T1, T2>, but was: "
                      + PsiUtilsKt.getElementTextWithContext(typeReference);
            // it's really ft<Foo, Bar>
            Flexibility flexibility = FlexibleTypesKt.flexibility(type);
            checkBounds(jetTypeArguments.get(0), flexibility.getLowerBound(), trace);
            checkBounds(jetTypeArguments.get(1), flexibility.getUpperBound(), trace);
            return;
        }

        assert jetTypeArguments.size() == arguments.size() : typeElement.getText() + ": " + jetTypeArguments + " - " + arguments;

        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        for (int i = 0; i < jetTypeArguments.size(); i++) {
            KtTypeReference jetTypeArgument = jetTypeArguments.get(i);

            if (jetTypeArgument == null) continue;

            KotlinType typeArgument = arguments.get(i).getType();
            checkBounds(jetTypeArgument, typeArgument, trace);

            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
            checkBounds(jetTypeArgument, typeArgument, typeParameterDescriptor, substitutor, trace);
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

    public static void registerFileInPackage(@NotNull BindingTrace trace, @NotNull KtFile file) {
        // Register files corresponding to this package
        // The trace currently does not support bi-di multimaps that would handle this task nicer
        FqName fqName = file.getPackageFqName();
        Collection<KtFile> files = trace.get(PACKAGE_TO_FILES, fqName);
        if (files == null) {
            files = Sets.newIdentityHashSet();
        }
        files.add(file);
        trace.record(BindingContext.PACKAGE_TO_FILES, fqName, files);
    }
}
