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

package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.jet.lang.resolve.calls.CallExpressionResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.calls.autocasts.Nullability;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.jet.lang.resolve.calls.context.TemporaryTraceAndCache;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantChecker;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.slicedmap.WritableSlice;
import org.jetbrains.jet.utils.ThrowingList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getStaticNestedClassesScope;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantChecker.ErrorCharValueWithDiagnostic;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.*;
import static org.jetbrains.jet.lang.types.expressions.ControlStructureTypingUtils.createCallForSpecialConstruction;
import static org.jetbrains.jet.lang.types.expressions.ControlStructureTypingUtils.resolveSpecialConstructionAsCall;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.jet.lang.types.expressions.TypeReconstructionUtil.reconstructBareType;
import static org.jetbrains.jet.lexer.JetTokens.AS_KEYWORD;
import static org.jetbrains.jet.lexer.JetTokens.AS_SAFE;

@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor {

    private static final TokenSet BARE_TYPES_ALLOWED = TokenSet.create(AS_KEYWORD, AS_SAFE);

    private final PlatformToKotlinClassMap platformToKotlinClassMap;

    protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade, @NotNull PlatformToKotlinClassMap platformToKotlinClassMap) {
        super(facade);
        this.platformToKotlinClassMap = platformToKotlinClassMap;
    }

    @Override
    public JetTypeInfo visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, ExpressionTypingContext context) {
        // TODO : other members
        // TODO : type substitutions???
        CallExpressionResolver callExpressionResolver = context.expressionTypingServices.getCallExpressionResolver();
        JetTypeInfo typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, NO_RECEIVER, null, context);
        JetType type = DataFlowUtils.checkType(typeInfo.getType(), expression, context);
        ExpressionTypingUtils.checkCapturingInClosure(expression, context.trace, context.scope);
        return JetTypeInfo.create(type, typeInfo.getDataFlowInfo()); // TODO : Extensions to this
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, ExpressionTypingContext context) {
        return visitParenthesizedExpression(expression, context, false);
    }

    public JetTypeInfo visitParenthesizedExpression(JetParenthesizedExpression expression, ExpressionTypingContext context, boolean isStatement) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        return facade.getTypeInfo(innerExpression, context.replaceScope(context.scope), isStatement);
    }

    @Override
    public JetTypeInfo visitConstantExpression(@NotNull JetConstantExpression expression, ExpressionTypingContext context) {
        CompileTimeConstant<?> value = ConstantExpressionEvaluator.object$.evaluate(expression, context.trace, context.expectedType);

        if (!(value instanceof IntegerValueTypeConstant)) {
            CompileTimeConstantChecker compileTimeConstantChecker = context.getCompileTimeConstantChecker();
            boolean hasError = compileTimeConstantChecker.checkConstantExpressionType(value, expression, context.expectedType);
            if (hasError) {
                IElementType elementType = expression.getNode().getElementType();
                return JetTypeInfo.create(getDefaultType(elementType), context.dataFlowInfo);
            }
        }

        assert value != null : "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " + expression.getText();
        return createCompileTimeConstantTypeInfo(value, expression, context);
    }

    @Override
    public JetTypeInfo visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context) {
        ExpressionTypingContext contextWithNoExpectedType =
                context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetExpression left = expression.getLeft();
        JetTypeReference right = expression.getRight();
        if (right == null) {
            JetTypeInfo leftTypeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);
            return JetTypeInfo.create(null, leftTypeInfo.getDataFlowInfo());
        }

        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();

        boolean allowBareTypes = BARE_TYPES_ALLOWED.contains(operationType);
        TypeResolutionContext typeResolutionContext = new TypeResolutionContext(context.scope, context.trace, true, allowBareTypes);
        PossiblyBareType possiblyBareTarget = context.expressionTypingServices.getTypeResolver().resolvePossiblyBareType(typeResolutionContext, right);

        if (operationType == JetTokens.COLON) {
            // We do not allow bare types on static assertions, because static assertions provide an expected type for their argument,
            // thus causing a circularity in type dependencies
            assert !possiblyBareTarget.isBare() : "Bare types should not be allowed for static assertions, because argument inference makes no sense there";
            JetType targetType = possiblyBareTarget.getActualType();

            JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType.replaceExpectedType(targetType));
            checkBinaryWithTypeRHS(expression, context, targetType, typeInfo.getType());
            return DataFlowUtils.checkType(targetType, expression, context, typeInfo.getDataFlowInfo());
        }

        JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);

        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        JetType subjectType = typeInfo.getType();
        JetType targetType = reconstructBareType(right, possiblyBareTarget, subjectType, context.trace);

        if (subjectType != null) {
            checkBinaryWithTypeRHS(expression, contextWithNoExpectedType, targetType, subjectType);
            dataFlowInfo = typeInfo.getDataFlowInfo();
            if (operationType == AS_KEYWORD) {
                DataFlowValue value = DataFlowValueFactory.createDataFlowValue(left, subjectType, context.trace.getBindingContext());
                dataFlowInfo = dataFlowInfo.establishSubtyping(value, targetType);
            }
        }

        JetType result = operationType == AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        return DataFlowUtils.checkType(result, expression, context, dataFlowInfo);
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
            return;
        }
        if (operationType != JetTokens.AS_KEYWORD && operationType != JetTokens.AS_SAFE) {
            context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"));
            return;
        }
        checkForCastImpossibility(expression, actualType, targetType, context);
    }

    private void checkForCastImpossibility(
            JetBinaryExpressionWithTypeRHS expression,
            JetType actualType,
            JetType targetType,
            ExpressionTypingContext context
    ) {
        if (actualType == null || noExpectedType(targetType)) return;

        if (!CastDiagnosticsUtil.isCastPossible(actualType, targetType, platformToKotlinClassMap)) {
            context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationReference()));
        }
        else {
            JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
            // Upcast?
            if (typeChecker.isSubtypeOf(actualType, targetType)) {
                if (!typeChecker.isSubtypeOf(targetType, actualType)) {
                    // proper upcast: String as Any
                    context.trace.report(USELESS_CAST_STATIC_ASSERT_IS_FINE.on(expression.getOperationReference()));
                }
                else {
                    // cast to itself: String as String
                    context.trace.report(USELESS_CAST.on(expression.getOperationReference()));
                }
            }
            else if (CastDiagnosticsUtil.isCastErased(actualType, targetType, typeChecker)) {
                context.trace.report(Errors.UNCHECKED_CAST.on(expression, actualType, targetType));
            }
        }
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
                context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
                break;
        }
        return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
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
                    context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
                }
                return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
        }
        throw new IllegalStateException("Unknown code: " + resolutionResult.getCode());
    }

    private static JetTypeInfo errorInSuper(JetSuperExpression expression, ExpressionTypingContext context) {
        JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            context.expressionTypingServices.getTypeResolver().resolveType(context.scope, superTypeQualifier, context.trace, true);
        }
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    private static JetType checkPossiblyQualifiedSuper(
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
                    classifierCandidate = context.expressionTypingServices.getTypeResolver().resolveClass(context.scope, userType, context.trace);
                }
                else {
                    supertype = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, superTypeQualifier, context.trace, true);
                    redundantTypeArguments = userType.getTypeArgumentList();
                }
            }
            else {
                supertype = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, superTypeQualifier, context.trace, true);
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
                context.trace.report(AMBIGUOUS_SUPER.on(expression));
            }
            else {
                // supertypes may be empty when all the supertypes are error types (are not resolved, for example)
                JetType type = supertypes.isEmpty()
                               ? KotlinBuiltIns.getInstance().getAnyType()
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
            context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
            context.trace.record(BindingContext.REFERENCE_TARGET, expression.getInstanceReference(), result.getConstructor().getDeclarationDescriptor());
            if (superTypeQualifier != null) {
                context.trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, superTypeQualifier, context.scope);
            }
        }
        return result;
    }

    @NotNull // No class receivers
    private static LabelResolver.LabeledReceiverResolutionResult resolveToReceiver(
            JetLabelQualifiedInstanceExpression expression,
            ExpressionTypingContext context,
            boolean onlyClassReceivers
    ) {
        String labelName = expression.getLabelName();
        if (labelName != null) {
            LabelResolver.LabeledReceiverResolutionResult resolutionResult = context.labelResolver.resolveThisLabel(
                    expression.getInstanceReference(), expression.getTargetLabel(), context, new LabelName(labelName));
            if (onlyClassReceivers && resolutionResult.success()) {
                if (!isDeclaredInClass(resolutionResult.getReceiverParameterDescriptor())) {
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

    private static void recordThisOrSuperCallInTraceAndCallExtension(
            ExpressionTypingContext context,
            ReceiverParameterDescriptor descriptor,
            JetExpression expression
    ) {
        BindingTrace trace = context.trace;
        ResolutionCandidate<ReceiverParameterDescriptor> resolutionCandidate =
                ResolutionCandidate.create(descriptor,
                                           NO_RECEIVER,
                                           NO_RECEIVER,
                                           ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                                           false);

        Call call = CallMaker.makeCall(expression, NO_RECEIVER, null, expression, Collections.<ValueArgument>emptyList());
        ResolvedCallImpl<ReceiverParameterDescriptor> resolvedCall =
                ResolvedCallImpl.create(resolutionCandidate,
                                        TemporaryBindingTrace.create(trace, "Fake trace for fake 'this' or 'super' resolved call"),
                                        TracingStrategy.EMPTY,
                                        new DataFlowInfoForArgumentsImpl(call));
        resolvedCall.markCallAsCompleted();

        trace.record(RESOLVED_CALL, expression, resolvedCall);
        trace.record(CALL, expression, call);

        context.callResolverExtension.run(resolvedCall,
                                          BasicCallResolutionContext.create(context, call, CheckValueArgumentsMode.DISABLED));
    }

    private static boolean isDeclaredInClass(ReceiverParameterDescriptor receiver) {
        return receiver.getContainingDeclaration() instanceof ClassDescriptor;
    }

    @Override
    public JetTypeInfo visitBlockExpression(@NotNull JetBlockExpression expression, ExpressionTypingContext context) {
        return visitBlockExpression(expression, context, false);
    }

    public static JetTypeInfo visitBlockExpression(JetBlockExpression expression, ExpressionTypingContext context, boolean isStatement) {
        return context.expressionTypingServices.getBlockReturnedType(
                expression, isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION, context);
    }

    @Override
    public JetTypeInfo visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, ExpressionTypingContext context) {
        JetTypeReference typeReference = expression.getTypeReference();

        JetType receiverType =
                typeReference == null
                ? null
                : context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, false);

        JetSimpleNameExpression callableReference = expression.getCallableReference();
        if (callableReference.getReferencedName().isEmpty()) {
            context.trace.report(UNRESOLVED_REFERENCE.on(callableReference, callableReference));
            JetType errorType = ErrorUtils.createErrorType("Empty callable reference");
            return DataFlowUtils.checkType(errorType, expression, context, context.dataFlowInfo);
        }

        JetType result = getCallableReferenceType(expression, receiverType, context);
        return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
    }

    @Nullable
    private static JetType getCallableReferenceType(
            @NotNull JetCallableReferenceExpression expression,
            @Nullable JetType lhsType,
            @NotNull ExpressionTypingContext context
    ) {
        JetSimpleNameExpression reference = expression.getCallableReference();

        boolean[] result = new boolean[1];
        FunctionDescriptor descriptor = resolveCallableReferenceTarget(lhsType, context, expression, result);

        if (!result[0]) {
            context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
        }
        if (descriptor == null) return null;

        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        ReceiverParameterDescriptor expectedThisObject = descriptor.getExpectedThisObject();
        if (receiverParameter != null && expectedThisObject != null) {
            context.trace.report(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED.on(reference, descriptor));
            return null;
        }

        JetType receiverType = null;
        if (receiverParameter != null) {
            receiverType = receiverParameter.getType();
        }
        else if (expectedThisObject != null) {
            receiverType = expectedThisObject.getType();
        }

        //noinspection ConstantConditions
        JetType type = KotlinBuiltIns.getInstance().getKFunctionType(
                Collections.<AnnotationDescriptor>emptyList(),
                receiverType,
                DescriptorUtils.getValueParametersTypes(descriptor.getValueParameters()),
                descriptor.getReturnType(),
                receiverParameter != null
        );

        AnonymousFunctionDescriptor functionDescriptor = new AnonymousFunctionDescriptor(
                context.scope.getContainingDeclaration(),
                Collections.<AnnotationDescriptor>emptyList(),
                CallableMemberDescriptor.Kind.DECLARATION);

        FunctionDescriptorUtil.initializeFromFunctionType(functionDescriptor, type, null, Modality.FINAL, Visibilities.PUBLIC);

        context.trace.record(FUNCTION, expression, functionDescriptor);

        return type;
    }

    @Nullable
    private static FunctionDescriptor resolveCallableReferenceTarget(
            @Nullable JetType lhsType,
            @NotNull ExpressionTypingContext context,
            @NotNull JetCallableReferenceExpression expression,
            @NotNull boolean[] result
    ) {
        JetSimpleNameExpression reference = expression.getCallableReference();

        if (lhsType == null) {
            return resolveCallableNotCheckingArguments(reference, NO_RECEIVER, context, result);
        }

        ClassifierDescriptor classifier = lhsType.getConstructor().getDeclarationDescriptor();
        if (!(classifier instanceof ClassDescriptor)) {
            context.trace.report(CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(expression));
            return null;
        }

        ReceiverValue receiver = new TransientReceiver(lhsType);
        TemporaryTraceAndCache temporaryWithReceiver = TemporaryTraceAndCache.create(
                context, "trace to resolve callable reference with receiver", reference);
        FunctionDescriptor descriptor = resolveCallableNotCheckingArguments(
                reference, receiver, context.replaceTraceAndCache(temporaryWithReceiver), result);
        if (result[0]) {
            temporaryWithReceiver.commit();
            return descriptor;
        }

        JetScope staticScope = getStaticNestedClassesScope((ClassDescriptor) classifier);
        TemporaryTraceAndCache temporaryForStatic = TemporaryTraceAndCache.create(
                context, "trace to resolve callable reference in static scope", reference);
        FunctionDescriptor possibleStaticNestedClassConstructor = resolveCallableNotCheckingArguments(reference, NO_RECEIVER,
                context.replaceTraceAndCache(temporaryForStatic).replaceScope(staticScope), result);
        if (result[0]) {
            temporaryForStatic.commit();
            return possibleStaticNestedClassConstructor;
        }

        return null;
    }

    @Nullable
    private static FunctionDescriptor resolveCallableNotCheckingArguments(
            @NotNull JetSimpleNameExpression reference,
            @NotNull ReceiverValue receiver,
            @NotNull ExpressionTypingContext context,
            @NotNull boolean[] result
    ) {
        Call call = CallMaker.makeCall(reference, receiver, null, reference, ThrowingList.<ValueArgument>instance());

        TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace, "trace to resolve as function", reference);

        ExpressionTypingContext contextForResolve = context.replaceBindingTrace(trace).replaceExpectedType(NO_EXPECTED_TYPE);
        ResolvedCallWithTrace<FunctionDescriptor> function = contextForResolve.expressionTypingServices.getCallExpressionResolver()
                .getResolvedCallForFunction(call, reference, contextForResolve, CheckValueArgumentsMode.DISABLED, result);
        if (!result[0]) return null;

        if (function instanceof VariableAsFunctionResolvedCall) {
            // TODO: KProperty
            context.trace.report(UNSUPPORTED.on(reference, "References to variables aren't supported yet"));
            context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
            return null;
        }

        trace.commit();
        return function != null ? function.getResultingDescriptor() : null;
    }

    @Override
    public JetTypeInfo visitQualifiedExpression(@NotNull JetQualifiedExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = context.expressionTypingServices.getCallExpressionResolver();
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context);
    }

    @Override
    public JetTypeInfo visitCallExpression(@NotNull JetCallExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = context.expressionTypingServices.getCallExpressionResolver();
        return callExpressionResolver.getCallExpressionTypeInfo(expression, NO_RECEIVER, null, context);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(@NotNull JetUnaryExpression expression, ExpressionTypingContext context) {
        return visitUnaryExpression(expression, context, false);
    }

    public JetTypeInfo visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = isUnaryExpressionDependentOnExpectedType(expression)
                ? contextWithExpectedType
                : contextWithExpectedType.replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE);

        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return JetTypeInfo.create(null, context.dataFlowInfo);

        JetSimpleNameExpression operationSign = expression.getOperationReference();

        IElementType operationType = operationSign.getReferencedNameElementType();
        // If it's a labeled expression
        if (JetTokens.LABELS.contains(operationType)) {
            return visitLabeledExpression(expression, context, isStatement);
        }

        // Special case for expr!!
        if (operationType == JetTokens.EXCLEXCL) {
            return visitExclExclExpression(expression, context);
        }

        // Type check the base expression
        JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context);
        JetType type = typeInfo.getType();
        if (type == null) {
            return typeInfo;
        }
        DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();

        // Conventions for unary operations
        Name name = OperatorConventions.UNARY_OPERATION_NAMES.get(operationType);
        if (name == null) {
            context.trace.report(UNSUPPORTED.on(operationSign, "visitUnaryExpression"));
            return JetTypeInfo.create(null, dataFlowInfo);
        }

        // a[i]++/-- takes special treatment because it is actually let j = i, arr = a in arr.set(j, a.get(j).inc())
        if ((operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) && baseExpression instanceof JetArrayAccessExpression) {
            JetExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(baseExpression.getProject(), context.trace, "$e", type);
            resolveArrayAccessSetMethod((JetArrayAccessExpression) baseExpression,
                                        stubExpression,
                                        context.replaceBindingTrace(
                                                TemporaryBindingTrace.create(context.trace, "trace to resolve array access set method for unary expression", expression)),
                                        context.trace);
        }

        ExpressionReceiver receiver = new ExpressionReceiver(baseExpression, type);

        // Resolve the operation reference
        OverloadResolutionResults<FunctionDescriptor> resolutionResults = context.resolveCallWithGivenName(
                CallMaker.makeCall(receiver, expression),
                expression.getOperationReference(),
                name);

        if (!resolutionResults.isSuccess()) {
            return JetTypeInfo.create(null, dataFlowInfo);
        }

        // Computing the return type
        JetType returnType = resolutionResults.getResultingDescriptor().getReturnType();
        JetType result;
        if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
            assert returnType != null : "returnType is null for " + resolutionResults.getResultingDescriptor();
            if (JetTypeChecker.INSTANCE.isSubtypeOf(returnType, KotlinBuiltIns.getInstance().getUnitType())) {
                result = ErrorUtils.createErrorType(KotlinBuiltIns.getInstance().getUnit().getName().asString());
                context.trace.report(INC_DEC_SHOULD_NOT_RETURN_UNIT.on(operationSign));
            }
            else {
                JetType receiverType = receiver.getType();
                if (!JetTypeChecker.INSTANCE.isSubtypeOf(returnType, receiverType)) {
                    context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, name.asString(), receiverType, returnType));
                }
                else {
                    context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);

                    checkLValue(context.trace, baseExpression);
                }
                // TODO : Maybe returnType?
                result = receiverType;
            }
        }
        else {
            result = returnType;
        }

        CompileTimeConstant<?> value = ConstantExpressionEvaluator.object$.evaluate(expression, contextWithExpectedType.trace,
                                                                                    contextWithExpectedType.expectedType);
        if (value != null) {
            return createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }

        return DataFlowUtils.checkType(result, expression, contextWithExpectedType, dataFlowInfo);
    }

    @NotNull
    public static JetTypeInfo createCompileTimeConstantTypeInfo(
            @NotNull CompileTimeConstant<?> value,
            @NotNull JetExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        JetType expressionType = value.getType(KotlinBuiltIns.getInstance());
        if (value instanceof IntegerValueTypeConstant && context.contextDependency == INDEPENDENT) {
            expressionType = getPrimitiveNumberType(((IntegerValueTypeConstant) value).getValue(), context.expectedType);
            ArgumentTypeResolver.updateNumberType(expressionType, expression, context.trace);
        }

        return DataFlowUtils.checkType(expressionType, expression, context, context.dataFlowInfo);
    }

    private JetTypeInfo visitExclExclExpression(@NotNull JetUnaryExpression expression, @NotNull ExpressionTypingContext context) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        assert operationSign.getReferencedNameElementType() == JetTokens.EXCLEXCL;

        Call call = createCallForSpecialConstruction(expression, Collections.singletonList(baseExpression));
        resolveSpecialConstructionAsCall(
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
            DataFlowValue value = DataFlowValueFactory.createDataFlowValue(baseExpression, baseType, context.trace.getBindingContext());
            dataFlowInfo = dataFlowInfo.disequate(value, DataFlowValue.NULL);
        }
        return JetTypeInfo.create(TypeUtils.makeNotNullable(baseType), dataFlowInfo);
    }

    private JetTypeInfo visitLabeledExpression(@NotNull JetUnaryExpression expression, @NotNull ExpressionTypingContext context,
            boolean isStatement) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        assert JetTokens.LABELS.contains(operationSign.getReferencedNameElementType());

        String referencedName = operationSign.getReferencedName();
        context.labelResolver.enterLabeledElement(new LabelName(referencedName.substring(1)), baseExpression);
        // TODO : Some processing for the label?
        JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context, isStatement);
        context.labelResolver.exitLabeledElement(baseExpression);
        return typeInfo;
    }

    private static boolean isKnownToBeNotNull(JetExpression expression, ExpressionTypingContext context) {
        JetType type = context.trace.get(EXPRESSION_TYPE, expression);
        assert type != null : "This method is only supposed to be called when the type is not null";
        return isKnownToBeNotNull(expression, type, context);
    }

    private static boolean isKnownToBeNotNull(JetExpression expression, JetType jetType, ExpressionTypingContext context) {
        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, jetType, context.trace.getBindingContext());
        return !context.dataFlowInfo.getNullability(dataFlowValue).canBeNull();
    }

    public static void checkLValue(@NotNull BindingTrace trace, @NotNull JetExpression expression) {
        checkLValue(trace, expression, false);
    }

    private static void checkLValue(@NotNull BindingTrace trace, @NotNull JetExpression expressionWithParenthesis, boolean canBeThis) {
        JetExpression expression = JetPsiUtil.deparenthesize(expressionWithParenthesis);
        if (expression instanceof JetArrayAccessExpression) {
            JetExpression arrayExpression = ((JetArrayAccessExpression) expression).getArrayExpression();
            if (arrayExpression != null) {
                checkLValue(trace, arrayExpression, true);
            }
            return;
        }
        if (canBeThis && expression instanceof JetThisExpression) return;
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), expression, true);

        JetExpression reportOn = expression != null ? expression : expressionWithParenthesis;
        if (variable instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variable;
            if (propertyDescriptor.isSetterProjectedOut()) {
                trace.report(SETTER_PROJECTED_OUT.on(reportOn, propertyDescriptor));
            }
        }

        if (variable == null) {
            trace.report(VARIABLE_EXPECTED.on(reportOn));
        }
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
        else if (operationType == JetTokens.EQEQEQ || operationType == JetTokens.EXCLEQEQEQ) {
            ensureNonemptyIntersectionOfOperandTypes(expression, context);
            // TODO : Check comparison pointlessness
            result = JetTypeInfo.create(KotlinBuiltIns.getInstance().getBooleanType(), context.dataFlowInfo);
        }
        else if (OperatorConventions.IN_OPERATIONS.contains(operationType)) {
            result = checkInExpression(expression, operationSign, left, right, context);
        }
        else if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationType)) {
            result = visitBooleanOperationExpression(operationType, left, right, context);
        }
        else {
            context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
            result = JetTypeInfo.create(null, context.dataFlowInfo);
        }
        CompileTimeConstant<?> value = ConstantExpressionEvaluator.object$.
                evaluate(expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType);
        if (value != null) {
            return createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }
        return DataFlowUtils.checkType(result, expression, contextWithExpectedType);
    }

    private JetTypeInfo visitEquality(
            JetBinaryExpression expression,
            ExpressionTypingContext context,
            JetSimpleNameExpression operationSign,
            final JetExpression left,
            final JetExpression right
    ) {
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (right != null && left != null) {
            ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, context);

            JetTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context, facade);

            dataFlowInfo = leftTypeInfo.getDataFlowInfo();
            ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

            TemporaryBindingTrace traceInterpretingRightAsNullableAny = TemporaryBindingTrace.create(
                    context.trace, "trace to resolve 'equals(Any?)' interpreting as of type Any? an expression:" + right);
            traceInterpretingRightAsNullableAny.record(EXPRESSION_TYPE, right, KotlinBuiltIns.getInstance().getNullableAnyType());
            traceInterpretingRightAsNullableAny.record(PROCESSED, right);

            Call call = CallMaker.makeCallWithExpressions(operationSign, receiver, null, operationSign, Collections.singletonList(right));
            ExpressionTypingContext newContext = context.replaceBindingTrace(traceInterpretingRightAsNullableAny);
            OverloadResolutionResults<FunctionDescriptor> resolutionResults =
                    newContext.resolveCallWithGivenName(call, operationSign, OperatorConventions.EQUALS);

            traceInterpretingRightAsNullableAny.commit(new TraceEntryFilter() {
                @Override
                public boolean accept(@Nullable WritableSlice<?, ?> slice, Object key) {

                    // the type of the right expression isn't 'Any?' actually
                    if (key == right && (slice == EXPRESSION_TYPE || slice == PROCESSED)) return false;

                    // a hack due to KT-678
                    // without this line an autocast is reported on the receiver (if it was previously checked for not-null)
                    // with not-null check the resolution result changes from 'fun Any?.equals' to 'equals' member
                    if (key == left && slice == AUTOCAST) return false;

                    return true;
                }
            }, true);
            dataFlowInfo = facade.getTypeInfo(right, contextWithDataFlow).getDataFlowInfo();

            if (resolutionResults.isSuccess()) {
                FunctionDescriptor equals = resolutionResults.getResultingCall().getResultingDescriptor();
                if (ensureBooleanResult(operationSign, OperatorConventions.EQUALS, equals.getReturnType(), context)) {
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
        }
        return JetTypeInfo.create(KotlinBuiltIns.getInstance().getBooleanType(), dataFlowInfo);
    }

    @NotNull
    private JetTypeInfo visitComparison(
            @NotNull JetBinaryExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull JetSimpleNameExpression operationSign
    ) {
        JetTypeInfo typeInfo = getTypeInfoForBinaryCall(OperatorConventions.COMPARE_TO, context, expression);
        DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
        JetType compareToReturnType = typeInfo.getType();
        JetType type = null;
        if (compareToReturnType != null && !compareToReturnType.isError()) {
            TypeConstructor constructor = compareToReturnType.getConstructor();
            KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
            TypeConstructor intTypeConstructor = builtIns.getInt().getTypeConstructor();
            if (constructor.equals(intTypeConstructor)) {
                type = builtIns.getBooleanType();
            }
            else {
                context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
            }
        }
        return JetTypeInfo.create(type, dataFlowInfo);
    }

    @NotNull
    private JetTypeInfo visitBooleanOperationExpression(
            @Nullable IElementType operationType,
            @Nullable JetExpression left,
            @Nullable JetExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        JetType booleanType = KotlinBuiltIns.getInstance().getBooleanType();
        JetTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context.replaceExpectedType(booleanType), facade);
        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();

        WritableScopeImpl leftScope = newWritableScopeImpl(context, "Left scope of && or ||");
        // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
        boolean isAnd = operationType == JetTokens.ANDAND;
        DataFlowInfo flowInfoLeft = DataFlowUtils.extractDataFlowInfoFromCondition(left, isAnd, context).and(dataFlowInfo);
        WritableScopeImpl rightScope = isAnd ? leftScope : newWritableScopeImpl(context, "Right scope of && or ||");

        ExpressionTypingContext contextForRightExpr =
                context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope).replaceExpectedType(booleanType);
        if (right != null) {
            facade.getTypeInfo(right, contextForRightExpr);
        }
        return JetTypeInfo.create(booleanType, dataFlowInfo);
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
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }

        Call call = createCallForSpecialConstruction(expression, Lists.newArrayList(left, right));
        ResolvedCall<FunctionDescriptor> resolvedCall = resolveSpecialConstructionAsCall(
                call, "Elvis", Lists.newArrayList("left", "right"), Lists.newArrayList(true, false), contextWithExpectedType, null);
        JetTypeInfo leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.getBindingContext());
        assert leftTypeInfo != null : "Left expression was not processed: " + expression;
        JetType leftType = leftTypeInfo.getType();
        if (leftType != null && isKnownToBeNotNull(left, leftType, context)) {
            context.trace.report(USELESS_ELVIS.on(left, leftType));
        }
        JetTypeInfo rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.getBindingContext());
        assert rightTypeInfo != null : "Right expression was not processed: " + expression;
        JetType rightType = rightTypeInfo.getType();

        DataFlowInfo dataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getResultInfo();
        JetType type = resolvedCall.getResultingDescriptor().getReturnType();
        if (type == null || rightType == null) return JetTypeInfo.create(null, dataFlowInfo);

        return JetTypeInfo.create(TypeUtils.makeNullableAsSpecified(type, rightType.isNullable()), dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo checkInExpression(
            @NotNull JetElement callElement,
            @NotNull JetSimpleNameExpression operationSign,
            @Nullable JetExpression left,
            @Nullable JetExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
        if (right == null) {
            if (left != null) facade.getTypeInfo(left, contextWithNoExpectedType);
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }

        DataFlowInfo dataFlowInfo = facade.getTypeInfo(right, contextWithNoExpectedType).getDataFlowInfo();

        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        OverloadResolutionResults<FunctionDescriptor> resolutionResult = contextWithDataFlow.resolveCallWithGivenName(
                CallMaker.makeCallWithExpressions(callElement, receiver, null, operationSign, Collections.singletonList(left)),
                operationSign,
                OperatorConventions.CONTAINS);
        JetType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context.contextDependency);
        ensureBooleanResult(operationSign, OperatorConventions.CONTAINS, containsType, context);

        if (left != null) {
            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
        }

        return JetTypeInfo.create(resolutionResult.isSuccess() ? KotlinBuiltIns.getInstance().getBooleanType() : null, dataFlowInfo);
    }

    private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression, ExpressionTypingContext context) {
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
                checkSenselessComparisonWithNull(expression, left, right, context);
            }
        }
    }

    private void checkSenselessComparisonWithNull(@NotNull JetBinaryExpression expression, @NotNull JetExpression left, @NotNull JetExpression right, @NotNull ExpressionTypingContext context) {
        JetExpression expr;
        if (JetPsiUtil.isNullConstant(left)) {
            expr = right;
        }
        else if (JetPsiUtil.isNullConstant(right)) {
            expr = left;
        }
        else return;

        JetSimpleNameExpression operationSign = expression.getOperationReference();
        JetType type = facade.getTypeInfo(expr, context).getType();
        if (type == null || type.isError()) return;

        DataFlowValue value = DataFlowValueFactory.createDataFlowValue(expr, type, context.trace.getBindingContext());
        Nullability nullability = context.dataFlowInfo.getNullability(value);

        boolean expressionIsAlways;
        boolean equality = operationSign.getReferencedNameElementType() == JetTokens.EQEQ || operationSign.getReferencedNameElementType() == JetTokens.EQEQEQ;

        if (nullability == Nullability.NULL) {
            expressionIsAlways = equality;
        }
        else if (nullability == Nullability.NOT_NULL) {
            expressionIsAlways = !equality;
        }
        else return;

        context.trace.report(SENSELESS_COMPARISON.on(expression, expression, expressionIsAlways));
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
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, ExpressionTypingContext context) {
        JetTypeInfo typeInfo = resolveArrayAccessGetMethod(expression, context);
        return DataFlowUtils.checkType(typeInfo, expression, context);
    }

    @NotNull
    public JetTypeInfo getTypeInfoForBinaryCall(
            @NotNull Name name,
            @NotNull ExpressionTypingContext context,
            @NotNull JetBinaryExpression binaryExpression
    ) {
        JetExpression left = binaryExpression.getLeft();
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (left != null) {
            //left here is a receiver, so it doesn't depend on expected type
            dataFlowInfo = facade.getTypeInfo(
                    left, context.replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE)).getDataFlowInfo();
        }
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        OverloadResolutionResults<FunctionDescriptor> resolutionResults;
        if (left != null) {
            ExpressionReceiver receiver = safeGetExpressionReceiver(facade, left, context);
            resolutionResults = getResolutionResultsForBinaryCall(context.scope, name, contextWithDataFlow, binaryExpression, receiver);
        }
        else {
            resolutionResults = OverloadResolutionResultsImpl.nameNotFound();
        }

        JetExpression right = binaryExpression.getRight();
        if (right != null) {
            dataFlowInfo = facade.getTypeInfo(right, contextWithDataFlow).getDataFlowInfo();
        }

        return JetTypeInfo.create(OverloadResolutionResultsUtil.getResultingType(resolutionResults, context.contextDependency), dataFlowInfo);
    }

    @NotNull
    static
        /*package*/ OverloadResolutionResults<FunctionDescriptor> getResolutionResultsForBinaryCall(
            JetScope scope,
            Name name,
            ExpressionTypingContext context,
            JetBinaryExpression binaryExpression,
            ExpressionReceiver receiver
    ) {
//        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, binaryExpression.getLeft(), context.replaceScope(scope));
        return context.replaceScope(scope).resolveCallWithGivenName(
                CallMaker.makeCall(receiver, binaryExpression),
                binaryExpression.getOperationReference(),
                name
        );
    }

    @Override
    public JetTypeInfo visitDeclaration(@NotNull JetDeclaration dcl, ExpressionTypingContext context) {
        context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(dcl));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitRootNamespaceExpression(@NotNull JetRootNamespaceExpression expression, ExpressionTypingContext context) {
        if (JetPsiUtil.isLHSOfDot(expression)) {
            return DataFlowUtils.checkType(JetModuleUtil.getRootNamespaceType(expression), expression, context, context.dataFlowInfo);
        }
        context.trace.report(NAMESPACE_IS_NOT_AN_EXPRESSION.on(expression));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }


    @Override
    public JetTypeInfo visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, ExpressionTypingContext contextWithExpectedType) {
        final ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        final DataFlowInfo[] dataFlowInfo = new DataFlowInfo[] { context.dataFlowInfo };
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(new JetVisitorVoid() {

                @Override
                public void visitStringTemplateEntryWithExpression(@NotNull JetStringTemplateEntryWithExpression entry) {
                    JetExpression entryExpression = entry.getExpression();
                    if (entryExpression != null) {
                        JetTypeInfo typeInfo = facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(dataFlowInfo[0]));
                        dataFlowInfo[0] = typeInfo.getDataFlowInfo();
                    }
                }

                @Override
                public void visitEscapeStringTemplateEntry(@NotNull JetEscapeStringTemplateEntry entry) {
                    CompileTimeConstant<?> compileTimeConstant = CompileTimeConstantChecker.escapedStringToCharValue(entry.getText(), entry);
                    if (compileTimeConstant instanceof ErrorCharValueWithDiagnostic) {
                        context.trace.report(((ErrorCharValueWithDiagnostic) compileTimeConstant).getDiagnostic());
                    }
                }
            });
        }
        ConstantExpressionEvaluator.object$.evaluate(expression, context.trace, contextWithExpectedType.expectedType);
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getStringType(), expression, contextWithExpectedType, dataFlowInfo[0]);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, ExpressionTypingContext context) {
        return visitAnnotatedExpression(expression, context, false);
    }

    public JetTypeInfo visitAnnotatedExpression(JetAnnotatedExpression expression, ExpressionTypingContext context, boolean isStatement) {
        context.expressionTypingServices.getAnnotationResolver().resolveAnnotationsWithArguments(
                context.scope, expression.getAnnotationEntries(), context.trace);

        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) {
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        return facade.getTypeInfo(baseExpression, context, isStatement);
    }

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
        return JetTypeInfo.create(null, context.dataFlowInfo);
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
        if (arrayExpression == null) return JetTypeInfo.create(null, oldContext.dataFlowInfo);

        JetTypeInfo arrayTypeInfo = facade.getTypeInfo(arrayExpression, oldContext.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT));
        JetType arrayType = arrayTypeInfo.getType();
        if (arrayType == null) return arrayTypeInfo;

        DataFlowInfo dataFlowInfo = arrayTypeInfo.getDataFlowInfo();
        ExpressionTypingContext context = oldContext.replaceDataFlowInfo(dataFlowInfo);
        ExpressionReceiver receiver = new ExpressionReceiver(arrayExpression, arrayType);
        if (!isGet) assert rightHandSide != null;

        OverloadResolutionResults<FunctionDescriptor> functionResults = context.resolveCallWithGivenName(
                isGet
                ? CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
                : CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide, Call.CallType.ARRAY_SET_METHOD),
                arrayAccessExpression,
                Name.identifier(isGet ? "get" : "set"));

        List<JetExpression> indices = arrayAccessExpression.getIndexExpressions();
        // The accumulated data flow info of all index expressions is saved on the last index
        if (!indices.isEmpty()) {
            dataFlowInfo = facade.getTypeInfo(indices.get(indices.size() - 1), context).getDataFlowInfo();
        }

        if (!isGet) {
            dataFlowInfo = facade.getTypeInfo(rightHandSide, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo();
        }

        if (!functionResults.isSingleResult()) {
            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        traceForResolveResult.record(isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.getResultingCall());
        return JetTypeInfo.create(functionResults.getResultingDescriptor().getReturnType(), dataFlowInfo);
    }
}
