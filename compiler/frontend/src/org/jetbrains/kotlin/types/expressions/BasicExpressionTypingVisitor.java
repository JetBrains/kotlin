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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.callableReferences.CallableReferencesPackage;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;
import org.jetbrains.kotlin.types.expressions.unqualifiedSuper.UnqualifiedSuperPackage;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.JetTokens.AS_KEYWORD;
import static org.jetbrains.kotlin.lexer.JetTokens.AS_SAFE;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory.createDataFlowValue;
import static org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;
import static org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createCallForSpecialConstruction;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.kotlin.types.expressions.TypeReconstructionUtil.reconstructBareType;

@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor {

    private static final TokenSet BARE_TYPES_ALLOWED = TokenSet.create(AS_KEYWORD, AS_SAFE);

    protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, ExpressionTypingContext context) {
        // TODO : other members
        // TODO : type substitutions???
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        JetTypeInfo typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, NO_RECEIVER, null, context);
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context); // TODO : Extensions to this
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, ExpressionTypingContext context) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }
        return facade.getTypeInfo(innerExpression, context.replaceScope(context.scope));
    }

    @Override
    public JetTypeInfo visitConstantExpression(@NotNull JetConstantExpression expression, ExpressionTypingContext context) {
        CompileTimeConstant<?> compileTimeConstant = components.constantExpressionEvaluator.evaluateExpression(
                expression, context.trace, context.expectedType
        );

        if (!(compileTimeConstant instanceof IntegerValueTypeConstant)) {
            CompileTimeConstantChecker compileTimeConstantChecker = context.getCompileTimeConstantChecker();
            ConstantValue constantValue = compileTimeConstant != null ? ((TypedCompileTimeConstant) compileTimeConstant).getConstantValue() : null;
            boolean hasError = compileTimeConstantChecker.checkConstantExpressionType(constantValue, expression, context.expectedType);
            if (hasError) {
                IElementType elementType = expression.getNode().getElementType();
                return TypeInfoFactoryPackage.createTypeInfo(getDefaultType(elementType), context);
            }
        }

        assert compileTimeConstant != null : "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " + expression.getText();
        return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(compileTimeConstant, expression, context);
    }

    @NotNull
    public JetType getDefaultType(IElementType constantType) {
        KotlinBuiltIns builtIns = components.builtIns;
        if (constantType == JetNodeTypes.INTEGER_CONSTANT) {
            return builtIns.getIntType();
        }
        else if (constantType == JetNodeTypes.FLOAT_CONSTANT) {
            return builtIns.getDoubleType();
        }
        else if (constantType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return builtIns.getBooleanType();
        }
        else if (constantType == JetNodeTypes.CHARACTER_CONSTANT) {
            return builtIns.getCharType();
        }
        else if (constantType == JetNodeTypes.NULL) {
            return builtIns.getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    @Override
    public JetTypeInfo visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context) {
        ExpressionTypingContext contextWithNoExpectedType =
                context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetExpression left = expression.getLeft();
        JetTypeReference right = expression.getRight();
        if (right == null) {
            return facade.getTypeInfo(left, contextWithNoExpectedType).clearType();
        }

        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();

        boolean allowBareTypes = BARE_TYPES_ALLOWED.contains(operationType);
        TypeResolutionContext typeResolutionContext = new TypeResolutionContext(context.scope, context.trace, true, allowBareTypes);
        PossiblyBareType possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, right);

        if (operationType == JetTokens.COLON) {
            // We do not allow bare types on static assertions, because static assertions provide an expected type for their argument,
            // thus causing a circularity in type dependencies
            assert !possiblyBareTarget.isBare() : "Bare types should not be allowed for static assertions, because argument inference makes no sense there";
            JetType targetType = possiblyBareTarget.getActualType();

            JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType.replaceExpectedType(targetType));
            checkBinaryWithTypeRHS(expression, context, targetType, typeInfo.getType());
            return components.dataFlowAnalyzer.checkType(typeInfo.replaceType(targetType), expression, context);
        }

        JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);

        JetType subjectType = typeInfo.getType();
        JetType targetType = reconstructBareType(right, possiblyBareTarget, subjectType, context.trace, components.builtIns);

        if (subjectType != null) {
            checkBinaryWithTypeRHS(expression, contextWithNoExpectedType, targetType, subjectType);
            DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
            if (operationType == AS_KEYWORD) {
                DataFlowValue value = createDataFlowValue(left, subjectType, context);
                typeInfo = typeInfo.replaceDataFlowInfo(dataFlowInfo.establishSubtyping(value, targetType));
            }
        }

        JetType result = operationType == AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        return components.dataFlowAnalyzer.checkType(typeInfo.replaceType(result), expression, context);
    }

    private void checkBinaryWithTypeRHS(
            @NotNull JetBinaryExpressionWithTypeRHS expression,
            @NotNull ExpressionTypingContext context,
            @NotNull JetType targetType,
            @Nullable JetType actualType
    ) {
        if (actualType == null) return;
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        if (operationType == JetTokens.COLON) {
            context.trace.report(DEPRECATED_STATIC_ASSERT.on(expression));
            return;
        }
        if (operationType != JetTokens.AS_KEYWORD && operationType != JetTokens.AS_SAFE) {
            context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"));
            return;
        }
        checkForCastImpossibilityOrRedundancy(expression, actualType, targetType, context);
    }

    private void checkForCastImpossibilityOrRedundancy(
            JetBinaryExpressionWithTypeRHS expression,
            JetType actualType,
            JetType targetType,
            ExpressionTypingContext context
    ) {
        if (actualType == null || noExpectedType(targetType) || targetType.isError()) return;

        if (TypesPackage.isDynamic(targetType)) {
            JetTypeReference right = expression.getRight();
            assert right != null : "We know target is dynamic, but RHS is missing";
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(right));
            return;
        }

        if (!CastDiagnosticsUtil.isCastPossible(actualType, targetType, components.platformToKotlinClassMap)) {
            context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationReference()));
            return;
        }
        JetTypeChecker typeChecker = JetTypeChecker.DEFAULT;
        if (actualType.equals(targetType)) {
            // cast to itself: String as String
            context.trace.report(USELESS_CAST.on(expression));
            return;
        }
        Collection<JetType> possibleTypes = components.dataFlowAnalyzer.getAllPossibleTypes(
                expression.getLeft(), context.dataFlowInfo, actualType, context);

        boolean checkExactType = checkExactTypeForUselessCast(expression);
        for (JetType possibleType : possibleTypes) {
            boolean castIsUseless = checkExactType
                                    ? possibleType.equals(targetType)
                                    : typeChecker.isSubtypeOf(possibleType, targetType);
            if (castIsUseless) {
                context.trace.report(USELESS_CAST.on(expression));
                return;
            }
        }
        if (CastDiagnosticsUtil.isCastErased(actualType, targetType, typeChecker)) {
            context.trace.report(UNCHECKED_CAST.on(expression, actualType, targetType));
        }
    }

    // Casting an argument or a receiver to a supertype may be useful to select an exact overload of a method.
    // Casting to a supertype in other contexts is unlikely to be useful.
    private static boolean checkExactTypeForUselessCast(JetBinaryExpressionWithTypeRHS expression) {
        PsiElement parent = expression.getParent();
        while (parent instanceof JetParenthesizedExpression ||
               parent instanceof JetLabeledExpression ||
               parent instanceof JetAnnotatedExpression) {
            parent = parent.getParent();
        }
        if (parent instanceof JetValueArgument) {
            return true;
        }
        if (parent instanceof JetQualifiedExpression) {
            JetExpression receiver = ((JetQualifiedExpression) parent).getReceiverExpression();
            return PsiTreeUtil.isAncestor(receiver, expression, false);
        }
        return false;
    }

    @Override
    public JetTypeInfo visitThisExpression(@NotNull JetThisExpression expression, ExpressionTypingContext context) {
        JetType result = null;
        LabelResolver.LabeledReceiverResolutionResult resolutionResult = resolveToReceiver(expression, context, false);

        switch (resolutionResult.getCode()) {
            case LABEL_RESOLUTION_ERROR:
                // Do nothing, the error is already reported
                break;
            case NO_THIS:
                context.trace.report(NO_THIS.on(expression));
                break;
            case SUCCESS:
                result = resolutionResult.getReceiverParameterDescriptor().getType();
                context.trace.recordType(expression.getInstanceReference(), result);
                break;
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression);
    }

    @Override
    public JetTypeInfo visitSuperExpression(@NotNull JetSuperExpression expression, ExpressionTypingContext context) {
        LabelResolver.LabeledReceiverResolutionResult resolutionResult = resolveToReceiver(expression, context, true);

        if (!JetPsiUtil.isLHSOfDot(expression)) {
            context.trace.report(SUPER_IS_NOT_AN_EXPRESSION.on(expression, expression.getText()));
            return errorInSuper(expression, context);
        }

        switch (resolutionResult.getCode()) {
            case LABEL_RESOLUTION_ERROR:
                // The error is already reported
                return errorInSuper(expression, context);
            case NO_THIS:
                context.trace.report(SUPER_NOT_AVAILABLE.on(expression));
                return errorInSuper(expression, context);
            case SUCCESS:
                JetType result = checkPossiblyQualifiedSuper(expression, context, resolutionResult.getReceiverParameterDescriptor());
                if (result != null) {
                    context.trace.recordType(expression.getInstanceReference(), result);
                }
                return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression);
        }
        throw new IllegalStateException("Unknown code: " + resolutionResult.getCode());
    }

    private JetTypeInfo errorInSuper(JetSuperExpression expression, ExpressionTypingContext context) {
        JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true);
        }
        return TypeInfoFactoryPackage.noTypeInfo(context);
    }

    private JetType checkPossiblyQualifiedSuper(
            JetSuperExpression expression,
            ExpressionTypingContext context,
            ReceiverParameterDescriptor thisReceiver
    ) {
        JetType result = null;
        JetType thisType = thisReceiver.getType();
        Collection<JetType> supertypes = thisType.getConstructor().getSupertypes();
        TypeSubstitutor substitutor = TypeSubstitutor.create(thisType);

        JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            JetTypeElement typeElement = superTypeQualifier.getTypeElement();

            DeclarationDescriptor classifierCandidate = null;
            JetType supertype = null;
            PsiElement redundantTypeArguments = null;
            if (typeElement instanceof JetUserType) {
                JetUserType userType = (JetUserType) typeElement;
                // This may be just a superclass name even if the superclass is generic
                if (userType.getTypeArguments().isEmpty()) {
                    classifierCandidate = components.typeResolver.resolveClass(context.scope, userType, context.trace);
                }
                else {
                    supertype = components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true);
                    redundantTypeArguments = userType.getTypeArgumentList();
                }
            }
            else {
                supertype = components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true);
            }

            if (supertype != null) {
                if (supertypes.contains(supertype)) {
                    result = supertype;
                }
            }
            else if (classifierCandidate instanceof ClassDescriptor) {
                ClassDescriptor superclass = (ClassDescriptor) classifierCandidate;

                for (JetType declaredSupertype : supertypes) {
                    if (declaredSupertype.getConstructor().equals(superclass.getTypeConstructor())) {
                        result = substitutor.safeSubstitute(declaredSupertype, Variance.INVARIANT);
                        break;
                    }
                }
            }

            boolean validClassifier = classifierCandidate != null && !ErrorUtils.isError(classifierCandidate);
            boolean validType = supertype != null && !supertype.isError();
            if (result == null && (validClassifier || validType)) {
                context.trace.report(NOT_A_SUPERTYPE.on(superTypeQualifier));
            }
            else if (redundantTypeArguments != null) {
                context.trace.report(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.on(redundantTypeArguments));
            }
        }
        else {
            if (supertypes.size() > 1) {
                Collection<JetType> supertypesResolvedFromContext =
                        UnqualifiedSuperPackage.resolveUnqualifiedSuperFromExpressionContext(expression, supertypes);
                if (supertypesResolvedFromContext.size() == 1) {
                    JetType singleResolvedType = supertypesResolvedFromContext.iterator().next();
                    result = substitutor.substitute(singleResolvedType, Variance.INVARIANT);
                }
                else if (supertypesResolvedFromContext.isEmpty()) {
                    // No supertype found, either with concrete or abstract members.
                    // Resolve to 'Any' (this will cause diagnostics for unresolved member reference).
                    result = components.builtIns.getAnyType();
                }
                else {
                    context.trace.report(AMBIGUOUS_SUPER.on(expression));
                }
            }
            else {
                // supertypes may be empty when all the supertypes are error types (are not resolved, for example)
                JetType type = supertypes.isEmpty()
                               ? components.builtIns.getAnyType()
                               : supertypes.iterator().next();
                result = substitutor.substitute(type, Variance.INVARIANT);
            }
        }
        if (result != null) {
            if (DescriptorUtils.isTrait(thisType.getConstructor().getDeclarationDescriptor())) {
                if (DescriptorUtils.isClass(result.getConstructor().getDeclarationDescriptor())) {
                    context.trace.report(SUPERCLASS_NOT_ACCESSIBLE_FROM_TRAIT.on(expression));
                }
            }
            context.trace.recordType(expression.getInstanceReference(), result);
            context.trace.record(BindingContext.REFERENCE_TARGET, expression.getInstanceReference(), result.getConstructor().getDeclarationDescriptor());
        }
        if (superTypeQualifier != null) {
            context.trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, superTypeQualifier, context.scope);
        }
        return result;
    }

    @NotNull // No class receivers
    private LabelResolver.LabeledReceiverResolutionResult resolveToReceiver(
            JetInstanceExpressionWithLabel expression,
            ExpressionTypingContext context,
            boolean onlyClassReceivers
    ) {
        Name labelName = expression.getLabelNameAsName();
        if (labelName != null) {
            LabelResolver.LabeledReceiverResolutionResult resolutionResult =
                    LabelResolver.INSTANCE.resolveThisOrSuperLabel(expression, context, labelName);
            if (resolutionResult.success()) {
                ReceiverParameterDescriptor receiverParameterDescriptor = resolutionResult.getReceiverParameterDescriptor();
                recordThisOrSuperCallInTraceAndCallExtension(context, receiverParameterDescriptor, expression);
                if (onlyClassReceivers && !isDeclaredInClass(receiverParameterDescriptor)) {
                    return LabelResolver.LabeledReceiverResolutionResult.labelResolutionSuccess(NO_RECEIVER_PARAMETER);
                }
            }
            return resolutionResult;
        }
        else {
            ReceiverParameterDescriptor result = NO_RECEIVER_PARAMETER;
            List<ReceiverParameterDescriptor> receivers = context.scope.getImplicitReceiversHierarchy();
            if (onlyClassReceivers) {
                for (ReceiverParameterDescriptor receiver : receivers) {
                    if (isDeclaredInClass(receiver)) {
                        result = receiver;
                        break;
                    }
                }
            }
            else if (!receivers.isEmpty()) {
                result = receivers.get(0);
            }
            if (result != NO_RECEIVER_PARAMETER) {
                context.trace.record(REFERENCE_TARGET, expression.getInstanceReference(), result.getContainingDeclaration());
                recordThisOrSuperCallInTraceAndCallExtension(context, result, expression);
            }
            return LabelResolver.LabeledReceiverResolutionResult.labelResolutionSuccess(result);
        }
    }

    private void recordThisOrSuperCallInTraceAndCallExtension(
            ExpressionTypingContext context,
            ReceiverParameterDescriptor descriptor,
            JetExpression expression
    ) {
        BindingTrace trace = context.trace;
        Call call = CallMaker.makeCall(expression, NO_RECEIVER, null, expression, Collections.<ValueArgument>emptyList());
        ResolutionCandidate<ReceiverParameterDescriptor> resolutionCandidate =
                ResolutionCandidate.create(
                        call, descriptor, NO_RECEIVER, NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);

        ResolvedCallImpl<ReceiverParameterDescriptor> resolvedCall =
                ResolvedCallImpl.create(resolutionCandidate,
                                        TemporaryBindingTrace.create(trace, "Fake trace for fake 'this' or 'super' resolved call"),
                                        TracingStrategy.EMPTY,
                                        new DataFlowInfoForArgumentsImpl(call));
        resolvedCall.markCallAsCompleted();

        trace.record(RESOLVED_CALL, call, resolvedCall);
        trace.record(CALL, expression, call);

        BasicCallResolutionContext resolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_CALLABLE_TYPE);
        resolutionContext.performContextDependentCallChecks(resolvedCall);
        for (CallChecker checker : components.callCheckers) {
            checker.check(resolvedCall, resolutionContext);
        }

        components.symbolUsageValidator.validateCall(descriptor, trace, expression);
    }

    private static boolean isDeclaredInClass(ReceiverParameterDescriptor receiver) {
        return receiver.getContainingDeclaration() instanceof ClassDescriptor;
    }

    @Override
    public JetTypeInfo visitBlockExpression(@NotNull JetBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, false);
    }

    @Override
    public JetTypeInfo visitClassLiteralExpression(@NotNull JetClassLiteralExpression expression, ExpressionTypingContext c) {
        JetType type = resolveClassLiteral(expression, c);
        if (type != null && !type.isError()) {
            return components.dataFlowAnalyzer.createCheckedTypeInfo(
                    components.reflectionTypes.getKClassType(Annotations.EMPTY, type), c, expression
            );
        }

        return TypeInfoFactoryPackage.createTypeInfo(ErrorUtils.createErrorType("Unresolved class"), c);
    }

    @Nullable
    private JetType resolveClassLiteral(@NotNull JetClassLiteralExpression expression, ExpressionTypingContext c) {
        JetTypeReference typeReference = expression.getTypeReference();

        if (typeReference == null) {
            // "::class" will mean "this::class", a class of "this" instance
            c.trace.report(UNSUPPORTED.on(expression, "Class literals with empty left hand side are not yet supported"));
            return null;
        }

        TypeResolutionContext context =
                new TypeResolutionContext(c.scope, c.trace, /* checkBounds = */ false, /* allowBareTypes = */ true);
        PossiblyBareType possiblyBareType =
                components.typeResolver.resolvePossiblyBareType(context, typeReference);

        JetType type = null;
        if (possiblyBareType.isBare()) {
            if (!possiblyBareType.isNullable()) {
                ClassifierDescriptor descriptor = possiblyBareType.getBareTypeConstructor().getDeclarationDescriptor();
                if (descriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                    if (KotlinBuiltIns.isNonPrimitiveArray(classDescriptor)) {
                        context.trace.report(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT.on(expression));
                        return null;
                    }
                    type = substituteWithStarProjections(classDescriptor);
                }
            }
        }
        else {
            JetType actualType = possiblyBareType.getActualType();
            if (isAllowedInClassLiteral(actualType)) {
                type = actualType;
            }
        }

        if (type != null) {
            return type;
        }

        context.trace.report(CLASS_LITERAL_LHS_NOT_A_CLASS.on(expression));
        return null;
    }

    @NotNull
    private static JetType substituteWithStarProjections(@NotNull ClassDescriptor descriptor) {
        TypeConstructor typeConstructor = descriptor.getTypeConstructor();
        List<TypeProjection> arguments =
                KotlinPackage.map(typeConstructor.getParameters(), new Function1<TypeParameterDescriptor, TypeProjection>() {
                    @Override
                    public TypeProjection invoke(TypeParameterDescriptor descriptor) {
                        return TypeUtils.makeStarProjection(descriptor);
                    }
                });

        return JetTypeImpl.create(Annotations.EMPTY, descriptor, false, arguments);
    }

    private static boolean isAllowedInClassLiteral(@NotNull JetType type) {
        return isClassAvailableAtRuntime(type, false);
    }

    private static boolean isClassAvailableAtRuntime(@NotNull JetType type, boolean canBeNullable) {
        if (type.isMarkedNullable() && !canBeNullable) return false;

        TypeConstructor typeConstructor = type.getConstructor();
        ClassifierDescriptor typeDeclarationDescriptor = typeConstructor.getDeclarationDescriptor();

        if (typeDeclarationDescriptor instanceof ClassDescriptor) {
            List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
            if (parameters.size() != type.getArguments().size()) return false;

            Iterator<TypeProjection> typeArgumentsIterator = type.getArguments().iterator();
            for (TypeParameterDescriptor parameter : parameters) {
                if (!parameter.isReified()) return false;
                TypeProjection typeArgument = typeArgumentsIterator.next();
                if (typeArgument == null) return false;
                if (typeArgument.isStarProjection()) return false;
                if (!isClassAvailableAtRuntime(typeArgument.getType(), true)) return false;
            }

            return true;
        }
        else if (typeDeclarationDescriptor instanceof TypeParameterDescriptor) {
            return ((TypeParameterDescriptor) typeDeclarationDescriptor).isReified();
        }

        return false;
    }

    @Override
    public JetTypeInfo visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, ExpressionTypingContext c) {
        JetTypeReference typeReference = expression.getTypeReference();

        JetType receiverType =
                typeReference == null
                ? null
                : components.typeResolver.resolveType(c.scope, typeReference, c.trace, false);

        JetSimpleNameExpression callableReference = expression.getCallableReference();
        if (callableReference.getReferencedName().isEmpty()) {
            c.trace.report(UNRESOLVED_REFERENCE.on(callableReference, callableReference));
            JetType errorType = ErrorUtils.createErrorType("Empty callable reference");
            return components.dataFlowAnalyzer.createCheckedTypeInfo(errorType, c, expression);
        }

        JetType result = getCallableReferenceType(expression, receiverType, c);
        return components.dataFlowAnalyzer.createCheckedTypeInfo(result, c, expression);
    }

    @Override
    public JetTypeInfo visitObjectLiteralExpression(@NotNull final JetObjectLiteralExpression expression, final ExpressionTypingContext context) {
        final JetType[] result = new JetType[1];
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace,
                                                                            "trace to resolve object literal expression", expression);
        ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor> handler = new ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor>() {

            @Override
            public void handleRecord(WritableSlice<PsiElement, ClassDescriptor> slice, PsiElement declaration, final ClassDescriptor descriptor) {
                if (slice == CLASS && declaration == expression.getObjectDeclaration()) {
                    JetType defaultType = DeferredType.createRecursionIntolerant(components.globalContext.getStorageManager(),
                                                                                 context.trace,
                                                                                 new Function0<JetType>() {
                                                                                     @Override
                                                                                     public JetType invoke() {
                                                                                         return descriptor.getDefaultType();
                                                                                     }
                                                                                 });
                    result[0] = defaultType;
                }
            }
        };
        ObservableBindingTrace traceAdapter = new ObservableBindingTrace(temporaryTrace);
        traceAdapter.addHandler(CLASS, handler);
        components.localClassifierAnalyzer.processClassOrObject(null, // don't need to add classifier of object literal to any scope
                                                                context.replaceBindingTrace(traceAdapter).replaceContextDependency(INDEPENDENT),
                                                                context.scope.getContainingDeclaration(),
                                                                expression.getObjectDeclaration());
        temporaryTrace.commit();
        DataFlowInfo resultFlowInfo = context.dataFlowInfo;
        for (JetDelegationSpecifier specifier: expression.getObjectDeclaration().getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                JetDelegatorToSuperCall delegator = (JetDelegatorToSuperCall)specifier;
                JetTypeInfo delegatorTypeInfo = context.trace.get(EXPRESSION_TYPE_INFO, delegator.getCalleeExpression());
                if (delegatorTypeInfo != null) {
                    resultFlowInfo = resultFlowInfo.and(delegatorTypeInfo.getDataFlowInfo());
                }
            }
        }
        // Breaks are not possible inside constructor arguments, so jumpPossible or jumpFlowInfo are not necessary here
        JetTypeInfo resultTypeInfo = components.dataFlowAnalyzer.checkType(TypeInfoFactoryPackage.createTypeInfo(result[0], resultFlowInfo),
                                                                expression,
                                                                context);
        // We have to record it here,
        // otherwise ExpressionTypingVisitorDispatcher records wrong information
        context.trace.record(EXPRESSION_TYPE_INFO, expression, resultTypeInfo);
        context.trace.record(PROCESSED, expression);
        return resultTypeInfo;
    }

    @Nullable
    private JetType getCallableReferenceType(
            @NotNull JetCallableReferenceExpression expression,
            @Nullable JetType lhsType,
            @NotNull ExpressionTypingContext context
    ) {
        JetSimpleNameExpression reference = expression.getCallableReference();

        boolean[] resolved = new boolean[1];
        CallableDescriptor descriptor = CallableReferencesPackage.resolveCallableReferenceTarget(
                expression, lhsType, context, resolved, components.callResolver);
        if (!resolved[0]) {
            context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
        }
        if (descriptor == null) return null;

        return CallableReferencesPackage.createReflectionTypeForResolvedCallableReference(expression, descriptor, context, components.reflectionTypes);
    }

    @Override
    public JetTypeInfo visitQualifiedExpression(@NotNull JetQualifiedExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context);
    }

    @Override
    public JetTypeInfo visitCallExpression(@NotNull JetCallExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        return callExpressionResolver.getCallExpressionTypeInfo(expression, NO_RECEIVER, null, context);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(@NotNull JetUnaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = isUnaryExpressionDependentOnExpectedType(expression)
                ? contextWithExpectedType
                : contextWithExpectedType.replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE);

        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return TypeInfoFactoryPackage.noTypeInfo(context);

        JetSimpleNameExpression operationSign = expression.getOperationReference();

        IElementType operationType = operationSign.getReferencedNameElementType();

        // Special case for expr!!
        if (operationType == JetTokens.EXCLEXCL) {
            return visitExclExclExpression(expression, context);
        }

        // Type check the base expression
        JetTypeInfo typeInfo = facade.safeGetTypeInfo(baseExpression, context);
        JetType type = ExpressionTypingUtils.safeGetType(typeInfo);
        ExpressionReceiver receiver = new ExpressionReceiver(baseExpression, type);

        Call call = CallMaker.makeCall(receiver, expression);

        // Conventions for unary operations
        Name name = OperatorConventions.UNARY_OPERATION_NAMES.get(operationType);
        if (name == null) {
            context.trace.report(UNSUPPORTED.on(operationSign, "visitUnaryExpression"));
            return typeInfo.clearType();
        }

        // a[i]++/-- takes special treatment because it is actually let j = i, arr = a in arr.set(j, a.get(j).inc())
        if ((operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) && baseExpression instanceof JetArrayAccessExpression) {
            JetExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(baseExpression.getProject(), context.trace, "$e", type);
            TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace
                    .create(context.trace, "trace to resolve array access set method for unary expression", expression);
            ExpressionTypingContext newContext = context.replaceBindingTrace(temporaryBindingTrace);
            resolveArrayAccessSetMethod((JetArrayAccessExpression) baseExpression, stubExpression, newContext, context.trace);
        }

        // Resolve the operation reference
        OverloadResolutionResults<FunctionDescriptor> resolutionResults = components.callResolver.resolveCallWithGivenName(
                context, call, expression.getOperationReference(), name);

        if (!resolutionResults.isSuccess()) {
            return typeInfo.clearType();
        }

        // Computing the return type
        JetType returnType = resolutionResults.getResultingDescriptor().getReturnType();
        JetType result;
        if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
            assert returnType != null : "returnType is null for " + resolutionResults.getResultingDescriptor();
            if (KotlinBuiltIns.isUnit(returnType)) {
                result = ErrorUtils.createErrorType(components.builtIns.getUnit().getName().asString());
                context.trace.report(INC_DEC_SHOULD_NOT_RETURN_UNIT.on(operationSign));
            }
            else {
                JetType receiverType = receiver.getType();
                if (!JetTypeChecker.DEFAULT.isSubtypeOf(returnType, receiverType)) {
                    context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, name.asString(), receiverType, returnType));
                }
                else {
                    context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);
                    JetExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(baseExpression.getProject(), context.trace, "$e", type);
                    checkLValue(context.trace, context, baseExpression, stubExpression);
                }
                // x++ type is x type, but ++x type is x.inc() type
                DataFlowValue receiverValue = DataFlowValueFactory.createDataFlowValue(call.getExplicitReceiver(), contextWithExpectedType);
                if (expression instanceof JetPrefixExpression) {
                    result = returnType;
                }
                else {
                    result = receiverType;
                    // Also record data flow information for x++ value (= x)
                    DataFlowValue returnValue = DataFlowValueFactory.createDataFlowValue(expression, receiverType, contextWithExpectedType);
                    typeInfo = typeInfo.replaceDataFlowInfo(typeInfo.getDataFlowInfo().assign(returnValue, receiverValue));
                }
            }
        }
        else {
            result = returnType;
        }

        CompileTimeConstant<?> value = components.constantExpressionEvaluator.evaluateExpression(
                expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        );
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }

        return components.dataFlowAnalyzer.checkType(typeInfo.replaceType(result),
                                          expression,
                                          contextWithExpectedType.replaceDataFlowInfo(typeInfo.getDataFlowInfo()));
    }

    private JetTypeInfo visitExclExclExpression(@NotNull JetUnaryExpression expression, @NotNull ExpressionTypingContext context) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        assert operationSign.getReferencedNameElementType() == JetTokens.EXCLEXCL;

        // TODO: something must be done for not to lose safe call chain information here
        // See also CallExpressionResolver.getSimpleNameExpressionTypeInfo, .getQualifiedExpressionTypeInfo
        Call call = createCallForSpecialConstruction(expression, expression.getOperationReference(), Collections.singletonList(baseExpression));
        components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                call, "ExclExcl", Collections.singletonList("baseExpr"), Collections.singletonList(true), context, null);
        JetTypeInfo baseTypeInfo = BindingContextUtils.getRecordedTypeInfo(baseExpression, context.trace.getBindingContext());
        assert baseTypeInfo != null : "Base expression was not processed: " + expression;
        JetType baseType = baseTypeInfo.getType();
        if (baseType == null) {
            return baseTypeInfo;
        }
        DataFlowInfo dataFlowInfo = baseTypeInfo.getDataFlowInfo();
        if (isKnownToBeNotNull(baseExpression, context) && !baseType.isError()) {
            context.trace.report(UNNECESSARY_NOT_NULL_ASSERTION.on(operationSign, baseType));
        }
        else {
            DataFlowValue value = createDataFlowValue(baseExpression, baseType, context);
            baseTypeInfo = baseTypeInfo.replaceDataFlowInfo(dataFlowInfo.disequate(value, DataFlowValue.NULL));
        }
        JetType resultingType = TypeUtils.makeNotNullable(baseType);
        if (context.contextDependency == DEPENDENT) {
            return baseTypeInfo.replaceType(resultingType);
        }

        // The call to checkType() is only needed here to execute additionalTypeCheckers, hence the NO_EXPECTED_TYPE
        return components.dataFlowAnalyzer.checkType(baseTypeInfo.replaceType(resultingType), expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
    }

    @Override
    public JetTypeInfo visitLabeledExpression(
            @NotNull JetLabeledExpression expression, ExpressionTypingContext context
    ) {
        return visitLabeledExpression(expression, context, false);
    }

    @NotNull
    public JetTypeInfo visitLabeledExpression(
            @NotNull JetLabeledExpression expression,
            @NotNull ExpressionTypingContext context,
            boolean isStatement
    ) {
        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return TypeInfoFactoryPackage.noTypeInfo(context);

        return facade.getTypeInfo(baseExpression, context, isStatement);
    }

    private static boolean isKnownToBeNotNull(JetExpression expression, ExpressionTypingContext context) {
        JetType type = context.trace.getType(expression);
        assert type != null : "This method is only supposed to be called when the type is not null";
        return isKnownToBeNotNull(expression, type, context);
    }

    private static boolean isKnownToBeNotNull(JetExpression expression, JetType jetType, ExpressionTypingContext context) {
        DataFlowValue dataFlowValue = createDataFlowValue(expression, jetType, context);
        return !context.dataFlowInfo.getNullability(dataFlowValue).canBeNull();
    }

    /**
     * @return {@code true} iff expression can be assigned to
     */
    public boolean checkLValue(
            @NotNull BindingTrace trace,
            @NotNull ExpressionTypingContext context,
            @NotNull JetExpression expression,
            @Nullable JetExpression rightHandSide
    ) {
        return checkLValue(trace, context, expression, rightHandSide, false);
    }

    private boolean checkLValue(
            @NotNull BindingTrace trace,
            @NotNull ExpressionTypingContext context,
            @NotNull JetExpression expressionWithParenthesis,
            @Nullable JetExpression rightHandSide,
            boolean canBeThis
    ) {
        JetExpression expression = JetPsiUtil.deparenthesize(expressionWithParenthesis);
        if (expression instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) expression;
            JetExpression arrayExpression = arrayAccessExpression.getArrayExpression();
            if (arrayExpression == null || rightHandSide == null) return false;

            TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(trace, "Trace for checking set function");
            ExpressionTypingContext findSetterContext = context.replaceBindingTrace(ignoreReportsTrace);
            JetTypeInfo info = resolveArrayAccessSetMethod(arrayAccessExpression, rightHandSide, findSetterContext, ignoreReportsTrace);
            return info.getType() != null;
        }

        if (canBeThis && expression instanceof JetThisExpression) return true;
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), expression, true);

        boolean result = true;
        JetExpression reportOn = expression != null ? expression : expressionWithParenthesis;
        if (reportOn instanceof JetQualifiedExpression)
            reportOn = ((JetQualifiedExpression)reportOn).getSelectorExpression();

        if (variable instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variable;
            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (propertyDescriptor.isSetterProjectedOut()) {
                trace.report(SETTER_PROJECTED_OUT.on(reportOn, propertyDescriptor));
                result = false;
            } else {
                if (setter != null) {
                    components.symbolUsageValidator.validateCall(setter, trace, reportOn);
                }
            }
        }

        if (variable == null) {
            trace.report(VARIABLE_EXPECTED.on(reportOn));
            result = false;
        }
        else if (!variable.isVar()) {
            result = false;
        }

        return result;
    }

    @Override
    public JetTypeInfo visitBinaryExpression(@NotNull JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = isBinaryExpressionDependentOnExpectedType(expression)
                ? contextWithExpectedType
                : contextWithExpectedType.replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE);

        JetSimpleNameExpression operationSign = expression.getOperationReference();
        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();
        IElementType operationType = operationSign.getReferencedNameElementType();

        JetTypeInfo result;

        //Expressions that can depend on expected type
        if (operationType == JetTokens.IDENTIFIER) {
            Name referencedName = operationSign.getReferencedNameAsName();
            result = getTypeInfoForBinaryCall(referencedName, context, expression);
        }
        else if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            Name referencedName = OperatorConventions.BINARY_OPERATION_NAMES.get(operationType);
            result = getTypeInfoForBinaryCall(referencedName, context, expression);
        }
        else if (operationType == JetTokens.ELVIS) {
            //base expression of elvis operator is checked for 'type mismatch', so the whole expression shouldn't be checked
            return visitElvisExpression(expression, context);
        }

        //Expressions that don't depend on expected type
        else if (operationType == JetTokens.EQ) {
            result = visitAssignment(expression, context);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context);
        }
        else if (OperatorConventions.COMPARISON_OPERATIONS.contains(operationType)) {
            result = visitComparison(expression, context, operationSign);
        }
        else if (OperatorConventions.EQUALS_OPERATIONS.contains(operationType)) {
            result = visitEquality(expression, context, operationSign, left, right);
        }
        else if (OperatorConventions.IDENTITY_EQUALS_OPERATIONS.contains(operationType)) {
            context.trace.record(REFERENCE_TARGET, operationSign, components.builtIns.getIdentityEquals());
            ensureNonemptyIntersectionOfOperandTypes(expression, context);
            // TODO : Check comparison pointlessness
            result = TypeInfoFactoryPackage.createTypeInfo(components.builtIns.getBooleanType(), context);
        }
        else if (OperatorConventions.IN_OPERATIONS.contains(operationType)) {
            ValueArgument leftArgument = CallMaker.makeValueArgument(left, left != null ? left : operationSign);
            result = checkInExpression(expression, operationSign, leftArgument, right, context);
        }
        else if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationType)) {
            result = visitBooleanOperationExpression(operationType, left, right, context);
        }
        else {
            context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
            result = TypeInfoFactoryPackage.noTypeInfo(context);
        }
        CompileTimeConstant<?> value = components.constantExpressionEvaluator.evaluateExpression(
                expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        );
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }
        return components.dataFlowAnalyzer.checkType(result, expression, contextWithExpectedType);
    }

    private JetTypeInfo visitEquality(
            JetBinaryExpression expression,
            ExpressionTypingContext context,
            JetSimpleNameExpression operationSign,
            final JetExpression left,
            final JetExpression right
    ) {
        if (right == null || left == null) {
            ExpressionTypingUtils.getTypeInfoOrNullType(right, context, facade);
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade);
            return TypeInfoFactoryPackage.createTypeInfo(components.builtIns.getBooleanType(), context);
        }

        JetTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context, facade);

        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        JetTypeInfo rightTypeInfo = facade.getTypeInfo(right, contextWithDataFlow);

        TemporaryBindingTrace traceInterpretingRightAsNullableAny = TemporaryBindingTrace.create(
                context.trace, "trace to resolve 'equals(Any?)' interpreting as of type Any? an expression:", right);
        traceInterpretingRightAsNullableAny.recordType(right, components.builtIns.getNullableAnyType());

        // Nothing? has no members, and `equals()` would be unresolved on it
        JetType leftType = leftTypeInfo.getType();
        if (leftType != null && KotlinBuiltIns.isNothingOrNullableNothing(leftType)) {
            traceInterpretingRightAsNullableAny.recordType(left, components.builtIns.getNullableAnyType());
        }

        ExpressionTypingContext newContext = context.replaceBindingTrace(traceInterpretingRightAsNullableAny);
        ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, newContext);
        Call call = CallMaker.makeCallWithExpressions(
                expression,
                receiver,
                // semantically, a call to `==` is a safe call
                new JetPsiFactory(expression.getProject()).createSafeCallNode(),
                operationSign,
                Collections.singletonList(right)
        );
        OverloadResolutionResults<FunctionDescriptor> resolutionResults =
                components.callResolver.resolveCallWithGivenName(newContext, call, operationSign, OperatorConventions.EQUALS);

        traceInterpretingRightAsNullableAny.commit(new TraceEntryFilter() {
            @Override
            public boolean accept(@Nullable WritableSlice<?, ?> slice, Object key) {
                // the type of the right (and sometimes left) expression isn't 'Any?' actually
                if ((key == right || key == left) && slice == EXPRESSION_TYPE_INFO) return false;

                // a hack due to KT-678
                // without this line an smartcast is reported on the receiver (if it was previously checked for not-null)
                // with not-null check the resolution result changes from 'fun Any?.equals' to 'equals' member
                if (key == left && slice == SMARTCAST) return false;

                return true;
            }
        }, true);

        if (resolutionResults.isSuccess()) {
            FunctionDescriptor equals = resolutionResults.getResultingCall().getResultingDescriptor();
            if (ensureBooleanResult(operationSign, OperatorConventions.EQUALS, equals.getReturnType(),
                                                                     context)) {
                ensureNonemptyIntersectionOfOperandTypes(expression, context);
            }
        }
        else {
            if (resolutionResults.isAmbiguity()) {
                context.trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(operationSign, resolutionResults.getResultingCalls()));
            }
            else {
                context.trace.report(EQUALS_MISSING.on(operationSign));
            }
        }
        return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
    }

    @NotNull
    private JetTypeInfo visitComparison(
            @NotNull JetBinaryExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull JetSimpleNameExpression operationSign
    ) {
        JetTypeInfo typeInfo = getTypeInfoForBinaryCall(OperatorConventions.COMPARE_TO, context, expression);
        JetType compareToReturnType = typeInfo.getType();
        JetType type = null;
        if (compareToReturnType != null && !compareToReturnType.isError()) {
            if (JetTypeChecker.DEFAULT.equalTypes(components.builtIns.getIntType(), compareToReturnType)) {
                type = components.builtIns.getBooleanType();
            }
            else {
                context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
            }
        }
        return typeInfo.replaceType(type);
    }

    @NotNull
    private JetTypeInfo visitBooleanOperationExpression(
            @Nullable IElementType operationType,
            @Nullable JetExpression left,
            @Nullable JetExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        JetType booleanType = components.builtIns.getBooleanType();
        JetTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context.replaceExpectedType(booleanType), facade);
        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();

        WritableScopeImpl leftScope = newWritableScopeImpl(context, "Left scope of && or ||");
        // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
        boolean isAnd = operationType == JetTokens.ANDAND;
        DataFlowInfo flowInfoLeft = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(left, isAnd, context).and(dataFlowInfo);
        WritableScopeImpl rightScope = isAnd ? leftScope : newWritableScopeImpl(context, "Right scope of && or ||");

        ExpressionTypingContext contextForRightExpr =
                context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope).replaceExpectedType(booleanType);
        if (right != null) {
            facade.getTypeInfo(right, contextForRightExpr);
        }
        return leftTypeInfo.replaceType(booleanType);
    }

    @NotNull
    private JetTypeInfo visitElvisExpression(
            @NotNull JetBinaryExpression expression,
            @NotNull ExpressionTypingContext contextWithExpectedType
    ) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();

        if (left == null || right == null) {
            getTypeInfoOrNullType(left, context, facade);
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }

        Call call = createCallForSpecialConstruction(expression, expression.getOperationReference(), Lists.newArrayList(left, right));
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                call, "Elvis", Lists.newArrayList("left", "right"), Lists.newArrayList(true, false), contextWithExpectedType, null);
        JetTypeInfo leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.getBindingContext());
        assert leftTypeInfo != null : "Left expression was not processed: " + expression;
        JetType leftType = leftTypeInfo.getType();
        if (leftType != null && isKnownToBeNotNull(left, leftType, context)) {
            context.trace.report(USELESS_ELVIS.on(expression, leftType));
        }
        JetTypeInfo rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.getBindingContext());
        assert rightTypeInfo != null : "Right expression was not processed: " + expression;
        boolean loopBreakContinuePossible = leftTypeInfo.getJumpOutPossible() || rightTypeInfo.getJumpOutPossible();
        JetType rightType = rightTypeInfo.getType();

        DataFlowInfo dataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getResultInfo();
        if (leftType != null && rightType != null && KotlinBuiltIns.isNothingOrNullableNothing(rightType) && !rightType.isMarkedNullable()) {
            DataFlowValue value = createDataFlowValue(left, leftType, context);
            dataFlowInfo = dataFlowInfo.disequate(value, DataFlowValue.NULL);
        }
        JetType type = resolvedCall.getResultingDescriptor().getReturnType();
        if (type == null || rightType == null) return TypeInfoFactoryPackage.noTypeInfo(dataFlowInfo);

        // Sometimes return type for special call for elvis operator might be nullable,
        // but result is not nullable if the right type is not nullable
        if (!TypeUtils.isNullableType(rightType) && TypeUtils.isNullableType(type)) {
            type = TypeUtils.makeNotNullable(type);
        }
        if (context.contextDependency == DEPENDENT) {
            return TypeInfoFactoryPackage.createTypeInfo(type, dataFlowInfo);
        }

        // If break or continue was possible, take condition check info as the jump info
        return TypeInfoFactoryPackage.createTypeInfo(components.dataFlowAnalyzer.checkType(type, expression, contextWithExpectedType),
                                                     dataFlowInfo,
                                                     loopBreakContinuePossible,
                                                     context.dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo checkInExpression(
            @NotNull JetElement callElement,
            @NotNull JetSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable JetExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        JetExpression left = leftArgument.getArgumentExpression();
        ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
        if (right == null) {
            if (left != null) facade.getTypeInfo(left, contextWithNoExpectedType);
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }

        JetTypeInfo rightTypeInfo = facade.getTypeInfo(right, contextWithNoExpectedType);
        DataFlowInfo dataFlowInfo = rightTypeInfo.getDataFlowInfo();

        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        OverloadResolutionResults<FunctionDescriptor> resolutionResult = components.callResolver.resolveCallWithGivenName(
                contextWithDataFlow,
                CallMaker.makeCall(callElement, receiver, null, operationSign, Collections.singletonList(leftArgument)),
                operationSign,
                OperatorConventions.CONTAINS);
        JetType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context.contextDependency);
        ensureBooleanResult(operationSign, OperatorConventions.CONTAINS, containsType, context);

        if (left != null) {
            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
            rightTypeInfo = rightTypeInfo.replaceDataFlowInfo(dataFlowInfo);
        }

        if (resolutionResult.isSuccess()) {
            return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
        }
        else {
            return rightTypeInfo.clearType();
        }
    }


    private boolean ensureBooleanResult(JetExpression operationSign, Name name, JetType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    private boolean ensureBooleanResultWithCustomSubject(JetExpression operationSign, JetType resultType, String subjectName, ExpressionTypingContext context) {
        if (resultType != null) {
            // TODO : Relax?
            if (!components.builtIns.isBooleanOrSubtype(resultType)) {
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, components.builtIns.getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression, final ExpressionTypingContext context) {
        JetExpression left = expression.getLeft();
        if (left == null) return;

        JetExpression right = expression.getRight();

        // TODO : duplicated effort for == and !=
        JetType leftType = facade.getTypeInfo(left, context).getType();
        if (leftType != null && right != null) {
            JetType rightType = facade.getTypeInfo(right, context).getType();

            if (rightType != null) {
                if (TypeUtils.isIntersectionEmpty(leftType, rightType)) {
                    context.trace.report(EQUALITY_NOT_APPLICABLE.on(expression, expression.getOperationReference(), leftType, rightType));
                }
                SenselessComparisonChecker.checkSenselessComparisonWithNull(
                        expression, left, right, context,
                        new Function1<JetExpression, JetType>() {
                            @Override
                            public JetType invoke(JetExpression expression) {
                                return facade.getTypeInfo(expression, context).getType();
                            }
                        },
                        new Function1<DataFlowValue, Nullability>() {
                            @Override
                            public Nullability invoke(DataFlowValue value) {
                                return context.dataFlowInfo.getNullability(value);
                            }
                        });
            }
        }
    }

    @NotNull
    private JetTypeInfo visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    @NotNull
    private JetTypeInfo visitAssignment(JetBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    @NotNull
    private JetTypeInfo assignmentIsNotAnExpressionError(JetBinaryExpression expression, ExpressionTypingContext context) {
        facade.checkStatementType(expression, context);
        context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
        return TypeInfoFactoryPackage.noTypeInfo(context);
    }

    @Override
    public JetTypeInfo visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, ExpressionTypingContext context) {
        return components.dataFlowAnalyzer.checkType(resolveArrayAccessGetMethod(expression, context), expression, context);
    }

    @NotNull
    public JetTypeInfo getTypeInfoForBinaryCall(
            @NotNull Name name,
            @NotNull ExpressionTypingContext context,
            @NotNull JetBinaryExpression binaryExpression
    ) {
        JetExpression left = binaryExpression.getLeft();
        JetTypeInfo typeInfo;
        if (left != null) {
            //left here is a receiver, so it doesn't depend on expected type
            typeInfo = facade.getTypeInfo(left, context.replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE));
        } else {
            typeInfo = TypeInfoFactoryPackage.noTypeInfo(context);
        }
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(typeInfo.getDataFlowInfo());

        OverloadResolutionResults<FunctionDescriptor> resolutionResults;
        if (left != null) {
            ExpressionReceiver receiver = safeGetExpressionReceiver(facade, left, context);
            resolutionResults = components.callResolver.resolveBinaryCall(
                    contextWithDataFlow.replaceScope(context.scope),
                    receiver, binaryExpression, name
            );
        }
        else {
            resolutionResults = OverloadResolutionResultsImpl.nameNotFound();
        }

        if (resolutionResults.isSingleResult()) {
            typeInfo = typeInfo.replaceDataFlowInfo(resolutionResults.getResultingCall().getDataFlowInfoForArguments().getResultInfo());
        }

        return typeInfo.replaceType(OverloadResolutionResultsUtil.getResultingType(resolutionResults, context.contextDependency));
    }

    @Override
    public JetTypeInfo visitDeclaration(@NotNull JetDeclaration dcl, ExpressionTypingContext context) {
        context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(dcl));
        return TypeInfoFactoryPackage.noTypeInfo(context);
    }

    @Override
    public JetTypeInfo visitRootPackageExpression(@NotNull JetRootPackageExpression expression, ExpressionTypingContext context) {
        if (!JetPsiUtil.isLHSOfDot(expression)) {
            context.trace.report(PACKAGE_IS_NOT_AN_EXPRESSION.on(expression));
        }
        return TypeInfoFactoryPackage.noTypeInfo(context);
    }

    @Override
    public JetTypeInfo visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, ExpressionTypingContext contextWithExpectedType) {
        final ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        class StringTemplateVisitor extends JetVisitorVoid {
            private JetTypeInfo typeInfo = TypeInfoFactoryPackage.noTypeInfo(context);

            @Override
            public void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry) {
                JetExpression entryExpression = entry.getExpression();
                if (entryExpression != null) {
                    typeInfo = facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(typeInfo.getDataFlowInfo()));
                }
            }

            @Override
            public void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry) {
                CompileTimeConstantChecker.CharacterWithDiagnostic value = CompileTimeConstantChecker.escapedStringToCharacter(entry.getText(), entry);
                Diagnostic diagnostic = value.getDiagnostic();
                if (diagnostic != null) {
                    context.trace.report(diagnostic);
                }
            }
        }
        StringTemplateVisitor visitor = new StringTemplateVisitor();
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(visitor);
        }
        components.constantExpressionEvaluator.evaluateExpression(expression, context.trace, contextWithExpectedType.expectedType);
        return components.dataFlowAnalyzer.checkType(visitor.typeInfo.replaceType(components.builtIns.getStringType()),
                                          expression,
                                          contextWithExpectedType);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, ExpressionTypingContext context) {
        return visitAnnotatedExpression(expression, context, false);
    }

    public JetTypeInfo visitAnnotatedExpression(JetAnnotatedExpression expression, ExpressionTypingContext context, boolean isStatement) {
        components.annotationResolver.resolveAnnotationsWithArguments(
                context.scope, expression.getAnnotationEntries(), context.trace);

        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) {
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }
        return facade.getTypeInfo(baseExpression, context, isStatement);
    }

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
        return TypeInfoFactoryPackage.noTypeInfo(context);
    }

    @NotNull
    /*package*/ JetTypeInfo resolveArrayAccessSetMethod(@NotNull JetArrayAccessExpression arrayAccessExpression, @NotNull JetExpression rightHandSide, @NotNull ExpressionTypingContext context, @NotNull BindingTrace traceForResolveResult) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, rightHandSide, context, traceForResolveResult, false);
    }

    @NotNull
    /*package*/ JetTypeInfo resolveArrayAccessGetMethod(@NotNull JetArrayAccessExpression arrayAccessExpression, @NotNull ExpressionTypingContext context) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, null, context, context.trace, true);
    }

    @NotNull
    private JetTypeInfo resolveArrayAccessSpecialMethod(@NotNull JetArrayAccessExpression arrayAccessExpression,
                                                        @Nullable JetExpression rightHandSide, //only for 'set' method
                                                        @NotNull ExpressionTypingContext oldContext,
                                                        @NotNull BindingTrace traceForResolveResult,
                                                        boolean isGet) {
        JetExpression arrayExpression = arrayAccessExpression.getArrayExpression();
        if (arrayExpression == null) return TypeInfoFactoryPackage.noTypeInfo(oldContext);


        JetTypeInfo arrayTypeInfo = facade.safeGetTypeInfo(arrayExpression, oldContext.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT));
        JetType arrayType = ExpressionTypingUtils.safeGetType(arrayTypeInfo);

        ExpressionTypingContext context = oldContext.replaceDataFlowInfo(arrayTypeInfo.getDataFlowInfo());
        ExpressionReceiver receiver = new ExpressionReceiver(arrayExpression, arrayType);
        if (!isGet) assert rightHandSide != null;

        Call call = isGet
                    ? CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
                    : CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide, Call.CallType.ARRAY_SET_METHOD);
        OverloadResolutionResults<FunctionDescriptor> functionResults = components.callResolver.resolveCallWithGivenName(
                context, call, arrayAccessExpression, Name.identifier(isGet ? "get" : "set"));

        List<JetExpression> indices = arrayAccessExpression.getIndexExpressions();
        // The accumulated data flow info of all index expressions is saved on the last index
        JetTypeInfo resultTypeInfo = arrayTypeInfo;
        if (!indices.isEmpty()) {
            resultTypeInfo = facade.getTypeInfo(indices.get(indices.size() - 1), context);
        }

        if (!isGet) {
            resultTypeInfo = facade.getTypeInfo(rightHandSide, context);
        }

        if (!functionResults.isSingleResult()) {
            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
            return resultTypeInfo.clearType();
        }
        traceForResolveResult.record(isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression,
                                     functionResults.getResultingCall());
        return resultTypeInfo.replaceType(functionResults.getResultingDescriptor().getReturnType());
    }
}
