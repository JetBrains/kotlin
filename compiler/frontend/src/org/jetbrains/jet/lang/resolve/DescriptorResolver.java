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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory1;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.CONSTRUCTOR;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.*;
import static org.jetbrains.jet.lexer.JetTokens.OVERRIDE_KEYWORD;
import static org.jetbrains.jet.util.StorageUtil.createRecursionIntolerantLazyValueWithDefault;

public class DescriptorResolver {
    public static final Name COPY_METHOD_NAME = Name.identifier("copy");
    public static final String COMPONENT_FUNCTION_NAME_PREFIX = "component";

    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private AnnotationResolver annotationResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private DelegatedPropertyResolver delegatedPropertyResolver;

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

    public void resolveMutableClassDescriptor(
            @NotNull JetClass classElement,
            @NotNull MutableClassDescriptor descriptor,
            BindingTrace trace
    ) {
        // TODO : Where-clause
        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        int index = 0;
        for (JetTypeParameter typeParameter : classElement.getTypeParameters()) {
            // TODO: Support
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(typeParameter, trace);

            TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                    descriptor,
                    Collections.<AnnotationDescriptor>emptyList(),
                    typeParameter.hasModifier(JetTokens.REIFIED_KEYWORD),
                    typeParameter.getVariance(),
                    JetPsiUtil.safeName(typeParameter.getName()),
                    index
            );
            trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
            typeParameters.add(typeParameterDescriptor);
            index++;
        }
        descriptor.setTypeParameterDescriptors(typeParameters);
        Modality defaultModality = descriptor.getKind() == ClassKind.TRAIT ? Modality.ABSTRACT : Modality.FINAL;
        descriptor.setModality(resolveModalityFromModifiers(classElement, defaultModality));
        descriptor.setVisibility(resolveVisibilityFromModifiers(classElement, getDefaultClassVisibility(descriptor)));

        trace.record(BindingContext.CLASS, classElement, descriptor);
    }

    public void resolveSupertypesForMutableClassDescriptor(
            @NotNull JetClassOrObject jetClass,
            @NotNull MutableClassDescriptor descriptor,
            BindingTrace trace
    ) {
        for (JetType supertype : resolveSupertypes(descriptor.getScopeForSupertypeResolution(), descriptor, jetClass, trace)) {
            descriptor.addSupertype(supertype);
        }
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
            supertypes.add(0, KotlinBuiltIns.getInstance().getEnumType(classDescriptor.getDefaultType()));
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
            JetClassOrObject parent = PsiTreeUtil.getParentOfType(jetClass, JetClassOrObject.class);
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
            return KotlinBuiltIns.getInstance().getAnnotationType();
        }
        return KotlinBuiltIns.getInstance().getAnyType();
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
                result.add(resolver.resolveType(extensibleScope, typeReference, trace, checkBounds));
                JetTypeElement bareSuperType = checkNullableSupertypeAndStripQuestionMarks(trace, typeReference.getTypeElement());
                checkProjectionsInImmediateArguments(trace, bareSuperType);
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
            if (!(typeElement instanceof JetNullableType)) {
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
            @NotNull List<AnnotationDescriptor> annotations
    ) {
        final SimpleFunctionDescriptorImpl functionDescriptor = new SimpleFunctionDescriptorImpl(
                containingDescriptor,
                annotations,
                JetPsiUtil.safeName(function.getName()),
                CallableMemberDescriptor.Kind.DECLARATION
        );
        WritableScope innerScope = new WritableScopeImpl(scope, functionDescriptor, new TraceBasedRedeclarationHandler(trace),
                                                         "Function descriptor header scope");
        innerScope.addLabeledDeclaration(functionDescriptor);

        List<TypeParameterDescriptorImpl> typeParameterDescriptors =
                resolveTypeParametersForCallableDescriptor(functionDescriptor, innerScope, function.getTypeParameters(), trace);
        innerScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        resolveGenericBounds(function, innerScope, typeParameterDescriptors, trace);

        JetType receiverType = null;
        JetTypeReference receiverTypeRef = function.getReceiverTypeRef();
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

        JetTypeReference returnTypeRef = function.getReturnTypeRef();
        JetType returnType;
        if (returnTypeRef != null) {
            returnType = typeResolver.resolveType(innerScope, returnTypeRef, trace, true);
        }
        else if (function.hasBlockBody()) {
            returnType = KotlinBuiltIns.getInstance().getUnitType();
        }
        else {
            JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression != null) {
                returnType =
                        DeferredType.create(trace,
                                            createRecursionIntolerantLazyValueWithDefault(
                                                    ErrorUtils.createErrorType("Recursive dependency"),
                                                    new Function0<JetType>() {
                                                        @Override
                                                        public JetType invoke() {
                                                            JetType type = expressionTypingServices
                                                                    .getBodyExpressionType(trace, scope, dataFlowInfo, function,
                                                                                           functionDescriptor);
                                                            return transformAnonymousTypeIfNeeded(functionDescriptor, function, type,
                                                                                                  trace);
                                                        }
                                                    }));
            }
            else {
                returnType = ErrorUtils.createErrorType("No type, no body");
            }
        }
        boolean hasBody = function.getBodyExpression() != null;
        Modality modality = resolveModalityFromModifiers(function, getDefaultModality(containingDescriptor, hasBody));
        Visibility visibility = resolveVisibilityFromModifiers(function, getDefaultVisibility(function, containingDescriptor));
        functionDescriptor.initialize(
                receiverType,
                getExpectedThisObjectIfNeeded(containingDescriptor),
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
        String functionName = COMPONENT_FUNCTION_NAME_PREFIX + parameterIndex;
        JetType returnType = property.getType();

        SimpleFunctionDescriptorImpl functionDescriptor = new SimpleFunctionDescriptorImpl(
                classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(),
                Name.identifier(functionName),
                CallableMemberDescriptor.Kind.SYNTHESIZED
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

        SimpleFunctionDescriptorImpl functionDescriptor = new SimpleFunctionDescriptorImpl(
                classDescriptor,
                Collections.<AnnotationDescriptor>emptyList(),
                COPY_METHOD_NAME,
                CallableMemberDescriptor.Kind.SYNTHESIZED
        );

        List<ValueParameterDescriptor> parameterDescriptors = Lists.newArrayList();

        for (ValueParameterDescriptor parameter : constructorParameters) {
            PropertyDescriptor propertyDescriptor = trace.getBindingContext().get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter);
            // If parameter hasn't corresponding property, so it mustn't have default value as a parameter in copy function for data class
            boolean declaresDefaultValue = propertyDescriptor != null;
            ValueParameterDescriptorImpl parameterDescriptor =
                    new ValueParameterDescriptorImpl(functionDescriptor, parameter.getIndex(), parameter.getAnnotations(),
                                                     parameter.getName(), parameter.getType(),
                                                     declaresDefaultValue,
                                                     parameter.getVarargElementType());
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
                classDescriptor.getVisibility()
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
    private static ValueParameterDescriptorImpl resolveValueParameterDescriptor(
            DeclarationDescriptor declarationDescriptor,
            JetParameter valueParameter, int index, JetType type, BindingTrace trace,
            List<AnnotationDescriptor> annotations
    ) {
        JetType varargElementType = null;
        JetType variableType = type;
        if (valueParameter.hasModifier(JetTokens.VARARG_KEYWORD)) {
            varargElementType = type;
            variableType = DescriptorUtils.getVarargParameterType(type);
        }
        ValueParameterDescriptorImpl valueParameterDescriptor = new ValueParameterDescriptorImpl(
                declarationDescriptor,
                index,
                annotations,
                JetPsiUtil.safeName(valueParameter.getName()),
                variableType,
                valueParameter.getDefaultValue() != null,
                varargElementType
        );

        trace.record(BindingContext.VALUE_PARAMETER, valueParameter, valueParameterDescriptor);
        return valueParameterDescriptor;
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
                Collections.<AnnotationDescriptor>emptyList(),
                typeParameter.hasModifier(JetTokens.REIFIED_KEYWORD),
                typeParameter.getVariance(),
                JetPsiUtil.safeName(typeParameter.getName()),
                index
        );
        trace.record(BindingContext.TYPE_PARAMETER, typeParameter, typeParameterDescriptor);
        extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    @NotNull
    public static ConstructorDescriptorImpl createAndRecordPrimaryConstructorForObject(
            @Nullable PsiElement object,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull BindingTrace trace
    ) {
        ConstructorDescriptorImpl constructorDescriptor = DescriptorFactory.createPrimaryConstructorForObject(classDescriptor);
        if (object != null) {
            trace.record(CONSTRUCTOR, object, constructorDescriptor);
        }
        return constructorDescriptor;
    }

    static final class UpperBoundCheckerTask {
        JetTypeReference upperBound;
        JetType upperBoundType;
        boolean isClassObjectConstraint;

        private UpperBoundCheckerTask(JetTypeReference upperBound, JetType upperBoundType, boolean classObjectConstraint) {
            this.upperBound = upperBound;
            this.upperBoundType = upperBoundType;
            isClassObjectConstraint = classObjectConstraint;
        }
    }

    public void resolveGenericBounds(
            @NotNull JetTypeParameterListOwner declaration,
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
            if (constraint.isClassObjectContraint()) {
                trace.report(UNSUPPORTED.on(constraint, "Class objects constraints are not supported yet"));
            }

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
                        .add(new UpperBoundCheckerTask(boundTypeReference, bound, constraint.isClassObjectContraint()));
            }

            if (typeParameterDescriptor == null) {
                // To tell the user that we look only for locally defined type parameters
                ClassifierDescriptor classifier = scope.getClassifier(referencedName);
                if (classifier != null) {
                    trace.report(NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER.on(subjectTypeParameterName, constraint, declaration));
                    trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, classifier);
                }
                else {
                    trace.report(UNRESOLVED_REFERENCE.on(subjectTypeParameterName, subjectTypeParameterName));
                }
            }
            else {
                trace.record(BindingContext.REFERENCE_TARGET, subjectTypeParameterName, typeParameterDescriptor);
                if (bound != null) {
                    if (constraint.isClassObjectContraint()) {
                        typeParameterDescriptor.addClassObjectBound(bound);
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

            if (KotlinBuiltIns.getInstance().isNothing(parameter.getUpperBoundsAsType())) {
                PsiElement nameIdentifier = typeParameters.get(parameter.getIndex()).getNameIdentifier();
                if (nameIdentifier != null) {
                    trace.report(CONFLICTING_UPPER_BOUNDS.on(nameIdentifier, parameter));
                }
            }

            JetType classObjectType = parameter.getClassObjectType();
            if (classObjectType != null && KotlinBuiltIns.getInstance().isNothing(classObjectType)) {
                PsiElement nameIdentifier = typeParameters.get(parameter.getIndex()).getNameIdentifier();
                if (nameIdentifier != null) {
                    trace.report(CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS.on(nameIdentifier, parameter));
                }
            }
        }

        for (UpperBoundCheckerTask checkerTask : deferredUpperBoundCheckerTasks) {
            checkUpperBoundType(checkerTask.upperBound, checkerTask.upperBoundType, checkerTask.isClassObjectConstraint, trace);
        }
    }

    private static void checkUpperBoundType(
            JetTypeReference upperBound,
            JetType upperBoundType,
            boolean isClassObjectConstraint,
            BindingTrace trace
    ) {
        if (!TypeUtils.canHaveSubtypes(JetTypeChecker.INSTANCE, upperBoundType)) {
            if (isClassObjectConstraint) {
                trace.report(FINAL_CLASS_OBJECT_UPPER_BOUND.on(upperBound, upperBoundType));
            }
            else {
                trace.report(FINAL_UPPER_BOUND.on(upperBound, upperBoundType));
            }
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
        if (parameter.hasModifier(JetTokens.VARARG_KEYWORD)) {
            return DescriptorUtils.getVarargParameterType(type);
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
                false);
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
        if (JetPsiUtil.isScriptDeclaration(variable)) {
            PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                    containingDeclaration,
                    annotationResolver.resolveAnnotationsWithArguments(scope, variable.getModifierList(), trace),
                    Modality.FINAL,
                    Visibilities.INTERNAL,
                    variable.isVar(),
                    JetPsiUtil.safeName(variable.getName()),
                    CallableMemberDescriptor.Kind.DECLARATION
            );

            JetType type =
                    getVariableType(propertyDescriptor, scope, variable, dataFlowInfo, false, trace); // For a local variable the type must not be deferred

            ReceiverParameterDescriptor receiverParameter = ((ScriptDescriptor) containingDeclaration).getThisAsReceiverParameter();
            propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(), receiverParameter, (JetType) null);
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
                variable.isVar());
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
        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                containingDeclaration,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace),
                modality,
                visibility,
                isVar,
                JetPsiUtil.safeName(property.getName()),
                CallableMemberDescriptor.Kind.DECLARATION
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
                typeParameterDescriptors = resolveTypeParametersForCallableDescriptor(containingDeclaration, writableScope, typeParameters,
                                                                                      trace);
                writableScope.changeLockLevel(WritableScope.LockLevel.READING);
                resolveGenericBounds(property, writableScope, typeParameterDescriptors, trace);
                scopeWithTypeParameters = writableScope;
            }

            JetTypeReference receiverTypeRef = property.getReceiverTypeRef();
            if (receiverTypeRef != null) {
                receiverType = typeResolver.resolveType(scopeWithTypeParameters, receiverTypeRef, trace, true);
            }
        }

        ReceiverParameterDescriptor receiverDescriptor = DescriptorFactory.createReceiverParameterForCallable(propertyDescriptor,
                                                                                                              receiverType);

        JetScope propertyScope = JetScopeUtils.getPropertyDeclarationInnerScope(propertyDescriptor, scope, typeParameterDescriptors,
                                                                                NO_RECEIVER_PARAMETER, trace);

        JetType type = getVariableType(propertyDescriptor, propertyScope, property, dataFlowInfo, true, trace);

        propertyDescriptor.setType(type, typeParameterDescriptors, getExpectedThisObjectIfNeeded(containingDeclaration),
                                   receiverDescriptor);

        PropertyGetterDescriptorImpl getter = resolvePropertyGetterDescriptor(scopeWithTypeParameters, property, propertyDescriptor, trace);
        PropertySetterDescriptor setter = resolvePropertySetterDescriptor(scopeWithTypeParameters, property, propertyDescriptor, trace);

        propertyDescriptor.initialize(getter, setter);

        trace.record(BindingContext.VARIABLE, property, propertyDescriptor);
        return propertyDescriptor;
    }

    /*package*/
    static boolean hasBody(JetProperty property) {
        boolean hasBody = property.getDelegateExpressionOrInitializer() != null;
        if (!hasBody) {
            JetPropertyAccessor getter = property.getGetter();
            if (getter != null && getter.getBodyExpression() != null) {
                hasBody = true;
            }
            JetPropertyAccessor setter = property.getSetter();
            if (!hasBody && setter != null && setter.getBodyExpression() != null) {
                hasBody = true;
            }
        }
        return hasBody;
    }

    @NotNull
    private JetType getVariableType(
            @NotNull final VariableDescriptor variableDescriptor,
            @NotNull final JetScope scope,
            @NotNull final JetVariableDeclaration variable,
            @NotNull final DataFlowInfo dataFlowInfo,
            boolean notLocal,
            @NotNull final BindingTrace trace
    ) {
        JetTypeReference propertyTypeRef = variable.getTypeRef();

        boolean hasDelegate = variable instanceof JetProperty && ((JetProperty) variable).getDelegateExpression() != null;
        if (propertyTypeRef == null) {
            final JetExpression initializer = variable.getInitializer();
            if (initializer == null) {
                if (hasDelegate && variableDescriptor instanceof PropertyDescriptor) {
                    final JetProperty property = (JetProperty) variable;
                    final JetExpression propertyDelegateExpression = property.getDelegateExpression();
                    if (propertyDelegateExpression != null) {
                        return DeferredType.create(
                                trace,
                                createRecursionIntolerantLazyValueWithDefault(
                                        ErrorUtils.createErrorType("Recursive dependency"),
                                        new Function0<JetType>() {
                                            @Override
                                            public JetType invoke() {
                                                return resolveDelegatedPropertyType(property, (PropertyDescriptor) variableDescriptor, scope,
                                                                                    propertyDelegateExpression, dataFlowInfo, trace);
                                            }
                                        }));
                    }
                }
                if (!notLocal) {
                    trace.report(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.on(variable));
                }
                return ErrorUtils.createErrorType("No type, no body");
            }
            else {
                if (notLocal) {
                    return DeferredType.create(trace,
                                               createRecursionIntolerantLazyValueWithDefault(
                                                       ErrorUtils.createErrorType("Recursive dependency"),
                                                       new Function0<JetType>() {
                                                           @Override
                                                           public JetType invoke() {
                                                               JetType type =
                                                                       resolveInitializerType(scope, initializer, dataFlowInfo, trace);

                                                               return transformAnonymousTypeIfNeeded(variableDescriptor, variable, type,
                                                                                                     trace);
                                                           }
                                                       }
                                               ));
                }
                else {
                    return resolveInitializerType(scope, initializer, dataFlowInfo, trace);
                }
            }
        }
        else {
            return typeResolver.resolveType(scope, propertyTypeRef, trace, true);
        }
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
        boolean isLocal = descriptor.getContainingDeclaration() instanceof CallableDescriptor;
        Visibility visibility = descriptor.getVisibility();
        boolean transformNeeded = !isLocal && !visibility.isPublicAPI()
                                  && !(definedInClass && Visibilities.PRIVATE.equals(visibility));
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
            List<AnnotationDescriptor> annotations =
                    annotationResolver.resolveAnnotationsWithoutArguments(scope, setter.getModifierList(), trace);
            JetParameter parameter = setter.getParameter();

            setterDescriptor = new PropertySetterDescriptorImpl(
                    propertyDescriptor, annotations,
                    resolveModalityFromModifiers(setter, propertyDescriptor.getModality()),
                    resolveVisibilityFromModifiers(setter, propertyDescriptor.getVisibility()),
                    setter.getBodyExpression() != null, false, CallableMemberDescriptor.Kind.DECLARATION);
            if (parameter != null) {

                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case
                JetExpression defaultValue = parameter.getDefaultValue();
                if (defaultValue != null) {
                    trace.report(SETTER_PARAMETER_WITH_DEFAULT_VALUE.on(defaultValue));
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
            setterDescriptor = DescriptorFactory.createSetter(propertyDescriptor, property.getDelegateExpression() == null);
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
            List<AnnotationDescriptor> annotations =
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

            getterDescriptor = new PropertyGetterDescriptorImpl(
                    propertyDescriptor, annotations,
                    resolveModalityFromModifiers(getter, propertyDescriptor.getModality()),
                    resolveVisibilityFromModifiers(getter, propertyDescriptor.getVisibility()),
                    getter.getBodyExpression() != null, false, CallableMemberDescriptor.Kind.DECLARATION);
            getterDescriptor.initialize(returnType);
            trace.record(BindingContext.PROPERTY_ACCESSOR, getter, getterDescriptor);
        }
        else {
            getterDescriptor = DescriptorFactory.createGetter(propertyDescriptor, property.getDelegateExpression() == null);
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
        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace),
                isPrimary
        );
        trace.record(BindingContext.CONSTRUCTOR, declarationToTrace, constructorDescriptor);
        WritableScopeImpl parameterScope = new WritableScopeImpl(
                scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "Scope with value parameters of a constructor");
        parameterScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        ConstructorDescriptorImpl constructor = constructorDescriptor.initialize(
                typeParameters,
                resolveValueParameters(
                        constructorDescriptor,
                        parameterScope,
                        valueParameters, trace),
                resolveVisibilityFromModifiers(modifierList, getDefaultConstructorVisibility(classDescriptor)),
                DescriptorUtils.isConstructorOfStaticNestedClass(constructorDescriptor));
        if (isAnnotationClass(classDescriptor)) {
            AnnotationUtils.checkConstructorParametersType(valueParameters, trace);
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
        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) return null;
        return createConstructorDescriptor(
                scope,
                classDescriptor,
                true,
                classElement.getPrimaryConstructorModifierList(),
                classElement,
                classDescriptor.getTypeConstructor().getParameters(), classElement.getPrimaryConstructorParameters(), trace);
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
            ASTNode abstractNode = modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD);
            if (abstractNode != null) {
                trace.report(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS.on(parameter));
            }
        }

        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                classDescriptor,
                valueParameter.getAnnotations(),
                resolveModalityFromModifiers(parameter, Modality.FINAL),
                resolveVisibilityFromModifiers(parameter, Visibilities.INTERNAL),
                isMutable,
                name,
                CallableMemberDescriptor.Kind.DECLARATION
        );
        propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(),
                                   getExpectedThisObjectIfNeeded(classDescriptor), NO_RECEIVER_PARAMETER);

        PropertyGetterDescriptorImpl getter = DescriptorFactory.createDefaultGetter(propertyDescriptor);
        PropertySetterDescriptor setter =
                propertyDescriptor.isVar() ? DescriptorFactory.createDefaultSetter(propertyDescriptor) : null;

        propertyDescriptor.initialize(getter, setter);
        getter.initialize(propertyDescriptor.getType());

        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, propertyDescriptor);
        trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter, propertyDescriptor);
        return propertyDescriptor;
    }

    public static void checkBounds(@NotNull JetTypeReference typeReference, @NotNull JetType type, BindingTrace trace) {
        if (type.isError()) return;

        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement == null) return;

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        assert parameters.size() == arguments.size();

        List<JetTypeReference> jetTypeArguments = typeElement.getTypeArgumentsAsTypes();
        assert jetTypeArguments.size() == arguments.size() : typeElement.getText();

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
            @NotNull TypeSubstitutor substitutor, BindingTrace trace
    ) {
        for (JetType bound : typeParameterDescriptor.getUpperBounds()) {
            JetType substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT);
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(typeArgument, substitutedBound)) {
                trace.report(UPPER_BOUND_VIOLATED.on(jetTypeArgument, substitutedBound, typeArgument));
            }
        }
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumClassObjectValuesMethod(
            @NotNull ClassDescriptor classObject,
            @NotNull BindingTrace trace
    ) {
        final ClassDescriptor enumClassDescriptor = (ClassDescriptor) classObject.getContainingDeclaration();
        assert DescriptorUtils.isEnumClass(enumClassDescriptor) : "values should be created in enum class: " + enumClassDescriptor;
        return DescriptorFactory
                .createEnumClassObjectValuesMethod(classObject, DeferredType.create(trace, new Function0<JetType>() {
                    @Override
                    public JetType invoke() {
                        return KotlinBuiltIns.getInstance().getArrayType(enumClassDescriptor.getDefaultType());
                    }
                }));
    }


    @NotNull
    public static SimpleFunctionDescriptor createEnumClassObjectValueOfMethod(
            @NotNull ClassDescriptor classObject,
            @NotNull BindingTrace trace
    ) {
        final ClassDescriptor enumClassDescriptor = (ClassDescriptor) classObject.getContainingDeclaration();
        assert DescriptorUtils.isEnumClass(enumClassDescriptor) : "valueOf should be created in enum class: " + enumClassDescriptor;
        return DescriptorFactory
                .createEnumClassObjectValueOfMethod(classObject, DeferredType.create(trace, new Function0<JetType>() {
                    @Override
                    public JetType invoke() {
                        return enumClassDescriptor.getDefaultType();
                    }
                }));
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
        if (parameter.getValOrVarNode() == null) {
            checkParameterHasNoModifier(trace, parameter);
        }
    }

    public static void checkParameterHasNoModifier(
            @NotNull BindingTrace trace,
            @NotNull JetParameter parameter
    ) {
        JetModifierList modifiers = parameter.getModifierList();
        if (modifiers != null) {
            ASTNode node = modifiers.getNode().getFirstChildNode();

            while (node != null) {
                IElementType elementType = node.getElementType();

                if (elementType != JetTokens.VARARG_KEYWORD && elementType instanceof JetKeywordToken) {
                    trace.report(ILLEGAL_MODIFIER.on(node.getPsi(), (JetKeywordToken) elementType));
                }
                node = node.getTreeNext();
            }
        }
    }
}
