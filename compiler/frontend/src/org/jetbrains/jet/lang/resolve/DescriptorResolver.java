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
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValueWithDefault;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.CONSTRUCTOR;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;
import static org.jetbrains.jet.lang.resolve.ModifiersChecker.*;
import static org.jetbrains.jet.lexer.JetTokens.OVERRIDE_KEYWORD;

public class DescriptorResolver {
    public static final Name VALUE_OF_METHOD_NAME = Name.identifier("valueOf");
    public static final Name VALUES_METHOD_NAME = Name.identifier("values");
    public static final Name COPY_METHOD_NAME = Name.identifier("copy");
    public static final String COMPONENT_FUNCTION_NAME_PREFIX = "component";

    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private AnnotationResolver annotationResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    public static ReceiverParameterDescriptor resolveReceiverParameterFor(@NotNull CallableDescriptor owner, @Nullable JetType receiverParameterType) {
        return receiverParameterType == null
                                 ? NO_RECEIVER_PARAMETER
                                 : new ReceiverParameterDescriptorImpl(owner, receiverParameterType, new ExtensionReceiver(owner, receiverParameterType));
    }

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


    public void resolveMutableClassDescriptor(
            @NotNull JetClass classElement,
            @NotNull MutableClassDescriptor descriptor,
            BindingTrace trace
    ) {
        // TODO : Where-clause
        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        int index = 0;
        for (JetTypeParameter typeParameter : classElement.getTypeParameters()) {
            TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                    descriptor,
                    annotationResolver.getResolvedAnnotations(typeParameter.getModifierList(), trace),
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
        if (!ErrorUtils.isErrorType(declaredSupertype)) {
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
    public SimpleFunctionDescriptor resolveFunctionDescriptor(
            DeclarationDescriptor containingDescriptor,
            final JetScope scope,
            final JetNamedFunction function,
            final BindingTrace trace
    ) {
        final SimpleFunctionDescriptorImpl functionDescriptor = new SimpleFunctionDescriptorImpl(
                containingDescriptor,
                annotationResolver.resolveAnnotations(scope, function.getModifierList(), trace),
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
            final JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression != null) {
                returnType =
                        DeferredType.create(trace, new RecursionIntolerantLazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency")) {
                            @Override
                            protected JetType compute() {
                                //JetFlowInformationProvider flowInformationProvider = computeFlowData(function, bodyExpression);
                                return expressionTypingServices.inferFunctionReturnType(scope, function, functionDescriptor, trace);
                            }
                        });
            }
            else {
                returnType = ErrorUtils.createErrorType("No type, no body");
            }
        }
        boolean hasBody = function.getBodyExpression() != null;
        Modality modality = resolveModalityFromModifiers(function, getDefaultModality(containingDescriptor, hasBody));
        Visibility visibility = resolveVisibilityFromModifiers(function, getDefaultVisibility(function, containingDescriptor));
        JetModifierList modifierList = function.getModifierList();
        boolean isInline = (modifierList != null) && modifierList.hasModifier(JetTokens.INLINE_KEYWORD);
        functionDescriptor.initialize(
                receiverType,
                getExpectedThisObjectIfNeeded(containingDescriptor),
                typeParameterDescriptors,
                valueParameterDescriptors,
                returnType,
                modality,
                visibility,
                isInline);

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
                property.getVisibility(),
                true
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
                                                     parameter.getName(), parameter.isVar(), parameter.getType(),
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
                classDescriptor.getVisibility(),
                true
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
        for (int i = 0, valueParametersSize = valueParameters.size(); i < valueParametersSize; i++) {
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

            ValueParameterDescriptor valueParameterDescriptor =
                    resolveValueParameterDescriptor(parameterScope, functionDescriptor, valueParameter, i, type, trace);
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
            result.add(valueParameterDescriptor);
        }
        return result;
    }

    @NotNull
    public MutableValueParameterDescriptor resolveValueParameterDescriptor(
            JetScope scope, DeclarationDescriptor declarationDescriptor,
            JetParameter valueParameter, int index, JetType type, BindingTrace trace
    ) {
        JetType varargElementType = null;
        JetType variableType = type;
        if (valueParameter.hasModifier(JetTokens.VARARG_KEYWORD)) {
            varargElementType = type;
            variableType = DescriptorUtils.getVarargParameterType(type);
        }
        MutableValueParameterDescriptor valueParameterDescriptor = new ValueParameterDescriptorImpl(
                declarationDescriptor,
                index,
                annotationResolver.resolveAnnotations(scope, valueParameter.getModifierList(), trace),
                JetPsiUtil.safeName(valueParameter.getName()),
                valueParameter.isMutable(),
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

        // TODO: Annotations are not resolved!
        TypeParameterDescriptorImpl typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDescriptor,
                annotationResolver.getResolvedAnnotations(typeParameter.getModifierList(), trace),
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
        ConstructorDescriptorImpl constructorDescriptor = createPrimaryConstructorForObject(classDescriptor);
        if (object != null) {
            trace.record(CONSTRUCTOR, object, constructorDescriptor);
        }
        return constructorDescriptor;
    }

    @NotNull
    public static ConstructorDescriptorImpl createPrimaryConstructorForObject(@NotNull ClassDescriptor containingClass) {
        ConstructorDescriptorImpl constructorDescriptor =
                new ConstructorDescriptorImpl(containingClass, Collections.<AnnotationDescriptor>emptyList(), true);
        constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(),
                                         Collections.<ValueParameterDescriptor>emptyList(),
                                         getDefaultConstructorVisibility(containingClass));
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
                    trace.report(UNRESOLVED_REFERENCE.on(subjectTypeParameterName));
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
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetScope scope,
            @NotNull JetParameter parameter,
            BindingTrace trace
    ) {
        JetType type = resolveParameterType(scope, parameter, trace);
        return resolveLocalVariableDescriptor(containingDeclaration, parameter, type, trace);
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
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetParameter parameter,
            @NotNull JetType type,
            BindingTrace trace
    ) {
        VariableDescriptor variableDescriptor = new LocalVariableDescriptor(
                containingDeclaration,
                annotationResolver.getResolvedAnnotations(parameter.getModifierList(), trace),
                JetPsiUtil.safeName(parameter.getName()),
                type,
                parameter.isMutable());
        trace.record(BindingContext.VALUE_PARAMETER, parameter, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(
            DeclarationDescriptor containingDeclaration,
            JetScope scope,
            JetVariableDeclaration variable,
            DataFlowInfo dataFlowInfo,
            BindingTrace trace
    ) {
        if (JetPsiUtil.isScriptDeclaration(variable)) {
            PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                    containingDeclaration,
                    annotationResolver.getResolvedAnnotations(variable.getModifierList(), trace),
                    Modality.FINAL,
                    Visibilities.INTERNAL,
                    variable.isVar(),
                    JetPsiUtil.safeName(variable.getName()),
                    CallableMemberDescriptor.Kind.DECLARATION
            );

            JetType type =
                    getVariableType(scope, variable, dataFlowInfo, false, trace); // For a local variable the type must not be deferred

            ReceiverParameterDescriptor receiverParameter = ((ScriptDescriptor) containingDeclaration).getThisAsReceiverParameter();
            propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(), receiverParameter, (JetType) null);
            trace.record(BindingContext.VARIABLE, variable, propertyDescriptor);
            return propertyDescriptor;
        }
        else {
            VariableDescriptorImpl variableDescriptor =
                    resolveLocalVariableDescriptorWithType(containingDeclaration, variable, null, trace);

            JetType type =
                    getVariableType(scope, variable, dataFlowInfo, false, trace); // For a local variable the type must not be deferred
            variableDescriptor.setOutType(type);
            return variableDescriptor;
        }
    }

    @NotNull
    public VariableDescriptorImpl resolveLocalVariableDescriptorWithType(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetVariableDeclaration variable,
            @Nullable JetType type,
            @NotNull BindingTrace trace
    ) {
        VariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(
                containingDeclaration,
                annotationResolver.getResolvedAnnotations(variable.getModifierList(), trace),
                JetPsiUtil.safeName(variable.getName()),
                type,
                variable.isVar());
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public VariableDescriptor resolveObjectDeclaration(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject objectDeclaration,
            @NotNull ClassDescriptor classDescriptor, BindingTrace trace
    ) {
        boolean isProperty = (containingDeclaration instanceof NamespaceDescriptor)
                             || (containingDeclaration instanceof ClassDescriptor);
        if (isProperty) {
            return resolveObjectDeclarationAsPropertyDescriptor(containingDeclaration, objectDeclaration, classDescriptor, trace);
        }
        else {
            return resolveObjectDeclarationAsLocalVariable(containingDeclaration, objectDeclaration, classDescriptor, trace);
        }
    }

    @NotNull
    public PropertyDescriptor resolveObjectDeclarationAsPropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject objectDeclaration,
            @NotNull ClassDescriptor classDescriptor, BindingTrace trace
    ) {
        JetModifierList modifierList = objectDeclaration.getModifierList();
        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                containingDeclaration,
                annotationResolver.getResolvedAnnotations(modifierList, trace),
                Modality.FINAL,
                resolveVisibilityFromModifiers(objectDeclaration, getDefaultVisibilityForObjectPropertyDescriptor(classDescriptor)),
                false,
                JetPsiUtil.safeName(objectDeclaration.getName()),
                CallableMemberDescriptor.Kind.DECLARATION
        );
        propertyDescriptor.setType(getTypeForObjectDeclaration(classDescriptor), Collections.<TypeParameterDescriptor>emptyList(),
                                   getExpectedThisObjectIfNeeded(containingDeclaration), NO_RECEIVER_PARAMETER);
        propertyDescriptor.initialize(createDefaultGetter(propertyDescriptor), null);
        trace.record(BindingContext.OBJECT_DECLARATION_CLASS, propertyDescriptor, classDescriptor);
        JetObjectDeclarationName nameAsDeclaration = objectDeclaration.getNameAsDeclaration();
        if (nameAsDeclaration != null) {
            trace.record(BindingContext.OBJECT_DECLARATION, nameAsDeclaration, propertyDescriptor);
        }
        return propertyDescriptor;
    }

    @NotNull
    private static JetType getTypeForObjectDeclaration(@NotNull ClassDescriptor objectClassDescriptor) {
        if (objectClassDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            DeclarationDescriptor containingDeclaration = objectClassDescriptor.getContainingDeclaration().getContainingDeclaration();
            assert containingDeclaration instanceof ClassDescriptor;
            ClassDescriptor enumClass = (ClassDescriptor) containingDeclaration;
            assert enumClass.getKind() == ClassKind.ENUM_CLASS;
            return enumClass.getDefaultType();
        }
        return objectClassDescriptor.getDefaultType();
    }

    @NotNull
    private VariableDescriptor resolveObjectDeclarationAsLocalVariable(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetClassOrObject objectDeclaration,
            @NotNull ClassDescriptor classDescriptor, BindingTrace trace
    ) {
        VariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(
                containingDeclaration,
                annotationResolver.getResolvedAnnotations(objectDeclaration.getModifierList(), trace),
                JetPsiUtil.safeName(objectDeclaration.getName()),
                classDescriptor.getDefaultType(),
                /*isVar =*/ false);
        trace.record(BindingContext.OBJECT_DECLARATION_CLASS, variableDescriptor, classDescriptor);
        JetObjectDeclarationName nameAsDeclaration = objectDeclaration.getNameAsDeclaration();
        if (nameAsDeclaration != null) {
            trace.record(BindingContext.VARIABLE, nameAsDeclaration, variableDescriptor);
        }
        return variableDescriptor;
    }

    public JetScope getPropertyDeclarationInnerScope(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(propertyDescriptor, outerScope, typeParameters, receiver, trace, true);
    }

    public JetScope getPropertyDeclarationInnerScopeForInitializer(
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace
    ) {
        return getPropertyDeclarationInnerScope(null, outerScope, typeParameters, receiver, trace, false);
    }

    private JetScope getPropertyDeclarationInnerScope(
            @Nullable PropertyDescriptor propertyDescriptor, // PropertyDescriptor can be null for property scope which hasn't label to property (in this case addLabelForProperty parameter must be false
            @NotNull JetScope outerScope,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor receiver,
            BindingTrace trace,
            boolean addLabelForProperty
    ) {
        WritableScopeImpl result = new WritableScopeImpl(
                outerScope, outerScope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(trace),
                "Property declaration inner scope");
        if (addLabelForProperty) {
            assert propertyDescriptor != null : "PropertyDescriptor can be null for property scope which hasn't label to property";
            result.addLabeledDeclaration(propertyDescriptor);
        }
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            result.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        if (receiver != null) {
            result.setImplicitReceiver(receiver);
        }
        result.changeLockLevel(WritableScope.LockLevel.READING);
        return result;
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetScope scope,
            @NotNull JetProperty property,
            BindingTrace trace
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
                annotationResolver.resolveAnnotations(scope, modifierList, trace),
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

        ReceiverParameterDescriptor receiverDescriptor = resolveReceiverParameterFor(propertyDescriptor, receiverType);

        JetScope propertyScope = getPropertyDeclarationInnerScope(propertyDescriptor, scope, typeParameterDescriptors,
                                                                  NO_RECEIVER_PARAMETER, trace);

        JetType type = getVariableType(propertyScope, property, DataFlowInfo.EMPTY, true, trace);

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
        boolean hasBody = property.getInitializer() != null;
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
            @NotNull final JetScope scope,
            @NotNull final JetVariableDeclaration variable,
            @NotNull final DataFlowInfo dataFlowInfo,
            boolean notLocal,
            final BindingTrace trace
    ) {
        JetTypeReference propertyTypeRef = variable.getTypeRef();

        if (propertyTypeRef == null) {
            final JetExpression initializer = variable.getInitializer();
            if (initializer == null) {
                if (!notLocal) {
                    trace.report(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.on(variable));
                }
                return ErrorUtils.createErrorType("No type, no body");
            }
            else {
                RecursionIntolerantLazyValue<JetType> lazyValue = new RecursionIntolerantLazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency")) {
                    @Override
                    protected JetType compute() {
                        return expressionTypingServices.safeGetType(scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, trace);
                    }
                };
                if (notLocal) {
                    return DeferredType.create(trace, lazyValue);
                }
                else {
                    return lazyValue.get();
                }
            }
        }
        else {
            return typeResolver.resolveType(scope, propertyTypeRef, trace, true);
        }
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
            List<AnnotationDescriptor> annotations = annotationResolver.resolveAnnotations(scope, setter.getModifierList(), trace);
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

                MutableValueParameterDescriptor valueParameterDescriptor =
                        resolveValueParameterDescriptor(scope, setterDescriptor, parameter, 0, type, trace);
                setterDescriptor.initialize(valueParameterDescriptor);
            }
            else {
                setterDescriptor.initializeDefault();
            }

            trace.record(BindingContext.PROPERTY_ACCESSOR, setter, setterDescriptor);
        }
        else if (property.isVar()) {
            setterDescriptor = createDefaultSetter(propertyDescriptor);
        }

        if (!property.isVar()) {
            if (setter != null) {
                //                trace.getErrorHandler().genericError(setter.asElement().getNode(), "A 'val'-property cannot have a setter");
                trace.report(VAL_WITH_SETTER.on(setter));
            }
        }
        return setterDescriptor;
    }

    public static PropertySetterDescriptorImpl createDefaultSetter(PropertyDescriptor propertyDescriptor) {
        PropertySetterDescriptorImpl setterDescriptor;
        setterDescriptor = new PropertySetterDescriptorImpl(
                propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), propertyDescriptor.getModality(),
                propertyDescriptor.getVisibility(),
                false, true, CallableMemberDescriptor.Kind.DECLARATION);
        setterDescriptor.initializeDefault();
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
            List<AnnotationDescriptor> annotations = annotationResolver.resolveAnnotations(scope, getter.getModifierList(), trace);

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
            getterDescriptor = createDefaultGetter(propertyDescriptor);
            getterDescriptor.initialize(propertyDescriptor.getType());
        }
        return getterDescriptor;
    }

    public static PropertyGetterDescriptorImpl createDefaultGetter(PropertyDescriptor propertyDescriptor) {
        PropertyGetterDescriptorImpl getterDescriptor;
        getterDescriptor = new PropertyGetterDescriptorImpl(
                propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), propertyDescriptor.getModality(),
                propertyDescriptor.getVisibility(),
                false, true, CallableMemberDescriptor.Kind.DECLARATION);
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
                annotationResolver.resolveAnnotations(scope, modifierList, trace),
                isPrimary
        );
        trace.record(BindingContext.CONSTRUCTOR, declarationToTrace, constructorDescriptor);
        WritableScopeImpl parameterScope = new WritableScopeImpl(
                scope, constructorDescriptor, new TraceBasedRedeclarationHandler(trace), "Scope with value parameters of a constructor");
        parameterScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return constructorDescriptor.initialize(
                typeParameters,
                resolveValueParameters(
                        constructorDescriptor,
                        parameterScope,
                        valueParameters, trace),
                resolveVisibilityFromModifiers(modifierList, getDefaultConstructorVisibility(classDescriptor)),
                DescriptorUtils.isConstructorOfStaticNestedClass(constructorDescriptor));
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
                resolveVisibilityFromModifiers(parameter),
                isMutable,
                name,
                CallableMemberDescriptor.Kind.DECLARATION
        );
        propertyDescriptor.setType(type, Collections.<TypeParameterDescriptor>emptyList(),
                                   getExpectedThisObjectIfNeeded(classDescriptor), NO_RECEIVER_PARAMETER);

        PropertyGetterDescriptorImpl getter = createDefaultGetter(propertyDescriptor);
        PropertySetterDescriptor setter = propertyDescriptor.isVar() ? createDefaultSetter(propertyDescriptor) : null;

        propertyDescriptor.initialize(getter, setter);
        getter.initialize(propertyDescriptor.getType());

        trace.record(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter, propertyDescriptor);
        trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter, propertyDescriptor);
        return propertyDescriptor;
    }

    public static void checkBounds(@NotNull JetTypeReference typeReference, @NotNull JetType type, BindingTrace trace) {
        if (ErrorUtils.isErrorType(type)) return;

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

    public static SimpleFunctionDescriptor createEnumClassObjectValuesMethod(
            @NotNull ClassDescriptor classObjectDescriptor,
            BindingTrace trace
    ) {
        final ClassDescriptor enumClassDescriptor = (ClassDescriptor) classObjectDescriptor.getContainingDeclaration();
        assert enumClassDescriptor.getKind() == ClassKind.ENUM_CLASS;
        List<AnnotationDescriptor> annotations = Collections.<AnnotationDescriptor>emptyList();
        SimpleFunctionDescriptorImpl values =
                new SimpleFunctionDescriptorImpl(classObjectDescriptor, annotations,
                                                 VALUES_METHOD_NAME,
                                                 CallableMemberDescriptor.Kind.DECLARATION);
        JetType type = DeferredType.create(trace, new RecursionIntolerantLazyValue<JetType>() {
            @Override
            protected JetType compute() {
                return KotlinBuiltIns.getInstance().getArrayType(enumClassDescriptor.getDefaultType());
            }
        });
        values.initialize(null, classObjectDescriptor.getThisAsReceiverParameter(), Collections.<TypeParameterDescriptor>emptyList(),
                          Collections.<ValueParameterDescriptor>emptyList(),
                          type, Modality.FINAL,
                          Visibilities.PUBLIC, false);
        return values;
    }

    public static SimpleFunctionDescriptor createEnumClassObjectValueOfMethod(
            @NotNull ClassDescriptor classObjectDescriptor,
            BindingTrace trace
    ) {
        final ClassDescriptor enumClassDescriptor = (ClassDescriptor) classObjectDescriptor.getContainingDeclaration();
        assert enumClassDescriptor.getKind() == ClassKind.ENUM_CLASS;
        List<AnnotationDescriptor> annotations = Collections.<AnnotationDescriptor>emptyList();
        SimpleFunctionDescriptorImpl values =
                new SimpleFunctionDescriptorImpl(classObjectDescriptor, annotations,
                                                 VALUE_OF_METHOD_NAME,
                                                 CallableMemberDescriptor.Kind.DECLARATION);
        JetType type = DeferredType.create(trace, new RecursionIntolerantLazyValue<JetType>() {
            @Override
            protected JetType compute() {
                return enumClassDescriptor.getDefaultType();
            }
        });
        ValueParameterDescriptor parameterDescriptor = new ValueParameterDescriptorImpl(
                values,
                0,
                Collections.<AnnotationDescriptor>emptyList(),
                Name.identifier("value"),
                false,
                KotlinBuiltIns.getInstance().getStringType(),
                false,
                null);
        values.initialize(null, classObjectDescriptor.getThisAsReceiverParameter(),
                          Collections.<TypeParameterDescriptor>emptyList(),
                          Collections.singletonList(parameterDescriptor),
                          type, Modality.FINAL,
                          Visibilities.PUBLIC, false);
        return values;
    }

    public static ReceiverParameterDescriptor createLazyReceiverParameterDescriptor(@NotNull final ClassDescriptor classDescriptor) {
        return new AbstractReceiverParameterDescriptor() {

            private ClassReceiver value;

            @NotNull
            @Override
            public JetType getType() {
                // This must be lazy, thus the inner class
                return classDescriptor.getDefaultType();
            }

            @NotNull
            @Override
            public ReceiverValue getValue() {
                if (value == null) {
                    value = new ClassReceiver(classDescriptor);
                }
                return value;
            }

            @NotNull
            @Override
            public DeclarationDescriptor getContainingDeclaration() {
                return classDescriptor;
            }

            @Override
            public String toString() {
                return "class " + classDescriptor.getName() + "::this";
            }
        };
    }

    public static boolean checkHasOuterClassInstance(
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            @NotNull PsiElement reportErrorsOn,
            @NotNull ClassDescriptor target
    ) {
        DeclarationDescriptor descriptor = getContainingClass(scope);
        while (descriptor != null) {
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                if (isSubclass(classDescriptor, target)) {
                    return true;
                }
                if (isStaticNestedClass(classDescriptor)) {
                    trace.report(INACCESSIBLE_OUTER_CLASS_EXPRESSION.on(reportErrorsOn, classDescriptor));
                    return false;
                }
            }
            descriptor = descriptor.getContainingDeclaration();
        }
        return true;
    }
}
