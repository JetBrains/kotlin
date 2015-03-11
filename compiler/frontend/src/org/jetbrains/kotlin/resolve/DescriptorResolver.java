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
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.lexer.JetKeywordToken;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.constants.evaluate.EvaluatePackage;
import org.jetbrains.kotlin.resolve.dataClassUtils.DataClassUtilsPackage;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.JetTokens.OVERRIDE_KEYWORD;
import static org.jetbrains.kotlin.lexer.JetTokens.VARARG_KEYWORD;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.ModifiersChecker.*;
import static org.jetbrains.kotlin.resolve.source.SourcePackage.toSourceElement;

public class DescriptorResolver {
    public static final Name COPY_METHOD_NAME = Name.identifier("copy");
    private static final Set<JetModifierKeywordToken> MODIFIERS_ILLEGAL_ON_PARAMETERS;
    static {
        MODIFIERS_ILLEGAL_ON_PARAMETERS = Sets.newHashSet();
        MODIFIERS_ILLEGAL_ON_PARAMETERS.addAll(Arrays.asList(JetTokens.MODIFIER_KEYWORDS_ARRAY));
        MODIFIERS_ILLEGAL_ON_PARAMETERS.remove(JetTokens.VARARG_KEYWORD);
    }

    private TypeResolver typeResolver;
    private AnnotationResolver annotationResolver;
    private ExpressionTypingServices expressionTypingServices;
    private DelegatedPropertyResolver delegatedPropertyResolver;
    private StorageManager storageManager;
    private KotlinBuiltIns builtIns;

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setDelegatedPropertyResolver(@NotNull DelegatedPropertyResolver delegatedPropertyResolver) {
        this.delegatedPropertyResolver = delegatedPropertyResolver;
    }

    @Inject
    public void setStorageManager(@NotNull StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Inject
    public void setBuiltIns(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    public List<JetType> resolveSupertypes(
            @NotNull JetScope scope,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetClassOrObject jetClass,
            BindingTrace trace
    ) {
        List<JetType> supertypes = Lists.newArrayList();
        List<JetDelegationSpecifier> delegationSpecifiers = jetClass.getDelegationSpecifiers();
        Collection<JetType> declaredSupertypes = resolveDelegationSpecifiers(
                scope,
                delegationSpecifiers,
                typeResolver, trace, false);

        for (JetType declaredSupertype : declaredSupertypes) {
            addValidSupertype(supertypes, declaredSupertype);
        }

        if (classDescriptor.getKind() == ClassKind.ENUM_CLASS && !containsClass(supertypes)) {
            supertypes.add(0, builtIns.getEnumType(classDescriptor.getDefaultType()));
        }

        if (supertypes.isEmpty()) {
            JetType defaultSupertype = getDefaultSupertype(jetClass, trace);
            addValidSupertype(supertypes, defaultSupertype);
        }

        return supertypes;
    }

    private static void addValidSupertype(List<JetType> supertypes, JetType declaredSupertype) {
        if (!declaredSupertype.isError()) {
            supertypes.add(declaredSupertype);
        }
    }

    private boolean containsClass(Collection<JetType> result) {
        for (JetType type : result) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor && ((ClassDescriptor) descriptor).getKind() != ClassKind.TRAIT) {
                return true;
            }
        }
        return false;
    }

    private JetType getDefaultSupertype(JetClassOrObject jetClass, BindingTrace trace) {
        // TODO : beautify
        if (jetClass instanceof JetEnumEntry) {
            JetClassOrObject parent = JetStubbedPsiUtil.getContainingDeclaration(jetClass, JetClassOrObject.class);
            ClassDescriptor parentDescriptor = trace.getBindingContext().get(BindingContext.CLASS, parent);
            if (parentDescriptor.getTypeConstructor().getParameters().isEmpty()) {
                return parentDescriptor.getDefaultType();
            }
            else {
                trace.report(NO_GENERICS_IN_SUPERTYPE_SPECIFIER.on(jetClass.getNameIdentifier()));
                return ErrorUtils.createErrorType("Supertype not specified");
            }
        }
        else if (jetClass instanceof JetClass && ((JetClass) jetClass).isAnnotation()) {
            return builtIns.getAnnotationType();
        }
        return builtIns.getAnyType();
    }

    public Collection<JetType> resolveDelegationSpecifiers(
            JetScope extensibleScope,
            List<JetDelegationSpecifier> delegationSpecifiers,
            @NotNull TypeResolver resolver,
            BindingTrace trace,
            boolean checkBounds
    ) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<JetType> result = Lists.newArrayList();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            JetTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                JetType supertype = resolver.resolveType(extensibleScope, typeReference, trace, checkBounds);
                if (TypesPackage.isDynamic(supertype)) {
                    trace.report(DYNAMIC_SUPERTYPE.on(typeReference));
                }
                else {
                    result.add(supertype);
                    JetTypeElement bareSuperType = checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.getTypeElement());
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
    private static JetTypeElement checkNullableSupertypeAndStripQuestionMarks(@NotNull BindingTrace trace, @Nullable JetTypeElement typeElement) {
        while (typeElement instanceof JetNullableType) {
            JetNullableType nullableType = (JetNullableType) typeElement;
            typeElement = nullableType.getInnerType();
            // report only for innermost '?', the rest gets a 'redundant' warning
            if (!(typeElement instanceof JetNullableType) && typeElement != null) {
                trace.report(NULLABLE_SUPERTYPE.on(nullableType));
            }
        }
        return typeElement;
    }

    private static void checkProjectionsInImmediateArguments(@NotNull BindingTrace trace, @Nullable JetTypeElement typeElement) {
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            List<JetTypeProjection> typeArguments = userType.getTypeArguments();
            for (JetTypeProjection typeArgument : typeArguments) {
                if (typeArgument.getProjectionKind() != JetProjectionKind.NONE) {
                    trace.report(PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE.on(typeArgument));
                }
            }
        }
    }

    @NotNull
    public SimpleFunctionDescriptor resolveFunctionDescriptorWithAnnotationArguments(
            @NotNull DeclarationDescriptor containingDescriptor,
            @NotNull JetScope scope,
            @NotNull JetNamedFunction function,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        return resolveFunctionDescriptor(containingDescriptor, scope, function, trace, dataFlowInfo,
                                         annotationResolver.resolveAnnotationsWithArguments(scope, function.getModifierList(), trace));
    }

    @NotNull
    public SimpleFunctionDescriptor resolveFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDescriptor,
            @NotNull JetScope scope,
            @NotNull JetNamedFunction function,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
       return resolveFunctionDescriptor(containingDescriptor, scope, function, trace, dataFlowInfo,
                                        annotationResolver.resolveAnnotationsWithoutArguments(scope, function.getModifierList(), trace));
    }

    @NotNull
    private SimpleFunctionDescriptor resolveFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDescriptor,
            @NotNull final JetScope scope,
            @NotNull final JetNamedFunction function,
            @NotNull final BindingTrace trace,
            @NotNull final DataFlowInfo dataFlowInfo,
            @NotNull Annotations annotations
    ) {
        final SimpleFunctionDescriptorImpl functionDescriptor = SimpleFunctionDescriptorImpl.create(
                containingDescriptor,
                annotations,
                JetPsiUtil.safeName(function.getName()),
                CallableMemberDescriptor.Kind.DECLARATION,
                toSourceElement(function)
        );
        WritableScope innerScope = new WritableScopeImpl(scope, functionDescriptor, new TraceBasedRedeclarationHandler(trace),
                                                         "Function descriptor header scope");
        innerScope.addLabeledDeclaration(functionDescriptor);

        List<TypeParameterDescriptorImpl> typeParameterDescriptors =
                resolveTypeParametersForCallableDescriptor(functionDescriptor, innerScope, function.getTypeParameters(), trace);
        innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        resolveGenericBounds(function, functionDescriptor, innerScope, typeParameterDescriptors, trace);

        JetType receiverType = null;
        JetTypeReference receiverTypeRef = function.getReceiverTypeReference();
        if (receiverTypeRef != null) {
            JetScope scopeForReceiver =
                    function.hasTypeParameterListBeforeFunctionName()
                    ? innerScope
                    : scope;
            receiverType = typeResolver.resolveType(scopeForReceiver, receiverTypeRef, trace, true);
        }

        List<ValueParameterDescriptor> valueParameterDescriptors =
                resolveValueParameters(functionDescriptor, innerScope, function.getValueParameters(), trace);

        innerScope.changeLockLevel(WritableScope.LockLevel.READING);

        JetTypeReference returnTypeRef = function.getTypeReference();
        JetType returnType;
        if (returnTypeRef != null) {
            returnType = typeResolver.resolveType(innerScope, returnTypeRef, trace, true);
        }
        else if (function.hasBlockBody()) {
            returnType = builtIns.getUnitType();
        }
        else {
            if (function.hasBody()) {
                returnType =
                        DeferredType.createRecursionIntolerant(
                                storageManager,
                                trace,
                                new Function0<JetType>() {
                                    @Override
                                    public JetType invoke() {
                                        JetType type = expressionTypingServices
                                                .getBodyExpressionType(trace, scope, dataFlowInfo, function, functionDescriptor);
                                        return transformAnonymousTypeIfNeeded(functionDescriptor, function, type, trace);
                                    }
                                });
            }
            else {
                returnType = ErrorUtils.createErrorType("No type, no body");
            }
        }
        Modality modality = resolveModalityFromModifiers(function, getDefaultModality(containingDescriptor, function.hasBody()));
        Visibility visibility = resolveVisibilityFromModifiers(function, getDefaultVisibility(function, containingDescriptor));
        functionDescriptor.initialize(
                receiverType,
                getDispatchReceiverParameterIfNeeded(containingDescriptor),
                typeParameterDescriptors,
                valueParameterDescriptors,
                returnType,
                modality,
                visibility
        );

        BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, function, functionDescriptor);
        return functionDescriptor;
    }

    @NotNull
    public static SimpleFunctionDescriptor createComponentFunctionDescriptor(
            int parameterIndex,
            @NotNull PropertyDescriptor property,
            @NotNull ValueParameterDescriptor parameter,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        Name functionName = DataClassUtilsPackage.createComponentName(parameterIndex);
        JetType returnType = property.getType();

        SimpleFunctionDescriptorImpl functionDescriptor = SimpleFunctionDescriptorImpl.create(
                classDescriptor,
                Annotations.EMPTY,
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

        trace.record(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter, functionDescriptor);

        return functionDescriptor;
    }

    @NotNull
    public static SimpleFunctionDescriptor createCopyFunctionDescriptor(
            @NotNull Collection<ValueParameterDescriptor> constructorParameters,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        JetType returnType = classDescriptor.getDefaultType();

        SimpleFunctionDescriptorImpl functionDescriptor = SimpleFunctionDescriptorImpl.create(
                classDescriptor,
                Annotations.EMPTY,
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
                                                     parameter.getVarargElementType(), SourceElement.NO_SOURCE);
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

    public static Visibility getDefaultVisibility(JetModifierListOwner modifierListOwner, DeclarationDescriptor containingDescriptor) {
        Visibility defaultVisibility;
        if (containingDescriptor instanceof ClassDescriptor) {
            JetModifierList modifierList = modifierListOwner.getModifierList();
            defaultVisibility = modifierList != null && modifierList.hasModifier(OVERRIDE_KEYWORD)
                                           ? Visibilities.INHERITED
                                           : Visibilities.INTERNAL;
        }
        else if (containingDescriptor instanceof FunctionDescriptor) {
            defaultVisibility = Visibilities.LOCAL;
        }
        else {
            defaultVisibility = Visibilities.INTERNAL;
        }
        return defaultVisibility;
    }

    public static Modality getDefaultModality(DeclarationDescriptor containingDescriptor, boolean isBodyPresent) {
        Modality defaultModality;
        if (containingDescriptor instanceof ClassDescriptor) {
            boolean isTrait = ((ClassDescriptor) containingDescriptor).getKind() == ClassKind.TRAIT;
            boolean isDefinitelyAbstract = isTrait && !isBodyPresent;
            Modality basicModality = isTrait ? Modality.OPEN : Modality.FINAL;
            defaultModality = isDefinitelyAbstract ? Modality.ABSTRACT : basicModality;
        }
        else {
            defaultModality = Modality.FINAL;
        }
        return defaultModality;
    }

    @NotNull
    private List<ValueParameterDescriptor> resolveValueParameters(
            FunctionDescriptor functionDescriptor,
            WritableScope parameterScope,
            List<JetParameter> valueParameters,
            BindingTrace trace
    ) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0; i < valueParameters.size(); i++) {
            JetParameter valueParameter = valueParameters.get(i);
            JetTypeReference typeReference = valueParameter.getTypeReference();

            JetType type;
            if (typeReference == null) {
                trace.report(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.on(valueParameter));
                type = ErrorUtils.createErrorType("Type annotation was missing");
            }
            else {
                type = typeResolver.resolveType(parameterScope, typeReference, trace, true);
            }

            if (!(functionDescriptor instanceof ConstructorDescriptor)) {
                checkParameterHasNoValOrVar(trace, valueParameter, VAL_OR_VAR_ON_FUN_PARAMETER);
                checkParameterHasNoModifier(trace, valueParameter);
            } else {
                checkConstructorParameterHasNoModifier(trace, valueParameter);
            }

            ValueParameterDescriptor valueParameterDescriptor =
                    resolveValueParameterDescriptor(parameterScope, functionDescriptor, valueParameter, i, type, trace);
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
            result.add(valueParameterDescriptor);
        }
        return result;
    }

    @NotNull
    public ValueParameterDescriptorImpl resolveValueParameterDescriptor(
            JetScope scope, DeclarationDescriptor declarationDescriptor,
            JetParameter valueParameter, int index, JetType type, BindingTrace trace
    ) {
        return resolveValueParameterDescriptor(declarationDescriptor, valueParameter, index, type, trace,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, valueParameter.getModifierList(), trace));
    }

    @NotNull
    public ValueParameterDescriptorImpl resolveValueParameterDescriptorWithAnnotationArguments(
            JetScope scope, DeclarationDescriptor declarationDescriptor,
            JetParameter valueParameter, int index, JetType type, BindingTrace trace
    ) {
        return resolveValueParameterDescriptor(declarationDescriptor, valueParameter, index, type, trace,
                annotationResolver.resolveAnnotationsWithArguments(scope, valueParameter.getModifierList(), trace));
    }

    @NotNull
    private ValueParameterDescriptorImpl resolveValueParameterDescriptor(
            DeclarationDescriptor declarationDescriptor,
            JetParameter valueParameter, int index, JetType type, BindingTrace trace,
            Annotations annotations
    ) {
        JetType varargElementType = null;
        JetType variableType = type;
        if (valueParameter.hasModifier(VARARG_KEYWORD)) {
            varargElementType = type;
            variableType = getVarargParameterType(type);
        }
        ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                declarationDescriptor,
                null,
                index,
                annotations,
                JetPsiUtil.safeName(valueParameter.getName()),
                variableType,
                valueParameter.hasDefaultValue(),
                varargElementType,
                toSourceElement(valueParameter)
        );

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    @NotNull
    private JetType getVarargParameterType(@NotNull JetType elementType) {
        JetType primitiveArrayType = builtIns.getPrimitiveArrayJetTypeByPrimitiveJetType(elementType);
        if (primitiveArrayType != null) {
            return primitiveArrayType;
        }
        return builtIns.getArrayType(Variance.OUT_VARIANCE, elementType);
    }

    public List<TypeParameterDescriptorImpl> resolveTypeParametersForCallableDescriptor(
            DeclarationDescriptor containingDescriptor,
            WritableScope extensibleScope,
            List<JetTypeParameter> typeParameters,
            BindingTrace trace
    ) {
        List<TypeParameterDescriptorImpl> result = new ArrayList<TypeParameterDescriptorImpl>();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            JetTypeParameter typeParameter = typeParameters.get(i);
            result.add(resolveTypeParameterForCallableDescriptor(containingDescriptor, extensibleScope, typeParameter, i, trace));
        }
        return result;
    }

    private TypeParameterDescriptorImpl resolveTypeParameterForCallableDescriptor(
            DeclarationDescriptor containingDescriptor,
            WritableScope extensibleScope,
            JetTypeParameter typeParameter,
            int index,
            BindingTrace trace
    ) {
        if (typeParameter.getVariance() != Variance.INVARIANT) {
            assert !(containingDescriptor instanceof ClassifierDescriptor) : "This method is intended for functions/properties";
            trace.report(VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY.on(typeParameter));
        }

        // TODO: Support annotation for type parameters
        AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(typeParameter, trace);

        TypeParameterDescriptorImpl typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDescriptor,
                Annotations.EMPTY,
                typeParameter.hasModifier(JetTokens.REIFIED_KEYWORD),
                typeParameter.getVariance(),
                JetPsiUtil.safeName(typeParameter.getName()),
                index,
                toSourceElement(typeParameter)
        );
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
        extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    @NotNull
    public static ConstructorDescriptorImpl createAndRecordPrimaryConstructorForObject(
            @Nullable JetClassOrObject object,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        ConstructorDescriptorImpl constructorDescriptor =
                DescriptorFactory.createPrimaryConstructorForObject(classDescriptor, toSourceElement(object));
        if (object != null) {
            trace.record(CONSTRUCTOR, object, constructorDescriptor);
        }
        return constructorDescriptor;
    }

    static final class UpperBoundCheckerTask {
        JetTypeReference upperBound;
        JetType upperBoundType;
        boolean isDefaultObjectConstraint;

        private UpperBoundCheckerTask(JetTypeReference upperBound, JetType upperBoundType, boolean defaultObjectConstraint) {
            this.upperBound = upperBound;
            this.upperBoundType = upperBoundType;
            isDefaultObjectConstraint = defaultObjectConstraint;
        }
    }

    public void resolveGenericBounds(
            @NotNull JetTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            JetScope scope,
            List<TypeParameterDescriptorImpl> parameters,
            BindingTrace trace
    ) {
        List<UpperBoundCheckerTask> deferredUpperBoundCheckerTasks = Lists.newArrayList();

        List<JetTypeParameter> typeParameters = declaration.getTypeParameters();
        Map<Name, TypeParameterDescriptorImpl> parameterByName = Maps.newHashMap();
        for (int i = 0; i < typeParameters.size(); i++) {
            JetTypeParameter jetTypeParameter = typeParameters.get(i);
            TypeParameterDescriptorImpl typeParameterDescriptor = parameters.get(i);

            parameterByName.put(typeParameterDescriptor.getName(), typeParameterDescriptor);

            JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
            if (extendsBound != null) {
                JetType type = typeResolver.resolveType(scope, extendsBound, trace, false);
                typeParameterDescriptor.addUpperBound(type);
                deferredUpperBoundCheckerTasks.add(new UpperBoundCheckerTask(extendsBound, type, false));
            }
        }
        for (JetTypeConstraint constraint : declaration.getTypeConstraints()) {
            reportUnsupportedDefaultObjectConstraint(trace, constraint);

            JetSimpleNameExpression subjectTypeParameterName = constraint.getSubjectTypeParameterName();
            if (subjectTypeParameterName == null) {
                continue;
            }
            Name referencedName = subjectTypeParameterName.getReferencedNameAsName();
            TypeParameterDescriptorImpl typeParameterDescriptor = parameterByName.get(referencedName);
            JetTypeReference boundTypeReference = constraint.getBoundTypeReference();
            JetType bound = null;
            if (boundTypeReference != null) {
                bound = typeResolver.resolveType(scope, boundTypeReference, trace, false);
                deferredUpperBoundCheckerTasks
                        .add(new UpperBoundCheckerTask(boundTypeReference, bound, constraint.isDefaultObjectConstraint()));
            }

            if (typeParameterDescriptor != null) {
                trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor);
                if (bound != null) {
                    if (constraint.isDefaultObjectConstraint()) {
                        // Default object bounds are not supported
                    }
                    else {
                        typeParameterDescriptor.addUpperBound(bound);
                    }
                }
            }
        }

        for (TypeParameterDescriptorImpl parameter : parameters) {
            parameter.addDefaultUpperBound();

            parameter.setInitialized();

            checkConflictingUpperBounds(trace, parameter, typeParameters.get(parameter.getIndex()));
        }

        if (!(declaration instanceof JetClass)) {
            for (UpperBoundCheckerTask checkerTask : deferredUpperBoundCheckerTasks) {
                checkUpperBoundType(checkerTask.upperBound, checkerTask.upperBoundType, checkerTask.isDefaultObjectConstraint, trace);
            }

            checkNamesInConstraints(declaration, descriptor, scope, trace);
        }
    }

    public static void checkConflictingUpperBounds(
            @NotNull BindingTrace trace,
            @NotNull TypeParameterDescriptor parameter,
            @NotNull JetTypeParameter typeParameter
    ) {
        if (KotlinBuiltIns.isNothing(parameter.getUpperBoundsAsType())) {
            trace.report(CONFLICTING_UPPER_BOUNDS.on(typeParameter, parameter));
        }

        JetType classObjectType = parameter.getClassObjectType();
        if (classObjectType != null && KotlinBuiltIns.isNothing(classObjectType)) {
            trace.report(CONFLICTING_DEFAULT_OBJECT_UPPER_BOUNDS.on(typeParameter, parameter));
        }
    }

    public void checkNamesInConstraints(
            @NotNull JetTypeParameterListOwner declaration,
            @NotNull DeclarationDescriptor descriptor,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace
    ) {
        for (JetTypeConstraint constraint : declaration.getTypeConstraints()) {
            JetSimpleNameExpression nameExpression = constraint.getSubjectTypeParameterName();
            if (nameExpression == null) continue;

            Name name = nameExpression.getReferencedNameAsName();

            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier instanceof TypeParameterDescriptor && classifier.getContainingDeclaration() == descriptor) continue;

            if (classifier != null) {
                // To tell the user that we look only for locally defined type parameters
                trace.report(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(nameExpression, constraint, declaration));
                trace.record(BindingContext.REFERENCE_TARGET, nameExpression, classifier);
            }
            else {
                trace.report(UNRESOLVED_REFERENCE.on(nameExpression, nameExpression));
            }

            JetTypeReference boundTypeReference = constraint.getBoundTypeReference();
            if (boundTypeReference != null) {
                typeResolver.resolveType(scope, boundTypeReference, trace, true);
            }
        }
    }

    public static void reportUnsupportedDefaultObjectConstraint(BindingTrace trace, JetTypeConstraint constraint) {
        if (constraint.isDefaultObjectConstraint()) {
            trace.report(UNSUPPORTED.on(constraint, "Default objects constraints are not supported yet"));
        }
    }

    public static void checkUpperBoundType(
            JetTypeReference upperBound,
            @NotNull JetType upperBoundType,
            boolean isDefaultObjectConstraint,
            BindingTrace trace
    ) {
        if (!TypeUtils.canHaveSubtypes(JetTypeChecker.DEFAULT, upperBoundType)) {
            if (isDefaultObjectConstraint) {
                trace.report(FINAL_DEFAULT_OBJECT_UPPER_BOUND.on(upperBound, upperBoundType));
            }
            else {
                trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
            }
        }
        if (TypesPackage.isDynamic(upperBoundType)) {
            trace.report(DYNAMIC_UPPER_BOUND.on(upperBound));
        }
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(
            @NotNull JetScope scope,
            @NotNull JetParameter parameter,
            BindingTrace trace
    ) {
        JetType type = resolveParameterType(scope, parameter, trace);
        return resolveLocalVariableDescriptor(parameter, type, trace, scope);
    }

    private JetType resolveParameterType(JetScope scope, JetParameter parameter, BindingTrace trace) {
        JetTypeReference typeReference = parameter.getTypeReference();
        JetType type;
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
            @NotNull JetParameter parameter,
            @NotNull JetType type,
            BindingTrace trace,
            @NotNull JetScope scope
    ) {
        VariableDescriptor variableDescriptor = new LocalVariableDescriptor(
                scope.getContainingDeclaration(),
                annotationResolver.resolveAnnotationsWithArguments(scope, parameter.getModifierList(), trace),
                JetPsiUtil.safeName(parameter.getName()),
                type,
                false,
                toSourceElement(parameter)
        );
        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(
            JetScope scope,
            JetVariableDeclaration variable,
            DataFlowInfo dataFlowInfo,
            BindingTrace trace
    ) {
        DeclarationDescriptor containingDeclaration = scope.getContainingDeclaration();
        // SCRIPT: Create property descriptors
        if (JetPsiUtil.isScriptDeclaration(variable)) {
            PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                    containingDeclaration,
                    annotationResolver.resolveAnnotationsWithArguments(scope, variable.getModifierList(), trace),
                    Modality.FINAL,
                    Visibilities.INTERNAL,
                    variable.isVar(),
                    JetPsiUtil.safeName(variable.getName()),
                    CallableMemberDescriptor.Kind.DECLARATION,
                    toSourceElement(variable)
            );

            JetType type =
                    getVariableType(propertyDescriptor, scope, variable, dataFlowInfo, false, trace); // For a local variable the type must not be deferred

            ReceiverParameterDescriptor receiverParameter = ((ScriptDescriptor) containingDeclaration).getThisAsReceiverParameter();
            propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(), receiverParameter, (JetType) null);
            initializeWithDefaultGetterSetter(propertyDescriptor);
            trace.record(BindingContext.VARIABLE, variable, propertyDescriptor);
            return propertyDescriptor;
        }
        else {
            VariableDescriptorImpl variableDescriptor =
                    resolveLocalVariableDescriptorWithType(scope, variable, null, trace);

            JetType type =
                    getVariableType(variableDescriptor, scope, variable, dataFlowInfo, false, trace); // For a local variable the type must not be deferred
            variableDescriptor.setOutType(type);
            return variableDescriptor;
        }
    }

    private static void initializeWithDefaultGetterSetter(PropertyDescriptorImpl propertyDescriptor) {
        PropertyGetterDescriptorImpl getter = propertyDescriptor.getGetter();
        if (getter == null && !Visibilities.isPrivate(propertyDescriptor.getVisibility())) {
            getter = DescriptorFactory.createDefaultGetter(propertyDescriptor);
            getter.initialize(propertyDescriptor.getType());
        }

        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        if (setter == null && propertyDescriptor.isVar()) {
            setter = DescriptorFactory.createDefaultSetter(propertyDescriptor);
        }
        propertyDescriptor.initialize(getter, setter);
    }

    @NotNull
    public VariableDescriptorImpl resolveLocalVariableDescriptorWithType(
            @NotNull JetScope scope,
            @NotNull JetVariableDeclaration variable,
            @Nullable JetType type,
            @NotNull BindingTrace trace
    ) {
        VariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(
                scope.getContainingDeclaration(),
                annotationResolver.resolveAnnotationsWithArguments(scope, variable.getModifierList(), trace),
                JetPsiUtil.safeName(variable.getName()),
                type,
                variable.isVar(),
                toSourceElement(variable)
        );
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetScope scope,
            @NotNull JetProperty property,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        JetModifierList modifierList = property.getModifierList();
        boolean isVar = property.isVar();

        boolean hasBody = hasBody(property);
        Modality modality = containingDeclaration instanceof ClassDescriptor
                            ? resolveModalityFromModifiers(property, getDefaultModality(containingDeclaration, hasBody))
                            : Modality.FINAL;
        Visibility visibility = resolveVisibilityFromModifiers(property, getDefaultVisibility(property, containingDeclaration));
        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                containingDeclaration,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace),
                modality,
                visibility,
                isVar,
                JetPsiUtil.safeName(property.getName()),
                CallableMemberDescriptor.Kind.DECLARATION,
                toSourceElement(property)
        );

        List<TypeParameterDescriptorImpl> typeParameterDescriptors;
        JetScope scopeWithTypeParameters;
        JetType receiverType = null;

        {
            List<JetTypeParameter> typeParameters = property.getTypeParameters();
            if (typeParameters.isEmpty()) {
                scopeWithTypeParameters = scope;
                typeParameterDescriptors = Collections.emptyList();
            }
            else {
                WritableScope writableScope = new WritableScopeImpl(
                        scope, containingDeclaration, new TraceBasedRedeclarationHandler(trace),
                        "Scope with type parameters of a property");
                typeParameterDescriptors = resolveTypeParametersForCallableDescriptor(propertyDescriptor, writableScope, typeParameters,
                                                                                      trace);
                writableScope.changeLockLevel(WritableScope.LockLevel.READING);
                resolveGenericBounds(property, propertyDescriptor, writableScope, typeParameterDescriptors, trace);
                scopeWithTypeParameters = writableScope;
            }

            JetTypeReference receiverTypeRef = property.getReceiverTypeReference();
            if (receiverTypeRef != null) {
                receiverType = typeResolver.resolveType(scopeWithTypeParameters, receiverTypeRef, trace, true);
            }
        }

        ReceiverParameterDescriptor receiverDescriptor =
                DescriptorFactory.createExtensionReceiverParameterForCallable(propertyDescriptor, receiverType);

        ReceiverParameterDescriptor implicitInitializerReceiver = property.hasDelegate() ? NO_RECEIVER_PARAMETER : receiverDescriptor;

        JetScope propertyScope = JetScopeUtils.getPropertyDeclarationInnerScope(propertyDescriptor, scope, typeParameterDescriptors,
                                                                                implicitInitializerReceiver, trace);

        JetType type = getVariableType(propertyDescriptor, propertyScope, property, dataFlowInfo, true, trace);

        propertyDescriptor.setType(type, typeParameterDescriptors, getDispatchReceiverParameterIfNeeded(containingDeclaration),
                                   receiverDescriptor);

        PropertyGetterDescriptorImpl getter = resolvePropertyGetterDescriptor(scopeWithTypeParameters, property, propertyDescriptor, trace);
        PropertySetterDescriptor setter = resolvePropertySetterDescriptor(scopeWithTypeParameters, property, propertyDescriptor, trace);

        propertyDescriptor.initialize(getter, setter);

        trace.record(BindingContext.VARIABLE, property, propertyDescriptor);
        return propertyDescriptor;
    }

    /*package*/
    static boolean hasBody(JetProperty property) {
        boolean hasBody = property.hasDelegateExpressionOrInitializer();
        if (!hasBody) {
            JetPropertyAccessor getter = property.getGetter();
            if (getter != null && getter.hasBody()) {
                hasBody = true;
            }
            JetPropertyAccessor setter = property.getSetter();
            if (!hasBody && setter != null && setter.hasBody()) {
                hasBody = true;
            }
        }
        return hasBody;
    }

    @NotNull
    private JetType getVariableType(
            @NotNull final VariableDescriptorImpl variableDescriptor,
            @NotNull final JetScope scope,
            @NotNull final JetVariableDeclaration variable,
            @NotNull final DataFlowInfo dataFlowInfo,
            boolean notLocal,
            @NotNull final BindingTrace trace
    ) {
        JetTypeReference propertyTypeRef = variable.getTypeReference();

        boolean hasDelegate = variable instanceof JetProperty && ((JetProperty) variable).hasDelegateExpression();
        if (propertyTypeRef == null) {
            if (!variable.hasInitializer()) {
                if (hasDelegate && variableDescriptor instanceof PropertyDescriptor) {
                    final JetProperty property = (JetProperty) variable;
                    if (property.hasDelegateExpression()) {
                        return DeferredType.createRecursionIntolerant(
                                storageManager,
                                trace,
                                new Function0<JetType>() {
                                    @Override
                                    public JetType invoke() {
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
                            new Function0<JetType>() {
                                @Override
                                public JetType invoke() {
                                    JetType initializerType = resolveInitializerType(scope, variable.getInitializer(), dataFlowInfo, trace);
                                    setConstantForVariableIfNeeded(variableDescriptor, scope, variable, dataFlowInfo, initializerType, trace);
                                    return transformAnonymousTypeIfNeeded(variableDescriptor, variable, initializerType, trace);
                                }
                            }
                    );
                }
                else {
                    JetType initializerType = resolveInitializerType(scope, variable.getInitializer(), dataFlowInfo, trace);
                    setConstantForVariableIfNeeded(variableDescriptor, scope, variable, dataFlowInfo, initializerType, trace);
                    return initializerType;
                }
            }
        }
        else {
            JetType type = typeResolver.resolveType(scope, propertyTypeRef, trace, true);
            setConstantForVariableIfNeeded(variableDescriptor, scope, variable, dataFlowInfo, type, trace);
            return type;
        }
    }

    private void setConstantForVariableIfNeeded(
            @NotNull VariableDescriptorImpl variableDescriptor,
            @NotNull final JetScope scope,
            @NotNull final JetVariableDeclaration variable,
            @NotNull final DataFlowInfo dataFlowInfo,
            @NotNull final JetType variableType,
            @NotNull final BindingTrace trace
    ) {
        if (!shouldRecordInitializerForProperty(variableDescriptor, variableType)) return;

        if (!variable.hasInitializer()) return;

        variableDescriptor.setCompileTimeInitializer(
            storageManager.createRecursionTolerantNullableLazyValue(new Function0<CompileTimeConstant<?>>() {
                @Nullable
                @Override
                public CompileTimeConstant<?> invoke() {
                    JetExpression initializer = variable.getInitializer();
                    JetType initializerType = expressionTypingServices.safeGetType(scope, initializer, variableType, dataFlowInfo, trace);
                    CompileTimeConstant<?> constant = ConstantExpressionEvaluator.evaluate(initializer, trace, initializerType);
                    if (constant instanceof IntegerValueTypeConstant) {
                        return EvaluatePackage.createCompileTimeConstantWithType((IntegerValueTypeConstant) constant, initializerType);
                    }
                    return constant;
                }
            }, null)
        );
    }

    @NotNull
    private JetType resolveDelegatedPropertyType(
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope scope,
            @NotNull JetExpression delegateExpression,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        JetScope accessorScope = JetScopeUtils.makeScopeForPropertyAccessor(propertyDescriptor, scope, trace);

        JetType type = delegatedPropertyResolver.resolveDelegateExpression(
                delegateExpression, property, propertyDescriptor, scope, accessorScope, trace, dataFlowInfo);

        if (type != null) {
            JetType getterReturnType = delegatedPropertyResolver
                    .getDelegatedPropertyGetMethodReturnType(propertyDescriptor, delegateExpression, type, trace, accessorScope);
            if (getterReturnType != null) {
                return getterReturnType;
            }
        }
        return ErrorUtils.createErrorType("Type from delegate");
    }

    @Nullable
    private static JetType transformAnonymousTypeIfNeeded(
            @NotNull DeclarationDescriptorWithVisibility descriptor,
            @NotNull JetNamedDeclaration declaration,
            @NotNull JetType type,
            @NotNull BindingTrace trace
    ) {
        ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (classifierDescriptor == null || !DescriptorUtils.isAnonymousObject(classifierDescriptor)) {
            return type;
        }

        boolean definedInClass = DescriptorUtils.getParentOfType(descriptor, ClassDescriptor.class) != null;
        boolean isLocal = DescriptorUtils.isLocal(descriptor);
        Visibility visibility = descriptor.getVisibility();
        boolean transformNeeded = !isLocal && !visibility.isPublicAPI()
                                  && !(definedInClass && Visibilities.isPrivate(visibility));
        if (transformNeeded) {
            if (type.getConstructor().getSupertypes().size() == 1) {
                assert type.getArguments().isEmpty() : "Object expression couldn't have any type parameters!";
                return type.getConstructor().getSupertypes().iterator().next();
            }
            else {
                trace.report(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED.on(declaration, type.getConstructor().getSupertypes()));
            }
        }
        return type;
    }

    @NotNull
    private JetType resolveInitializerType(
            @NotNull JetScope scope,
            @NotNull JetExpression initializer,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        return expressionTypingServices.safeGetType(scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, trace);
    }

    @Nullable
    private PropertySetterDescriptor resolvePropertySetterDescriptor(
            @NotNull JetScope scope,
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            BindingTrace trace
    ) {
        JetPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptorImpl setterDescriptor = null;
        if (setter != null) {
            Annotations annotations =
                    annotationResolver.resolveAnnotationsWithoutArguments(scope, setter.getModifierList(), trace);
            JetParameter parameter = setter.getParameter();

            setterDescriptor = new PropertySetterDescriptorImpl(propertyDescriptor, annotations,
                                                                resolveModalityFromModifiers(setter, propertyDescriptor.getModality()),
                                                                resolveVisibilityFromModifiers(setter, propertyDescriptor.getVisibility()),
                                                                setter.hasBody(), false,
                                                                CallableMemberDescriptor.Kind.DECLARATION, null, toSourceElement(setter));
            if (parameter != null) {

                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case
                if (parameter.hasDefaultValue()) {
                    trace.report(SETTER_PARAMETER_WITH_DEFAULT_VALUE.on(parameter.getDefaultValue()));
                }

                JetType type;
                JetTypeReference typeReference = parameter.getTypeReference();
                if (typeReference == null) {
                    type = propertyDescriptor.getType(); // TODO : this maybe unknown at this point
                }
                else {
                    type = typeResolver.resolveType(scope, typeReference, trace, true);
                    JetType inType = propertyDescriptor.getType();
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
            setterDescriptor = DescriptorFactory.createSetter(propertyDescriptor, !property.hasDelegate());
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
            @NotNull JetScope scope,
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            BindingTrace trace
    ) {
        PropertyGetterDescriptorImpl getterDescriptor;
        JetPropertyAccessor getter = property.getGetter();
        if (getter != null) {
            Annotations annotations =
                    annotationResolver.resolveAnnotationsWithoutArguments(scope, getter.getModifierList(), trace);

            JetType outType = propertyDescriptor.getType();
            JetType returnType = outType;
            JetTypeReference returnTypeReference = getter.getReturnTypeReference();
            if (returnTypeReference != null) {
                returnType = typeResolver.resolveType(scope, returnTypeReference, trace, true);
                if (outType != null && !TypeUtils.equalTypes(returnType, outType)) {
                    trace.report(WRONG_GETTER_RETURN_TYPE.on(returnTypeReference, propertyDescriptor.getReturnType(), outType));
                }
            }

            getterDescriptor = new PropertyGetterDescriptorImpl(propertyDescriptor, annotations,
                                                                resolveModalityFromModifiers(getter, propertyDescriptor.getModality()),
                                                                resolveVisibilityFromModifiers(getter, propertyDescriptor.getVisibility()),
                                                                getter.hasBody(), false,
                                                                CallableMemberDescriptor.Kind.DECLARATION, null, toSourceElement(getter));
            getterDescriptor.initialize(returnType);
            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor);
        }
        else {
            getterDescriptor = DescriptorFactory.createGetter(propertyDescriptor, !property.hasDelegate());
            getterDescriptor.initialize(propertyDescriptor.getType());
        }
        return getterDescriptor;
    }

    @NotNull
    private ConstructorDescriptorImpl createConstructorDescriptor(
            @NotNull JetScope scope,
            @NotNull ClassDescriptor classDescriptor,
            boolean isPrimary,
            @Nullable JetModifierList modifierList,
            @NotNull JetDeclaration declarationToTrace,
            List<TypeParameterDescriptor> typeParameters, @NotNull List<JetParameter> valueParameters, BindingTrace trace
    ) {
        ConstructorDescriptorImpl constructorDescriptor = ConstructorDescriptorImpl.create(
                classDescriptor,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace),
                isPrimary,
                toSourceElement(declarationToTrace)
        );
        trace.record(BindingContext.CONSTRUCTOR, declarationToTrace, constructorDescriptor);
        WritableScopeImpl parameterScope = new WritableScopeImpl(
                scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "Scope with value parameters of a constructor");
        parameterScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        ConstructorDescriptorImpl constructor = constructorDescriptor.initialize(
                typeParameters,
                resolveValueParameters(constructorDescriptor, parameterScope, valueParameters, trace),
                resolveVisibilityFromModifiers(modifierList, getDefaultConstructorVisibility(classDescriptor))
        );
        if (isAnnotationClass(classDescriptor)) {
            CompileTimeConstantUtils.checkConstructorParametersType(valueParameters, trace);
        }
        return constructor;
    }

    @Nullable
    public ConstructorDescriptorImpl resolvePrimaryConstructorDescriptor(
            @NotNull JetScope scope,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetClass classElement,
            BindingTrace trace
    ) {
        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY || !classElement.hasPrimaryConstructor()) return null;
        return createConstructorDescriptor(
                scope,
                classDescriptor,
                true,
                classElement.getPrimaryConstructorModifierList(),
                classElement,
                classDescriptor.getTypeConstructor().getParameters(), classElement.getPrimaryConstructorParameters(), trace);
    }

    @NotNull
    public ConstructorDescriptorImpl resolveSecondaryConstructorDescriptor(
            @NotNull JetScope scope,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetSecondaryConstructor constructor,
            @NotNull BindingTrace trace
    ) {
        return createConstructorDescriptor(
                scope,
                classDescriptor,
                false,
                constructor.getModifierList(),
                constructor,
                classDescriptor.getTypeConstructor().getParameters(),
                constructor.getValueParameters(),
                trace
        );
    }

    @NotNull
    public PropertyDescriptor resolvePrimaryConstructorParameterToAProperty(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ValueParameterDescriptor valueParameter,
            @NotNull JetScope scope,
            @NotNull JetParameter parameter, BindingTrace trace
    ) {
        JetType type = resolveParameterType(scope, parameter, trace);
        Name name = parameter.getNameAsSafeName();
        boolean isMutable = parameter.isMutable();
        JetModifierList modifierList = parameter.getModifierList();

        if (modifierList != null) {
            if (modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
                trace.report(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.on(parameter));
            }
        }

        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(
                classDescriptor,
                valueParameter.getAnnotations(),
                resolveModalityFromModifiers(parameter, Modality.FINAL),
                resolveVisibilityFromModifiers(parameter, getDefaultVisibility(parameter, classDescriptor)),
                isMutable,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                toSourceElement(parameter)
        );
        propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(),
                                   getDispatchReceiverParameterIfNeeded(classDescriptor), NO_RECEIVER_PARAMETER);

        PropertyGetterDescriptorImpl getter = DescriptorFactory.createDefaultGetter(propertyDescriptor);
        PropertySetterDescriptor setter =
                propertyDescriptor.isVar() ? DescriptorFactory.createDefaultSetter(propertyDescriptor) : null;

        propertyDescriptor.initialize(getter, setter);
        getter.initialize(propertyDescriptor.getType());

        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, propertyDescriptor);
        trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter, propertyDescriptor);
        return propertyDescriptor;
    }

    public static void checkBounds(@NotNull JetTypeReference typeReference, @NotNull JetType type, @NotNull BindingTrace trace) {
        if (type.isError()) return;

        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement == null) return;

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        assert parameters.size() == arguments.size();

        List<JetTypeReference> jetTypeArguments = typeElement.getTypeArgumentsAsTypes();

        // A type reference from Kotlin code can yield a flexible type only if it's `ft<T1, T2>`, whose bounds should not be checked
        if (TypesPackage.isFlexible(type) && !TypesPackage.isDynamic(type)) {
            assert jetTypeArguments.size() == 2
                    : "Flexible type cannot be denoted in Kotlin otherwise than as ft<T1, T2>, but was: "
                      + JetPsiUtil.getElementTextWithContext(typeReference);
            // it's really ft<Foo, Bar>
            Flexibility flexibility = TypesPackage.flexibility(type);
            checkBounds(jetTypeArguments.get(0), flexibility.getLowerBound(), trace);
            checkBounds(jetTypeArguments.get(1), flexibility.getUpperBound(), trace);
            return;
        }

        assert jetTypeArguments.size() == arguments.size() : typeElement.getText() + ": " + jetTypeArguments + " - " + arguments;

        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        for (int i = 0; i < jetTypeArguments.size(); i++) {
            JetTypeReference jetTypeArgument = jetTypeArguments.get(i);

            if (jetTypeArgument == null) continue;

            JetType typeArgument = arguments.get(i).getType();
            checkBounds(jetTypeArgument, typeArgument, trace);

            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
            checkBounds(jetTypeArgument, typeArgument, typeParameterDescriptor, substitutor, trace);
        }
    }

    public static void checkBounds(
            @NotNull JetTypeReference jetTypeArgument,
            @NotNull JetType typeArgument,
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull TypeSubstitutor substitutor,
            @NotNull BindingTrace trace
    ) {
        for (JetType bound : typeParameterDescriptor.getUpperBounds()) {
            JetType substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT);
            if (!JetTypeChecker.DEFAULT.isSubtypeOf(typeArgument, substitutedBound)) {
                trace.report(UPPER_BOUND_VIOLATED.on(jetTypeArgument, substitutedBound, typeArgument));
            }
        }
    }

    public static boolean checkHasOuterClassInstance(
            @NotNull JetScope scope,
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
    public static ClassDescriptor getContainingClass(@NotNull JetScope scope) {
        return getParentOfType(scope.getContainingDeclaration(), ClassDescriptor.class, false);
    }

    public static void checkParameterHasNoValOrVar(
            @NotNull BindingTrace trace,
            @NotNull JetParameter parameter,
            @NotNull DiagnosticFactory1<PsiElement, JetKeywordToken> diagnosticFactory
    ) {
        ASTNode valOrVarNode = parameter.getValOrVarNode();
        if (valOrVarNode != null) {
            trace.report(diagnosticFactory.on(valOrVarNode.getPsi(), ((JetKeywordToken) valOrVarNode.getElementType())));
        }
    }

    private static void checkConstructorParameterHasNoModifier(
            @NotNull BindingTrace trace,
            @NotNull JetParameter parameter
    ) {
        // If is not a property, then it must have no modifier
        if (!parameter.hasValOrVarNode()) {
            checkParameterHasNoModifier(trace, parameter);
        }
    }

    public static void checkParameterHasNoModifier(
            @NotNull BindingTrace trace,
            @NotNull JetParameter parameter
    ) {
        ModifiersChecker.reportIllegalModifiers(parameter.getModifierList(), MODIFIERS_ILLEGAL_ON_PARAMETERS, trace);
    }

    public static void resolvePackageHeader(
            @NotNull JetPackageDirective packageDirective,
            @NotNull ModuleDescriptor module,
            @NotNull BindingTrace trace
    ) {
        for (JetSimpleNameExpression nameExpression : packageDirective.getPackageNames()) {
            FqName fqName = packageDirective.getFqName(nameExpression);

            PackageViewDescriptor packageView = module.getPackage(fqName);
            assert packageView != null : "package not found: " + fqName;
            trace.record(REFERENCE_TARGET, nameExpression, packageView);

            PackageViewDescriptor parentPackageView = packageView.getContainingDeclaration();
            assert parentPackageView != null : "package has no parent: " + packageView;
            trace.record(RESOLUTION_SCOPE, nameExpression, parentPackageView.getMemberScope());
        }
    }

    public static void registerFileInPackage(@NotNull BindingTrace trace, @NotNull JetFile file) {
        // Register files corresponding to this package
        // The trace currently does not support bi-di multimaps that would handle this task nicer
        FqName fqName = file.getPackageFqName();
        Collection<JetFile> files = trace.get(PACKAGE_TO_FILES, fqName);
        if (files == null) {
            files = Sets.newIdentityHashSet();
        }
        files.add(file);
        trace.record(BindingContext.PACKAGE_TO_FILES, fqName, files);
    }
}
