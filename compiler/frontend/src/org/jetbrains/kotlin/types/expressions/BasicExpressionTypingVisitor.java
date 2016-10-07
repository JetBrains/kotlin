/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext;
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
import org.jetbrains.kotlin.resolve.checkers.UnderscoreChecker;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.ResolveConstruct;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.types.expressions.unqualifiedSuper.UnqualifiedSuperKt;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.builtins.FunctionTypesKt.isExtensionFunctionType;
import static org.jetbrains.kotlin.builtins.FunctionTypesKt.isFunctionType;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory.createDataFlowValue;
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

    private static boolean isLValueOrUnsafeReceiver(@NotNull KtSimpleNameExpression expression) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expression, KtParenthesizedExpression.class);
        if (parent instanceof KtQualifiedExpression) {
            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) parent;
            // See KT-10175: receiver of unsafe call is always not-null at resolver
            // so we have to analyze its nullability here
            return qualifiedExpression.getOperationSign() == KtTokens.DOT &&
                   qualifiedExpression.getReceiverExpression() == KtPsiUtil.deparenthesize(expression);
        }
        if (parent instanceof KtBinaryExpression) {
            KtBinaryExpression binaryExpression = (KtBinaryExpression) parent;
            if (!OperatorConventions.BINARY_OPERATION_NAMES.containsKey(binaryExpression.getOperationToken()) &&
                !KtTokens.ALL_ASSIGNMENTS.contains(binaryExpression.getOperationToken())) {
                return false;
            }
            return PsiTreeUtil.isAncestor(binaryExpression.getLeft(), expression, false);
        }
        return false;
    }

    private static boolean isDangerousWithNull(@NotNull KtSimpleNameExpression expression, @NotNull ExpressionTypingContext context) {
        PsiElement parent = PsiTreeUtil.skipParentsOfType(expression, KtParenthesizedExpression.class);
        if (parent instanceof KtUnaryExpression) {
            // Unary: !! only
            KtUnaryExpression unaryExpression = (KtUnaryExpression) parent;
            return unaryExpression.getOperationToken() == KtTokens.EXCLEXCL;
        }
        if (parent instanceof KtBinaryExpressionWithTypeRHS) {
            // Binary: unsafe as only
            KtBinaryExpressionWithTypeRHS binaryExpression = (KtBinaryExpressionWithTypeRHS) parent;
            KotlinType type = context.trace.get(TYPE, binaryExpression.getRight());
            return type != null && !type.isMarkedNullable() &&
                   binaryExpression.getOperationReference().getReferencedNameElementType() == KtTokens.AS_KEYWORD;
        }
        return false;
    }

    private static void checkNull(
            @NotNull KtSimpleNameExpression expression,
            @NotNull ExpressionTypingContext context,
            @Nullable KotlinType type
    ) {
        // Receivers are normally analyzed at resolve, with an exception of KT-10175
        if (type != null && !type.isError() && !isLValueOrUnsafeReceiver(expression)) {
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, context);
            Nullability nullability = context.dataFlowInfo.getStableNullability(dataFlowValue);
            if (!nullability.canBeNonNull() && nullability.canBeNull()) {
                if (isDangerousWithNull(expression, context)) {
                    context.trace.report(ALWAYS_NULL.on(expression));
                }
                else {
                    context.trace.record(SMARTCAST_NULL, expression);
                }
            }
        }
    }

    @Override
    public KotlinTypeInfo visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, ExpressionTypingContext context) {
        // TODO : other members
        // TODO : type substitutions???
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        KotlinTypeInfo typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, null, null, context);
        checkNull(expression, context, typeInfo.getType());

        components.constantExpressionEvaluator.evaluateExpression(expression, context.trace, context.expectedType);
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context); // TODO : Extensions to this
    }

    @Override
    public KotlinTypeInfo visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, ExpressionTypingContext context) {
        KtExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        return facade.getTypeInfo(innerExpression, context.replaceScope(context.scope));
    }

    @Override
    public KotlinTypeInfo visitConstantExpression(@NotNull KtConstantExpression expression, ExpressionTypingContext context) {
        IElementType elementType = expression.getNode().getElementType();
        if (elementType == KtNodeTypes.CHARACTER_CONSTANT
            || elementType == KtNodeTypes.INTEGER_CONSTANT
            || elementType == KtNodeTypes.FLOAT_CONSTANT) {
            checkLiteralPrefixAndSuffix(expression, context);
        }

        CompileTimeConstant<?> compileTimeConstant = components.constantExpressionEvaluator.evaluateExpression(
                expression, context.trace, context.expectedType
        );

        if (!(compileTimeConstant instanceof IntegerValueTypeConstant)) {
            CompileTimeConstantChecker constantChecker = new CompileTimeConstantChecker(context, components.builtIns, false);
            ConstantValue constantValue =
                    compileTimeConstant != null ? ((TypedCompileTimeConstant) compileTimeConstant).getConstantValue() : null;
            boolean hasError = constantChecker.checkConstantExpressionType(constantValue, expression, context.expectedType);
            if (hasError) {
                return TypeInfoFactoryKt.createTypeInfo(constantValue != null ? constantValue.getType() : getDefaultType(elementType),
                                                        context);
            }
        }

        assert compileTimeConstant != null :
                "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " +
                expression.getText();
        return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(compileTimeConstant, expression, context);
    }

    @NotNull
    public KotlinType getDefaultType(IElementType constantType) {
        KotlinBuiltIns builtIns = components.builtIns;
        if (constantType == KtNodeTypes.INTEGER_CONSTANT) {
            return builtIns.getIntType();
        }
        else if (constantType == KtNodeTypes.FLOAT_CONSTANT) {
            return builtIns.getDoubleType();
        }
        else if (constantType == KtNodeTypes.BOOLEAN_CONSTANT) {
            return builtIns.getBooleanType();
        }
        else if (constantType == KtNodeTypes.CHARACTER_CONSTANT) {
            return builtIns.getCharType();
        }
        else if (constantType == KtNodeTypes.NULL) {
            return builtIns.getNullableNothingType();
        }
        else {
            throw new IllegalArgumentException("Unsupported constant type: " + constantType);
        }
    }

    @Override
    public KotlinTypeInfo visitBinaryWithTypeRHSExpression(
            @NotNull KtBinaryExpressionWithTypeRHS expression,
            ExpressionTypingContext context
    ) {
        ExpressionTypingContext contextWithNoExpectedType =
                context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        KtExpression left = expression.getLeft();
        KtTypeReference right = expression.getRight();
        if (right == null) {
            return facade.getTypeInfo(left, contextWithNoExpectedType).clearType();
        }

        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();

        boolean allowBareTypes = BARE_TYPES_ALLOWED.contains(operationType);
        TypeResolutionContext typeResolutionContext = new TypeResolutionContext(context.scope, context.trace, true, allowBareTypes, context.isDebuggerContext);
        PossiblyBareType possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, right);

        KotlinTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);

        KotlinType subjectType = typeInfo.getType();
        KotlinType targetType = reconstructBareType(right, possiblyBareTarget, subjectType, context.trace, components.builtIns);

        if (subjectType != null) {
            checkBinaryWithTypeRHS(expression, contextWithNoExpectedType, targetType, subjectType);
            DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
            if (operationType == AS_KEYWORD) {
                DataFlowValue value = createDataFlowValue(left, subjectType, context);
                typeInfo = typeInfo.replaceDataFlowInfo(dataFlowInfo.establishSubtyping(value, targetType));
            }
        }

        KotlinType result = operationType == AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        return components.dataFlowAnalyzer.checkType(typeInfo.replaceType(result), expression, context);
    }

    private void checkBinaryWithTypeRHS(
            @NotNull KtBinaryExpressionWithTypeRHS expression,
            @NotNull ExpressionTypingContext context,
            @NotNull KotlinType targetType,
            @Nullable KotlinType actualType
    ) {
        if (actualType == null) return;
        KtSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        if (operationType != KtTokens.AS_KEYWORD && operationType != KtTokens.AS_SAFE) {
            context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"));
            return;
        }
        checkForCastImpossibilityOrRedundancy(expression, actualType, targetType, context);
    }

    private void checkForCastImpossibilityOrRedundancy(
            KtBinaryExpressionWithTypeRHS expression,
            KotlinType actualType,
            KotlinType targetType,
            ExpressionTypingContext context
    ) {
        if (actualType == null || noExpectedType(targetType) || targetType.isError()) return;

        DeclarationsCheckerKt.checkNotEnumEntry(expression.getRight(), context.trace);

        if (DynamicTypesKt.isDynamic(targetType)) {
            KtTypeReference right = expression.getRight();
            assert right != null : "We know target is dynamic, but RHS is missing";
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(right));
            return;
        }

        if (!CastDiagnosticsUtil.isCastPossible(actualType, targetType, components.platformToKotlinClassMap)) {
            context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationReference()));
            return;
        }
        KotlinTypeChecker typeChecker = KotlinTypeChecker.DEFAULT;
        if (isExactTypeCast(actualType, targetType)) {
            // cast to itself: String as String
            context.trace.report(USELESS_CAST.on(expression));
            return;
        }
        Collection<KotlinType> possibleTypes = components.dataFlowAnalyzer.getAllPossibleTypes(
                expression.getLeft(), context.dataFlowInfo, actualType, context);

        boolean checkExactType = checkExactTypeForUselessCast(expression);
        for (KotlinType possibleType : possibleTypes) {
            boolean castIsUseless = checkExactType
                                    ? isExactTypeCast(possibleType, targetType)
                                    : isUpcast(possibleType, targetType, typeChecker);
            if (castIsUseless) {
                context.trace.report(USELESS_CAST.on(expression));
                return;
            }
        }
        if (CastDiagnosticsUtil.isCastErased(actualType, targetType, typeChecker)) {
            context.trace.report(UNCHECKED_CAST.on(expression, actualType, targetType));
        }
    }

    private static boolean isExactTypeCast(KotlinType candidateType, KotlinType targetType) {
        return candidateType.equals(targetType) && isExtensionFunctionType(candidateType) == isExtensionFunctionType(targetType);
    }

    private static boolean isUpcast(KotlinType candidateType, KotlinType targetType, KotlinTypeChecker typeChecker) {
        if (KotlinBuiltIns.isNullableNothing(candidateType)) return false;
        if (!typeChecker.isSubtypeOf(candidateType, targetType)) return false;

        if (isFunctionType(candidateType) && isFunctionType(targetType)) {
            return isExtensionFunctionType(candidateType) == isExtensionFunctionType(targetType);
        }

        return true;
    }

    // Casting an argument or a receiver to a supertype may be useful to select an exact overload of a method.
    // Casting to a supertype in other contexts is unlikely to be useful.
    private static boolean checkExactTypeForUselessCast(KtBinaryExpressionWithTypeRHS expression) {
        PsiElement parent = expression.getParent();
        while (parent instanceof KtParenthesizedExpression ||
               parent instanceof KtLabeledExpression ||
               parent instanceof KtAnnotatedExpression) {
            parent = parent.getParent();
        }
        if (parent instanceof KtValueArgument) {
            return true;
        }
        if (parent instanceof KtQualifiedExpression) {
            KtExpression receiver = ((KtQualifiedExpression) parent).getReceiverExpression();
            return PsiTreeUtil.isAncestor(receiver, expression, false);
        }
        // in binary expression, left argument can be a receiver and right an argument
        // in unary expression, left argument can be a receiver
        if (parent instanceof KtBinaryExpression || parent instanceof KtUnaryExpression) {
            return true;
        }
        return false;
    }

    @Override
    public KotlinTypeInfo visitThisExpression(@NotNull KtThisExpression expression, ExpressionTypingContext context) {
        KotlinType result = null;
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
    public KotlinTypeInfo visitSuperExpression(@NotNull KtSuperExpression expression, ExpressionTypingContext context) {
        LabelResolver.LabeledReceiverResolutionResult resolutionResult = resolveToReceiver(expression, context, true);

        if (!KtPsiUtil.isLHSOfDot(expression)) {
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
                KotlinType result = checkPossiblyQualifiedSuper(expression, context, resolutionResult.getReceiverParameterDescriptor());
                if (result != null) {
                    context.trace.recordType(expression.getInstanceReference(), result);
                }
                return components.dataFlowAnalyzer.createCheckedTypeInfo(result, context, expression);
        }
        throw new IllegalStateException("Unknown code: " + resolutionResult.getCode());
    }

    private KotlinTypeInfo errorInSuper(KtSuperExpression expression, ExpressionTypingContext context) {
        KtTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true);
        }
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    private KotlinType checkPossiblyQualifiedSuper(
            KtSuperExpression expression,
            ExpressionTypingContext context,
            ReceiverParameterDescriptor thisReceiver
    ) {
        KotlinType result = null;
        KotlinType thisType = thisReceiver.getType();
        Collection<KotlinType> supertypes = thisType.getConstructor().getSupertypes();
        TypeSubstitutor substitutor = TypeSubstitutor.create(thisType);

        KtTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            KtTypeElement typeElement = superTypeQualifier.getTypeElement();

            DeclarationDescriptor classifierCandidate = null;
            KotlinType supertype = null;
            PsiElement redundantTypeArguments = null;
            if (typeElement instanceof KtUserType) {
                KtUserType userType = (KtUserType) typeElement;
                // This may be just a superclass name even if the superclass is generic
                if (userType.getTypeArguments().isEmpty()) {
                    classifierCandidate = components.typeResolver.resolveClass(context.scope, userType, context.trace, context.isDebuggerContext);
                }
                else {
                    supertype = components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true);
                    redundantTypeArguments = userType.getTypeArgumentList();
                }
            }
            else {
                supertype = components.typeResolver.resolveType(context.scope, superTypeQualifier, context.trace, true);
            }

            if (classifierCandidate instanceof TypeAliasDescriptor) {
                classifierCandidate = ((TypeAliasDescriptor) classifierCandidate).getClassDescriptor();
            }

            if (supertype != null) {
                if (supertypes.contains(supertype)) {
                    result = supertype;
                }
            }
            else if (classifierCandidate instanceof ClassDescriptor) {
                ClassDescriptor superclass = (ClassDescriptor) classifierCandidate;

                for (KotlinType declaredSupertype : supertypes) {
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

            if (result != null && (validClassifier || validType)) {
                checkResolvedExplicitlyQualifiedSupertype(context.trace, result, supertypes, superTypeQualifier);
            }
        }
        else {
            if (UnqualifiedSuperKt.isPossiblyAmbiguousUnqualifiedSuper(expression, supertypes)) {
                Collection<KotlinType> supertypesResolvedFromContext =
                        UnqualifiedSuperKt.resolveUnqualifiedSuperFromExpressionContext(
                                expression, supertypes, components.builtIns.getAnyType());
                if (supertypesResolvedFromContext.size() == 1) {
                    KotlinType singleResolvedType = supertypesResolvedFromContext.iterator().next();
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
                KotlinType type = supertypes.isEmpty()
                                  ? components.builtIns.getAnyType()
                                  : supertypes.iterator().next();
                result = substitutor.substitute(type, Variance.INVARIANT);
            }
        }
        if (result != null) {
            if (DescriptorUtils.isInterface(thisType.getConstructor().getDeclarationDescriptor())) {
                if (DescriptorUtils.isClass(result.getConstructor().getDeclarationDescriptor())) {
                    context.trace.report(SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE.on(expression));
                }
            }
            context.trace.recordType(expression.getInstanceReference(), result);
            context.trace.record(BindingContext.REFERENCE_TARGET, expression.getInstanceReference(),
                                 result.getConstructor().getDeclarationDescriptor());
            context.trace.record(THIS_TYPE_FOR_SUPER_EXPRESSION, expression, thisType);
        }

        BindingContextUtilsKt.recordScope(context.trace, context.scope, superTypeQualifier);
        return result;
    }

    private static void checkResolvedExplicitlyQualifiedSupertype(
            @NotNull BindingTrace trace,
            @NotNull KotlinType result,
            @NotNull Collection<KotlinType> supertypes,
            @NotNull KtTypeReference superTypeQualifier
    ) {
        if (supertypes.size() > 1) {
            ClassifierDescriptor resultClassifierDescriptor = result.getConstructor().getDeclarationDescriptor();
            for (KotlinType otherSupertype : supertypes) {
                ClassifierDescriptor otherSupertypeClassifierDescriptor = otherSupertype.getConstructor().getDeclarationDescriptor();
                if (otherSupertypeClassifierDescriptor == resultClassifierDescriptor) {
                    continue;
                }
                if (KotlinTypeChecker.DEFAULT.isSubtypeOf(otherSupertype, result)) {
                    trace.report(QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE.on(superTypeQualifier, otherSupertype));
                    break;
                }
            }
        }
    }

    @NotNull // No class receivers
    private LabelResolver.LabeledReceiverResolutionResult resolveToReceiver(
            KtInstanceExpressionWithLabel expression,
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
                    return LabelResolver.LabeledReceiverResolutionResult.labelResolutionSuccess(null);
                }
            }
            return resolutionResult;
        }
        else {
            ReceiverParameterDescriptor result = null;
            List<ReceiverParameterDescriptor> receivers = ScopeUtilsKt.getImplicitReceiversHierarchy(context.scope);
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
            if (result != null) {
                context.trace.record(REFERENCE_TARGET, expression.getInstanceReference(), result.getContainingDeclaration());
                recordThisOrSuperCallInTraceAndCallExtension(context, result, expression);
            }
            return LabelResolver.LabeledReceiverResolutionResult.labelResolutionSuccess(result);
        }
    }

    private void recordThisOrSuperCallInTraceAndCallExtension(
            ExpressionTypingContext context,
            ReceiverParameterDescriptor descriptor,
            KtExpression expression
    ) {
        BindingTrace trace = context.trace;
        Call call = CallMaker.makeCall(expression, null, null, expression, Collections.<ValueArgument>emptyList());
        ResolutionCandidate<ReceiverParameterDescriptor> resolutionCandidate =
                ResolutionCandidate.create(
                        call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);

        ResolvedCallImpl<ReceiverParameterDescriptor> resolvedCall =
                ResolvedCallImpl.create(resolutionCandidate,
                                        TemporaryBindingTrace.create(trace, "Fake trace for fake 'this' or 'super' resolved call"),
                                        TracingStrategy.EMPTY,
                                        new DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call));
        resolvedCall.markCallAsCompleted();

        trace.record(RESOLVED_CALL, call, resolvedCall);
        trace.record(CALL, expression, call);

        if (context.trace.wantsDiagnostics()) {
            CallCheckerContext callCheckerContext = new CallCheckerContext(context, components.languageVersionSettings);
            for (CallChecker checker : components.callCheckers) {
                checker.check(resolvedCall, expression, callCheckerContext);
            }
        }
    }

    private static boolean isDeclaredInClass(ReceiverParameterDescriptor receiver) {
        return receiver.getContainingDeclaration() instanceof ClassDescriptor;
    }

    @Override
    public KotlinTypeInfo visitBlockExpression(@NotNull KtBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, false);
    }

    @Override
    public KotlinTypeInfo visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression, ExpressionTypingContext c) {
        return components.doubleColonExpressionResolver.visitClassLiteralExpression(expression, c);
    }

    @Override
    public KotlinTypeInfo visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, ExpressionTypingContext c) {
        return components.doubleColonExpressionResolver.visitCallableReferenceExpression(expression, c);
    }

    @Override
    public KotlinTypeInfo visitObjectLiteralExpression(
            @NotNull final KtObjectLiteralExpression expression,
            final ExpressionTypingContext context
    ) {
        final KotlinType[] result = new KotlinType[1];
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace,
                                                                            "trace to resolve object literal expression", expression);
        ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor> handler =
                new ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor>() {

                    @Override
                    public void handleRecord(
                            WritableSlice<PsiElement, ClassDescriptor> slice,
                            PsiElement declaration,
                            final ClassDescriptor descriptor
                    ) {
                        if (slice == CLASS && declaration == expression.getObjectDeclaration()) {
                            KotlinType defaultType = DeferredType.createRecursionIntolerant(components.globalContext.getStorageManager(),
                                                                                            context.trace,
                                                                                            new Function0<KotlinType>() {
                                                                                                @Override
                                                                                                public KotlinType invoke() {
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
                                                                context.replaceBindingTrace(traceAdapter)
                                                                       .replaceContextDependency(INDEPENDENT),
                                                                context.scope.getOwnerDescriptor(),
                                                                expression.getObjectDeclaration());
        temporaryTrace.commit();
        DataFlowInfo resultFlowInfo = context.dataFlowInfo;
        for (KtSuperTypeListEntry specifier : expression.getObjectDeclaration().getSuperTypeListEntries()) {
            if (specifier instanceof KtSuperTypeCallEntry) {
                KtSuperTypeCallEntry delegator = (KtSuperTypeCallEntry) specifier;
                KotlinTypeInfo delegatorTypeInfo = context.trace.get(EXPRESSION_TYPE_INFO, delegator.getCalleeExpression());
                if (delegatorTypeInfo != null) {
                    resultFlowInfo = resultFlowInfo.and(delegatorTypeInfo.getDataFlowInfo());
                }
            }
        }
        // Breaks are not possible inside constructor arguments, so jumpPossible or jumpFlowInfo are not necessary here
        KotlinTypeInfo resultTypeInfo = components.dataFlowAnalyzer.checkType(TypeInfoFactoryKt.createTypeInfo(result[0], resultFlowInfo),
                                                                              expression,
                                                                              context);
        // We have to record it here,
        // otherwise ExpressionTypingVisitorDispatcher records wrong information
        context.trace.record(EXPRESSION_TYPE_INFO, expression, resultTypeInfo);
        context.trace.record(PROCESSED, expression);
        return resultTypeInfo;
    }

    @Override
    public KotlinTypeInfo visitQualifiedExpression(@NotNull KtQualifiedExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context);
    }

    @Override
    public KotlinTypeInfo visitCallExpression(@NotNull KtCallExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        return callExpressionResolver.getCallExpressionTypeInfo(expression, null, null, context);
    }

    @Override
    public KotlinTypeInfo visitUnaryExpression(@NotNull KtUnaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = isUnaryExpressionDependentOnExpectedType(expression)
                                          ? contextWithExpectedType
                                          : contextWithExpectedType.replaceContextDependency(INDEPENDENT)
                                                                   .replaceExpectedType(NO_EXPECTED_TYPE);

        KtExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return TypeInfoFactoryKt.noTypeInfo(context);

        KtSimpleNameExpression operationSign = expression.getOperationReference();

        IElementType operationType = operationSign.getReferencedNameElementType();

        // Special case for expr!!
        if (operationType == KtTokens.EXCLEXCL) {
            return visitExclExclExpression(expression, context);
        }

        // Type check the base expression
        KotlinTypeInfo typeInfo = facade.safeGetTypeInfo(baseExpression, context);
        KotlinType type = ExpressionTypingUtils.safeGetType(typeInfo);
        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(baseExpression, type, context.trace.getBindingContext());

        Call call = CallMaker.makeCall(receiver, expression);

        // Conventions for unary operations
        Name name = OperatorConventions.UNARY_OPERATION_NAMES.get(operationType);
        if (name == null) {
            context.trace.report(UNSUPPORTED.on(operationSign, "visitUnaryExpression"));
            return typeInfo.clearType();
        }

        // a[i]++/-- takes special treatment because it is actually let j = i, arr = a in arr.set(j, a.get(j).inc())
        if ((operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS) &&
            baseExpression instanceof KtArrayAccessExpression) {
            KtExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(
                    baseExpression.getProject(), context.trace, "e", type);
            TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(
                    context.trace, "trace to resolve array access set method for unary expression", expression);
            ExpressionTypingContext newContext = context.replaceBindingTrace(temporaryBindingTrace);
            resolveImplicitArrayAccessSetMethod((KtArrayAccessExpression) baseExpression, stubExpression, newContext, context.trace);
        }

        // Resolve the operation reference
        OverloadResolutionResults<FunctionDescriptor> resolutionResults = components.callResolver.resolveCallWithGivenName(
                context, call, expression.getOperationReference(), name);

        if (!resolutionResults.isSuccess()) {
            return typeInfo.clearType();
        }

        // Computing the return type
        KotlinType returnType = resolutionResults.getResultingDescriptor().getReturnType();
        KotlinType result;
        if (operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS) {
            assert returnType != null : "returnType is null for " + resolutionResults.getResultingDescriptor();
            if (KotlinBuiltIns.isUnit(returnType)) {
                result = ErrorUtils.createErrorType(components.builtIns.getUnit().getName().asString());
                context.trace.report(INC_DEC_SHOULD_NOT_RETURN_UNIT.on(operationSign));
            }
            else {
                KotlinType receiverType = receiver.getType();
                if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(returnType, receiverType)) {
                    context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, name.asString(), receiverType, returnType));
                }
                else {
                    context.trace.record(BindingContext.VARIABLE_REASSIGNMENT, expression);
                    KtExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(
                            baseExpression.getProject(), context.trace, "e", type);
                    checkLValue(context.trace, context, baseExpression, stubExpression, expression);
                }
                // x++ type is x type, but ++x type is x.inc() type
                DataFlowValue receiverValue = DataFlowValueFactory.createDataFlowValue(
                        (ReceiverValue) call.getExplicitReceiver(), contextWithExpectedType);
                if (expression instanceof KtPrefixExpression) {
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

    private KotlinTypeInfo visitExclExclExpression(@NotNull KtUnaryExpression expression, @NotNull ExpressionTypingContext context) {
        KtExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        KtSimpleNameExpression operationSign = expression.getOperationReference();
        assert operationSign.getReferencedNameElementType() == KtTokens.EXCLEXCL;

        // TODO: something must be done for not to lose safe call chain information here
        // See also CallExpressionResolver.getSimpleNameExpressionTypeInfo, .getQualifiedExpressionTypeInfo
        Call call = createCallForSpecialConstruction(
                expression, expression.getOperationReference(), Collections.singletonList(baseExpression));
        components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                call, ResolveConstruct.EXCL_EXCL, Collections.singletonList("baseExpr"), Collections.singletonList(true), context, null);
        KotlinTypeInfo baseTypeInfo = BindingContextUtils.getRecordedTypeInfo(baseExpression, context.trace.getBindingContext());

        if (ArgumentTypeResolver.isFunctionLiteralArgument(baseExpression, context)) {
            context.trace.report(NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION.on(operationSign));
            if (baseTypeInfo == null) {
                return TypeInfoFactoryKt.createTypeInfo(ErrorUtils.createErrorType("Unresolved lambda expression"), context);
            }
            return baseTypeInfo;
        }
        assert baseTypeInfo != null : "Base expression was not processed: " + expression;
        KotlinType baseType = baseTypeInfo.getType();
        if (baseType == null) {
            return baseTypeInfo;
        }
        DataFlowInfo dataFlowInfo = baseTypeInfo.getDataFlowInfo();
        if (isKnownToBeNotNull(baseExpression, context) && !baseType.isError()) {
            context.trace.report(UNNECESSARY_NOT_NULL_ASSERTION.on(operationSign, TypeUtils.makeNotNullable(baseType)));
        }
        else {
            DataFlowValue value = createDataFlowValue(baseExpression, baseType, context);
            baseTypeInfo = baseTypeInfo.replaceDataFlowInfo(dataFlowInfo.disequate(value, DataFlowValue.nullValue(components.builtIns)));
        }
        KotlinType resultingType = TypeUtils.makeNotNullable(baseType);
        if (context.contextDependency == DEPENDENT) {
            return baseTypeInfo.replaceType(resultingType);
        }

        // The call to checkType() is only needed here to execute additionalTypeCheckers, hence the NO_EXPECTED_TYPE
        return components.dataFlowAnalyzer.checkType(
                baseTypeInfo.replaceType(resultingType), expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
    }

    @Override
    public KotlinTypeInfo visitLabeledExpression(
            @NotNull KtLabeledExpression expression, ExpressionTypingContext context
    ) {
        return visitLabeledExpression(expression, context, false);
    }

    @NotNull
    public KotlinTypeInfo visitLabeledExpression(
            @NotNull KtLabeledExpression expression,
            @NotNull ExpressionTypingContext context,
            boolean isStatement
    ) {
        KtSimpleNameExpression labelExpression = expression.getTargetLabel();
        if (labelExpression != null) {
            PsiElement labelIdentifier = labelExpression.getIdentifier();
            UnderscoreChecker.INSTANCE.checkIdentifier(labelIdentifier, context.trace);
        }
        KtExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return TypeInfoFactoryKt.noTypeInfo(context);

        return facade.getTypeInfo(baseExpression, context, isStatement);
    }

    private static boolean isKnownToBeNotNull(KtExpression expression, ExpressionTypingContext context) {
        KotlinType type = context.trace.getType(expression);
        assert type != null : "This method is only supposed to be called when the type is not null";
        return isKnownToBeNotNull(expression, type, context);
    }

    private static boolean isKnownToBeNotNull(KtExpression expression, KotlinType jetType, ExpressionTypingContext context) {
        DataFlowValue dataFlowValue = createDataFlowValue(expression, jetType, context);
        return !context.dataFlowInfo.getStableNullability(dataFlowValue).canBeNull();
    }

    /**
     * @return {@code true} iff expression can be assigned to
     */
    public boolean checkLValue(
            @NotNull BindingTrace trace,
            @NotNull ExpressionTypingContext context,
            @NotNull KtExpression expressionWithParenthesis,
            @Nullable KtExpression rightHandSide,
            @NotNull KtOperationExpression operationExpression
    ) {
        KtExpression expression = KtPsiUtil.deparenthesize(expressionWithParenthesis);
        if (expression instanceof KtArrayAccessExpression) {
            KtArrayAccessExpression arrayAccessExpression = (KtArrayAccessExpression) expression;
            KtExpression arrayExpression = arrayAccessExpression.getArrayExpression();
            if (arrayExpression == null || rightHandSide == null) return false;

            TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(trace, "Trace for checking set function");
            ExpressionTypingContext findSetterContext = context.replaceBindingTrace(ignoreReportsTrace);
            KotlinTypeInfo info = resolveArrayAccessSetMethod(arrayAccessExpression, rightHandSide, findSetterContext, ignoreReportsTrace);

            IElementType operationType = operationExpression.getOperationReference().getReferencedNameElementType();
            if (KtTokens.AUGMENTED_ASSIGNMENTS.contains(operationType)
                    || operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS) {
                ResolvedCall<?> resolvedCall = ignoreReportsTrace.get(INDEXED_LVALUE_SET, expression);
                if (resolvedCall != null && trace.wantsDiagnostics()) {
                    // Call must be validated with the actual, not temporary trace in order to report operator diagnostic
                    // Only unary assignment expressions (++, --) and +=/... must be checked, normal assignments have the proper trace
                    CallCheckerContext callCheckerContext = new CallCheckerContext(context, trace, components.languageVersionSettings);
                    for (CallChecker checker : components.callCheckers) {
                        checker.check(resolvedCall, expression, callCheckerContext);
                    }
                }
            }

            return info.getType() != null;
        }

        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorFromReference(trace.getBindingContext(), expression);

        boolean result = true;
        KtExpression reportOn = expression != null ? expression : expressionWithParenthesis;
        if (reportOn instanceof KtQualifiedExpression) {
            KtExpression selector = ((KtQualifiedExpression) reportOn).getSelectorExpression();
            if (selector != null)
                reportOn = selector;
        }

        if (variable instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variable;
            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (propertyDescriptor.isSetterProjectedOut()) {
                trace.report(SETTER_PROJECTED_OUT.on(reportOn, propertyDescriptor));
                result = false;
            }
            else if (setter != null) {
                ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expressionWithParenthesis, context.trace.getBindingContext());
                assert resolvedCall != null
                        : "Call is not resolved for property setter: " + PsiUtilsKt.getElementTextWithContext(expressionWithParenthesis);
                checkPropertySetterCall(context.replaceBindingTrace(trace), setter, resolvedCall, reportOn);
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

    private void checkPropertySetterCall(
            @NotNull ExpressionTypingContext context,
            @NotNull PropertySetterDescriptor descriptor,
            @NotNull ResolvedCall<?> propertyResolvedCall,
            @NotNull KtExpression expression
    ) {
        Call call = propertyResolvedCall.getCall();

        ResolutionCandidate<PropertySetterDescriptor> resolutionCandidate = ResolutionCandidate.create(
                call, descriptor, propertyResolvedCall.getDispatchReceiver(), propertyResolvedCall.getExplicitReceiverKind(), null
        );

        ResolvedCallImpl<PropertySetterDescriptor> resolvedCall = ResolvedCallImpl.create(
                resolutionCandidate,
                TemporaryBindingTrace.create(context.trace, "Trace for fake property setter resolved call"),
                TracingStrategy.EMPTY,
                new DataFlowInfoForArgumentsImpl(propertyResolvedCall.getDataFlowInfoForArguments().getResultInfo(), call)
        );
        resolvedCall.markCallAsCompleted();

        if (context.trace.wantsDiagnostics()) {
            CallCheckerContext callCheckerContext = new CallCheckerContext(context, components.languageVersionSettings);
            for (CallChecker checker : components.callCheckers) {
                checker.check(resolvedCall, expression, callCheckerContext);
            }
        }
    }

    @Override
    public KotlinTypeInfo visitBinaryExpression(@NotNull KtBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = isBinaryExpressionDependentOnExpectedType(expression)
                                          ? contextWithExpectedType
                                          : contextWithExpectedType.replaceContextDependency(INDEPENDENT)
                                                                   .replaceExpectedType(NO_EXPECTED_TYPE);

        KtSimpleNameExpression operationSign = expression.getOperationReference();
        KtExpression left = expression.getLeft();
        KtExpression right = expression.getRight();
        IElementType operationType = operationSign.getReferencedNameElementType();

        KotlinTypeInfo result;

        //Expressions that can depend on expected type
        if (operationType == KtTokens.IDENTIFIER) {
            Name referencedName = operationSign.getReferencedNameAsName();
            result = getTypeInfoForBinaryCall(referencedName, context, expression);
        }
        else if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            Name referencedName = OperatorConventions.BINARY_OPERATION_NAMES.get(operationType);
            result = getTypeInfoForBinaryCall(referencedName, context, expression);
        }
        else if (operationType == KtTokens.ELVIS) {
            //base expression of elvis operator is checked for 'type mismatch', so the whole expression shouldn't be checked
            return visitElvisExpression(expression, context);
        }

        //Expressions that don't depend on expected type
        else if (operationType == KtTokens.EQ) {
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
            ensureNonemptyIntersectionOfOperandTypes(expression, context);
            // TODO : Check comparison pointlessness
            result = TypeInfoFactoryKt.createTypeInfo(components.builtIns.getBooleanType(), context);
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
            result = TypeInfoFactoryKt.noTypeInfo(context);
        }
        CompileTimeConstant<?> value = components.constantExpressionEvaluator.evaluateExpression(
                expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType
        );
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }
        return components.dataFlowAnalyzer.checkType(result, expression, contextWithExpectedType);
    }

    private KotlinTypeInfo visitEquality(
            KtBinaryExpression expression,
            ExpressionTypingContext context,
            KtSimpleNameExpression operationSign,
            final KtExpression left,
            final KtExpression right
    ) {
        if (right == null || left == null) {
            ExpressionTypingUtils.getTypeInfoOrNullType(right, context, facade);
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade);
            return TypeInfoFactoryKt.createTypeInfo(components.builtIns.getBooleanType(), context);
        }

        KotlinTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context, facade);

        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        KotlinTypeInfo rightTypeInfo = facade.getTypeInfo(right, contextWithDataFlow);

        TemporaryBindingTrace traceInterpretingRightAsNullableAny = TemporaryBindingTrace.create(
                context.trace, "trace to resolve 'equals(Any?)' interpreting as of type Any? an expression:", right);
        traceInterpretingRightAsNullableAny.recordType(right, components.builtIns.getNullableAnyType());

        // Nothing? has no members, and `equals()` would be unresolved on it
        KotlinType leftType = leftTypeInfo.getType();
        if (leftType != null && KotlinBuiltIns.isNothingOrNullableNothing(leftType)) {
            traceInterpretingRightAsNullableAny.recordType(left, components.builtIns.getNullableAnyType());
        }

        ExpressionTypingContext newContext = context.replaceBindingTrace(traceInterpretingRightAsNullableAny);
        ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, newContext);
        Call call = CallMaker.makeCallWithExpressions(
                expression,
                receiver,
                // semantically, a call to `==` is a safe call
                new KtPsiFactory(expression.getProject()).createSafeCallNode(),
                operationSign,
                Collections.singletonList(right)
        );
        OverloadResolutionResults<FunctionDescriptor> resolutionResults =
                components.callResolver.resolveCallWithGivenName(newContext, call, operationSign, OperatorNameConventions.EQUALS);

        traceInterpretingRightAsNullableAny.commit(new TraceEntryFilter() {
            @Override
            public boolean accept(@Nullable WritableSlice<?, ?> slice, Object key) {
                // the type of the right (and sometimes left) expression isn't 'Any?' actually
                if ((key == right || key == left) && slice == EXPRESSION_TYPE_INFO) return false;
                return true;
            }
        }, true);

        if (resolutionResults.isSuccess()) {
            FunctionDescriptor equals = resolutionResults.getResultingCall().getResultingDescriptor();
            if (ensureBooleanResult(operationSign, OperatorNameConventions.EQUALS, equals.getReturnType(), context)) {
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
    private KotlinTypeInfo visitComparison(
            @NotNull KtBinaryExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull KtSimpleNameExpression operationSign
    ) {
        KotlinTypeInfo typeInfo = getTypeInfoForBinaryCall(OperatorNameConventions.COMPARE_TO, context, expression);
        KotlinType compareToReturnType = typeInfo.getType();
        KotlinType type = null;
        if (compareToReturnType != null && !compareToReturnType.isError()) {
            if (KotlinTypeChecker.DEFAULT.equalTypes(components.builtIns.getIntType(), compareToReturnType)) {
                type = components.builtIns.getBooleanType();
            }
            else {
                context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
            }
        }
        return typeInfo.replaceType(type);
    }

    @NotNull
    private KotlinTypeInfo visitBooleanOperationExpression(
            @Nullable IElementType operationType,
            @Nullable KtExpression left,
            @Nullable KtExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        KotlinType booleanType = components.builtIns.getBooleanType();
        KotlinTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context.replaceExpectedType(booleanType), facade);
        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();

        LexicalWritableScope leftScope = newWritableScopeImpl(context, LexicalScopeKind.LEFT_BOOLEAN_EXPRESSION, facade.getComponents().overloadChecker);
        // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
        boolean isAnd = operationType == KtTokens.ANDAND;
        DataFlowInfo flowInfoLeft = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(left, isAnd, context).and(dataFlowInfo);
        LexicalWritableScope rightScope = isAnd ? leftScope : newWritableScopeImpl(context, LexicalScopeKind.RIGHT_BOOLEAN_EXPRESSION,
                                                                                   facade.getComponents().overloadChecker);

        ExpressionTypingContext contextForRightExpr =
                context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope).replaceExpectedType(booleanType);
        if (right != null) {
            facade.getTypeInfo(right, contextForRightExpr);
        }
        return leftTypeInfo.replaceType(booleanType);
    }

    @NotNull
    private KotlinTypeInfo visitElvisExpression(
            @NotNull KtBinaryExpression expression,
            @NotNull ExpressionTypingContext contextWithExpectedType
    ) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        KtExpression left = expression.getLeft();
        KtExpression right = expression.getRight();

        if (left == null || right == null) {
            getTypeInfoOrNullType(left, context, facade);
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        Call call = createCallForSpecialConstruction(expression, expression.getOperationReference(), Lists.newArrayList(left, right));
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                call, ResolveConstruct.ELVIS, Lists.newArrayList("left", "right"),
                Lists.newArrayList(true, false), contextWithExpectedType, null);
        KotlinTypeInfo leftTypeInfo = BindingContextUtils.getRecordedTypeInfo(left, context.trace.getBindingContext());
        if (ArgumentTypeResolver.isFunctionLiteralArgument(left, context)) {
            context.trace.report(USELESS_ELVIS_ON_LAMBDA_EXPRESSION.on(expression.getOperationReference()));
            if (leftTypeInfo == null) return TypeInfoFactoryKt.noTypeInfo(context);
        }
        assert leftTypeInfo != null : "Left expression was not processed: " + expression;
        KotlinType leftType = leftTypeInfo.getType();
        if (leftType != null && isKnownToBeNotNull(left, leftType, context)) {
            context.trace.report(USELESS_ELVIS.on(expression, leftType));
        }
        else if (KtPsiUtil.isNullConstant(right)) {
            context.trace.report(USELESS_ELVIS_RIGHT_IS_NULL.on(expression));
        }
        KotlinTypeInfo rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.getBindingContext());
        if (rightTypeInfo == null && ArgumentTypeResolver.isFunctionLiteralArgument(right, context)) {
            // the type is computed later in call completer according to the '?:' semantics as a function
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        assert rightTypeInfo != null : "Right expression was not processed: " + expression;
        boolean loopBreakContinuePossible = leftTypeInfo.getJumpOutPossible() || rightTypeInfo.getJumpOutPossible();
        KotlinType rightType = rightTypeInfo.getType();

        // Only left argument DFA is taken into account here: we cannot be sure that right argument is joined
        // (we merge it with right DFA if right argument contains no jump outside)
        DataFlowInfo dataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getInfo(call.getValueArguments().get(1));

        KotlinType type = resolvedCall.getResultingDescriptor().getReturnType();
        if (type == null ||
            rightType == null ||
            leftType == null && KotlinBuiltIns.isNothing(rightType)) return TypeInfoFactoryKt.noTypeInfo(dataFlowInfo);

        if (leftType != null) {
            DataFlowValue leftValue = createDataFlowValue(left, leftType, context);
            DataFlowInfo rightDataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getResultInfo();
            boolean jumpInRight = KotlinBuiltIns.isNothing(rightType);
            DataFlowValue nullValue = DataFlowValue.nullValue(components.builtIns);
            // left argument is considered not-null if it's not-null also in right part or if we have jump in right part
            if (jumpInRight || !rightDataFlowInfo.getStableNullability(leftValue).canBeNull()) {
                dataFlowInfo = dataFlowInfo.disequate(leftValue, nullValue);
                if (left instanceof KtBinaryExpressionWithTypeRHS) {
                    dataFlowInfo = establishSubtypingForTypeRHS((KtBinaryExpressionWithTypeRHS) left, dataFlowInfo, context);
                }
            }
            DataFlowValue resultValue = DataFlowValueFactory.createDataFlowValue(expression, type, context);
            dataFlowInfo = dataFlowInfo.disequate(resultValue, nullValue);
            if (!jumpInRight) {
                dataFlowInfo = dataFlowInfo.or(rightDataFlowInfo);
            }
        }

        // Sometimes return type for special call for elvis operator might be nullable,
        // but result is not nullable if the right type is not nullable
        if (!TypeUtils.isNullableType(rightType) && TypeUtils.isNullableType(type)) {
            type = TypeUtils.makeNotNullable(type);
        }
        if (context.contextDependency == DEPENDENT) {
            return TypeInfoFactoryKt.createTypeInfo(type, dataFlowInfo);
        }

        // If break or continue was possible, take condition check info as the jump info
        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkType(type, expression, contextWithExpectedType),
                                                dataFlowInfo,
                                                loopBreakContinuePossible,
                                                context.dataFlowInfo);
    }

    @NotNull
    private static DataFlowInfo establishSubtypingForTypeRHS(
            @NotNull KtBinaryExpressionWithTypeRHS left,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ExpressionTypingContext context
    ) {
        IElementType operationType = left.getOperationReference().getReferencedNameElementType();
        if (operationType == AS_SAFE) {
            KtExpression underSafeAs = left.getLeft();
            KotlinType underSafeAsType = context.trace.getType(underSafeAs);
            if (underSafeAsType != null) {
                DataFlowValue underSafeAsValue = createDataFlowValue(underSafeAs, underSafeAsType, context);
                KotlinType targetType = context.trace.get(BindingContext.TYPE, left.getRight());
                if (targetType != null) {
                    return dataFlowInfo.establishSubtyping(underSafeAsValue, targetType);
                }
            }
        }
        return dataFlowInfo;
    }

    @NotNull
    public KotlinTypeInfo checkInExpression(
            @NotNull KtElement callElement,
            @NotNull KtSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable KtExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        KtExpression left = leftArgument.getArgumentExpression();
        ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
        if (right == null) {
            if (left != null) facade.getTypeInfo(left, contextWithNoExpectedType);
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        KotlinTypeInfo rightTypeInfo = facade.getTypeInfo(right, contextWithNoExpectedType);
        DataFlowInfo dataFlowInfo = rightTypeInfo.getDataFlowInfo();

        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        OverloadResolutionResults<FunctionDescriptor> resolutionResult = components.callResolver.resolveCallWithGivenName(
                contextWithDataFlow,
                CallMaker.makeCall(callElement, receiver, null, operationSign, Collections.singletonList(leftArgument)),
                operationSign,
                OperatorNameConventions.CONTAINS);
        KotlinType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context.contextDependency);
        ensureBooleanResult(operationSign, OperatorNameConventions.CONTAINS, containsType, context);

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


    private boolean ensureBooleanResult(KtExpression operationSign, Name name, KotlinType resultType, ExpressionTypingContext context) {
        return ensureBooleanResultWithCustomSubject(operationSign, resultType, "'" + name + "'", context);
    }

    private boolean ensureBooleanResultWithCustomSubject(
            KtExpression operationSign,
            KotlinType resultType,
            String subjectName,
            ExpressionTypingContext context
    ) {
        if (resultType != null) {
            // TODO : Relax?
            if (!components.builtIns.isBooleanOrSubtype(resultType)) {
                context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, subjectName, components.builtIns.getBooleanType(), resultType));
                return false;
            }
        }
        return true;
    }

    private void ensureNonemptyIntersectionOfOperandTypes(KtBinaryExpression expression, final ExpressionTypingContext context) {
        KtExpression left = expression.getLeft();
        if (left == null) return;

        KtExpression right = expression.getRight();

        // TODO : duplicated effort for == and !=
        KotlinType leftType = facade.getTypeInfo(left, context).getType();
        if (leftType != null && right != null) {
            KotlinType rightType = facade.getTypeInfo(right, context).getType();

            if (rightType != null) {
                if (TypeIntersector.isIntersectionEmpty(leftType, rightType)) {
                    context.trace.report(EQUALITY_NOT_APPLICABLE.on(expression, expression.getOperationReference(), leftType, rightType));
                }
                SenselessComparisonChecker.checkSenselessComparisonWithNull(
                        expression, left, right, context,
                        new Function1<KtExpression, KotlinType>() {
                            @Override
                            public KotlinType invoke(KtExpression expression) {
                                return facade.getTypeInfo(expression, context).getType();
                            }
                        },
                        new Function1<DataFlowValue, Nullability>() {
                            @Override
                            public Nullability invoke(DataFlowValue value) {
                                return context.dataFlowInfo.getStableNullability(value);
                            }
                        });
            }
        }
    }

    @NotNull
    private KotlinTypeInfo visitAssignmentOperation(KtBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    @NotNull
    private KotlinTypeInfo visitAssignment(KtBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    @NotNull
    private KotlinTypeInfo assignmentIsNotAnExpressionError(KtBinaryExpression expression, ExpressionTypingContext context) {
        facade.checkStatementType(expression, context);
        context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @Override
    public KotlinTypeInfo visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, ExpressionTypingContext context) {
        return components.dataFlowAnalyzer.checkType(resolveArrayAccessGetMethod(expression, context), expression, context);
    }

    @Override
    public KotlinTypeInfo visitClass(@NotNull KtClass klass, ExpressionTypingContext context) {
        // analyze class in illegal position and write descriptor to trace but do not write to any scope
        components.localClassifierAnalyzer.processClassOrObject(
                null, context.replaceContextDependency(INDEPENDENT),
                context.scope.getOwnerDescriptor(),
                klass
        );
        return declarationInIllegalContext(klass, context);
    }

    @NotNull
    private static KotlinTypeInfo declarationInIllegalContext(
            @NotNull KtDeclaration declaration,
            @NotNull ExpressionTypingContext context
    ) {
        context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(declaration));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @Override
    public KotlinTypeInfo visitProperty(@NotNull KtProperty property, ExpressionTypingContext context) {
        components.localVariableResolver.process(property, context, context.scope, facade);
        return declarationInIllegalContext(property, context);
    }

    @NotNull
    public KotlinTypeInfo getTypeInfoForBinaryCall(
            @NotNull Name name,
            @NotNull ExpressionTypingContext context,
            @NotNull KtBinaryExpression binaryExpression
    ) {
        KtExpression left = binaryExpression.getLeft();
        KotlinTypeInfo typeInfo;
        if (left != null) {
            //left here is a receiver, so it doesn't depend on expected type
            typeInfo = facade.getTypeInfo(left, context.replaceContextDependency(INDEPENDENT).replaceExpectedType(NO_EXPECTED_TYPE));
        }
        else {
            typeInfo = TypeInfoFactoryKt.noTypeInfo(context);
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
    public KotlinTypeInfo visitDeclaration(@NotNull KtDeclaration dcl, ExpressionTypingContext context) {
        return declarationInIllegalContext(dcl, context);
    }

    @Override
    public KotlinTypeInfo visitStringTemplateExpression(
            @NotNull KtStringTemplateExpression expression,
            ExpressionTypingContext contextWithExpectedType
    ) {
        final ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
                                                                       .replaceContextDependency(INDEPENDENT);

        checkLiteralPrefixAndSuffix(expression, context);

        class StringTemplateVisitor extends KtVisitorVoid {
            private KotlinTypeInfo typeInfo = TypeInfoFactoryKt.noTypeInfo(context);

            @Override
            public void visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry) {
                KtExpression entryExpression = entry.getExpression();
                if (entryExpression != null) {
                    typeInfo = facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(typeInfo.getDataFlowInfo()));
                }
            }

            @Override
            public void visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry) {
                CompileTimeConstantChecker.CharacterWithDiagnostic value =
                        CompileTimeConstantChecker.escapedStringToCharacter(entry.getText(), entry);
                Diagnostic diagnostic = value.getDiagnostic();
                if (diagnostic != null) {
                    context.trace.report(diagnostic);
                }
            }
        }
        StringTemplateVisitor visitor = new StringTemplateVisitor();
        for (KtStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(visitor);
        }
        components.constantExpressionEvaluator.evaluateExpression(expression, context.trace, contextWithExpectedType.expectedType);
        return components.dataFlowAnalyzer.checkType(visitor.typeInfo.replaceType(components.builtIns.getStringType()),
                                                     expression,
                                                     contextWithExpectedType);
    }

    private static void checkLiteralPrefixAndSuffix(@NotNull PsiElement expression, ExpressionTypingContext context) {
        checkLiteralPrefixOrSuffix(PsiTreeUtil.prevLeaf(expression), context);
        checkLiteralPrefixOrSuffix(PsiTreeUtil.nextLeaf(expression), context);
    }

    private static void checkLiteralPrefixOrSuffix(PsiElement prefixOrSuffix, ExpressionTypingContext context) {
        if (illegalLiteralPrefixOrSuffix(prefixOrSuffix)) {
            context.trace.report(Errors.UNSUPPORTED.on(prefixOrSuffix, "literal prefixes and suffixes"));
        }
    }

    private static boolean illegalLiteralPrefixOrSuffix(@Nullable PsiElement element) {
        if (element == null) return false;

        IElementType elementType = element.getNode().getElementType();
        return elementType == IDENTIFIER ||
               elementType == INTEGER_LITERAL ||
               elementType == FLOAT_LITERAL ||
               elementType instanceof KtKeywordToken;
    }

    @Override
    public KotlinTypeInfo visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, ExpressionTypingContext context) {
        return visitAnnotatedExpression(expression, context, false);
    }

    public KotlinTypeInfo visitAnnotatedExpression(KtAnnotatedExpression expression, ExpressionTypingContext context, boolean isStatement) {
        resolveAnnotationsOnExpression(expression, context);

        KtExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        return facade.getTypeInfo(baseExpression, context, isStatement);
    }

    protected void resolveAnnotationsOnExpression(KtAnnotatedExpression expression, ExpressionTypingContext context) {
        if (!(expression.getBaseExpression() instanceof KtObjectLiteralExpression)) {
            // annotations on object literals are resolved later inside LazyClassDescriptor
            components.annotationResolver.resolveAnnotationsWithArguments(context.scope, expression.getAnnotationEntries(), context.trace);
        }
    }

    @Override
    public KotlinTypeInfo visitKtElement(@NotNull KtElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @NotNull
    /*package*/ KotlinTypeInfo resolveArrayAccessSetMethod(
            @NotNull KtArrayAccessExpression arrayAccessExpression,
            @NotNull KtExpression rightHandSide,
            @NotNull ExpressionTypingContext context,
            @NotNull BindingTrace traceForResolveResult
    ) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, rightHandSide, context, traceForResolveResult, false, false);
    }

    @NotNull
    /*package*/ KotlinTypeInfo resolveImplicitArrayAccessSetMethod(
            @NotNull KtArrayAccessExpression arrayAccessExpression,
            @NotNull KtExpression rightHandSide,
            @NotNull ExpressionTypingContext context,
            @NotNull BindingTrace traceForResolveResult
    ) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, rightHandSide, context, traceForResolveResult, false, true);
    }

    @NotNull
    /*package*/ KotlinTypeInfo resolveArrayAccessGetMethod(
            @NotNull KtArrayAccessExpression arrayAccessExpression,
            @NotNull ExpressionTypingContext context
    ) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, null, context, context.trace, true, false);
    }

    @NotNull
    private KotlinTypeInfo resolveArrayAccessSpecialMethod(
            @NotNull KtArrayAccessExpression arrayAccessExpression,
            @Nullable KtExpression rightHandSide, //only for 'set' method
            @NotNull ExpressionTypingContext oldContext,
            @NotNull BindingTrace traceForResolveResult,
            boolean isGet,
            boolean isImplicit
    ) {
        KtExpression arrayExpression = arrayAccessExpression.getArrayExpression();
        if (arrayExpression == null) return TypeInfoFactoryKt.noTypeInfo(oldContext);


        KotlinTypeInfo arrayTypeInfo = facade.safeGetTypeInfo(arrayExpression, oldContext.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT));
        KotlinType arrayType = ExpressionTypingUtils.safeGetType(arrayTypeInfo);

        ExpressionTypingContext context = oldContext.replaceDataFlowInfo(arrayTypeInfo.getDataFlowInfo());
        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(arrayExpression, arrayType, context.trace.getBindingContext());
        if (!isGet) assert rightHandSide != null;

        Call call = isGet
                    ? CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
                    : CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide, Call.CallType.ARRAY_SET_METHOD);
        OverloadResolutionResults<FunctionDescriptor> functionResults = components.callResolver.resolveCallWithGivenName(
                context, call, arrayAccessExpression, Name.identifier(isGet ? "get" : "set"));

        List<KtExpression> indices = arrayAccessExpression.getIndexExpressions();
        // The accumulated data flow info of all index expressions is saved on the last index
        KotlinTypeInfo resultTypeInfo = arrayTypeInfo;
        if (!indices.isEmpty()) {
            resultTypeInfo = facade.getTypeInfo(indices.get(indices.size() - 1), context);
        }

        if (!isGet) {
            resultTypeInfo = facade.getTypeInfo(rightHandSide, context);
        }

        if ((isImplicit && !functionResults.isSuccess()) || !functionResults.isSingleResult()) {
            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
            return resultTypeInfo.clearType();
        }
        traceForResolveResult.record(isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression,
                                     functionResults.getResultingCall());
        return resultTypeInfo.replaceType(functionResults.getResultingDescriptor().getReturnType());
    }
}
