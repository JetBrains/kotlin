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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.incremental.KotlinLookupLocation;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.parsing.ParseUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.psiUtil.ReservedCheckingKt;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.calls.checkers.*;
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession;
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.calls.tasks.OldResolutionCandidate;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.calls.tower.NewAbstractResolvedCall;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.calls.util.CallUtilKt;
import org.jetbrains.kotlin.resolve.checkers.UnderscoreChecker;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ContextReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.utils.ScopeUtilsKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.ResolveConstruct;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.types.expressions.unqualifiedSuper.UnqualifiedSuperKt;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
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

        return isLValue(expression, parent);
    }

    public static boolean isLValue(@NotNull KtSimpleNameExpression expression, @Nullable PsiElement parent) {
        if (!(parent instanceof KtBinaryExpression)) {
            return false;
        }

        KtBinaryExpression binaryExpression = (KtBinaryExpression) parent;
        if (!OperatorConventions.BINARY_OPERATION_NAMES.containsKey(binaryExpression.getOperationToken()) &&
            !KtTokens.ALL_ASSIGNMENTS.contains(binaryExpression.getOperationToken())) {
            return false;
        }
        return PsiTreeUtil.isAncestor(binaryExpression.getLeft(), expression, false);
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

    private void checkNull(
            @NotNull KtSimpleNameExpression expression,
            @NotNull ExpressionTypingContext context,
            @Nullable KotlinType type
    ) {
        // Receivers are normally analyzed at resolve, with an exception of KT-10175
        if (type != null && !KotlinTypeKt.isError(type) && !isLValueOrUnsafeReceiver(expression)) {
            DataFlowValue dataFlowValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context);
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
        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
            ReservedCheckingKt.checkReservedYield(expression, context.trace);
        }

        // TODO : other members
        // TODO : type substitutions???
        CallExpressionResolver callExpressionResolver = components.callExpressionResolver;
        KotlinTypeInfo typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, null, null, context);
        checkNull(expression, context, typeInfo.getType());

        components.constantExpressionEvaluator.evaluateExpression(
                expression, context.trace, context.expectedType, evaluateIntegerConstantInIndependentMode(context)
        );
        return components.dataFlowAnalyzer.checkType(typeInfo, expression, context); // TODO : Extensions to this
    }

    @Override
    public KotlinTypeInfo visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, ExpressionTypingContext context) {
        KtExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        KotlinTypeInfo result = facade.getTypeInfo(innerExpression, context);
        KotlinType resultType = result.getType();
        if (resultType != null) {
            DataFlowValue innerValue = components.dataFlowValueFactory.createDataFlowValue(innerExpression, resultType, context);
            DataFlowValue resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, resultType, context);
            result = result.replaceDataFlowInfo(result.getDataFlowInfo().assign(resultValue, innerValue,
                                                                                components.languageVersionSettings));
        }
        return result;
    }

    @Override
    public KotlinTypeInfo visitConstantExpression(@NotNull KtConstantExpression expression, ExpressionTypingContext context) {
        IElementType elementType = expression.getNode().getElementType();
        if (elementType == KtNodeTypes.CHARACTER_CONSTANT
            || elementType == KtNodeTypes.INTEGER_CONSTANT
            || elementType == KtNodeTypes.FLOAT_CONSTANT) {
            checkLiteralPrefixAndSuffix(expression, context);
        }

        if (elementType == KtNodeTypes.INTEGER_CONSTANT || elementType == KtNodeTypes.FLOAT_CONSTANT) {
            checkUnderscores(expression, elementType, context);
        }

        CompileTimeConstant<?> compileTimeConstant = components.constantExpressionEvaluator.evaluateExpression(
                expression, context.trace, context.expectedType, evaluateIntegerConstantInIndependentMode(context)
        );

        if (compileTimeConstant instanceof UnsignedErrorValueTypeConstant) {
            ErrorValue.ErrorValueWithMessage value = ((UnsignedErrorValueTypeConstant) compileTimeConstant).getErrorValue();
            context.trace.report(Errors.UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH.on(expression));

            return TypeInfoFactoryKt.createTypeInfo(value.getType(components.moduleDescriptor), context);
        }
        else if (!(compileTimeConstant instanceof IntegerValueTypeConstant)) {
            CompileTimeConstantChecker constantChecker = new CompileTimeConstantChecker(context, components.moduleDescriptor, false);
            ConstantValue constantValue =
                    compileTimeConstant != null ? ((TypedCompileTimeConstant) compileTimeConstant).getConstantValue() : null;
            boolean hasError = constantChecker.checkConstantExpressionType(constantValue, expression, context.expectedType);
            if (hasError) {
                return TypeInfoFactoryKt.createTypeInfo(
                        constantValue != null ? constantValue.getType(components.moduleDescriptor) : getDefaultType(elementType),
                        context
                );
            }
        }

        assert compileTimeConstant != null :
                "CompileTimeConstant should be evaluated for constant expression or an error should be recorded " +
                expression.getText();
        return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(compileTimeConstant, expression, context);
    }

    private void checkUnderscores(
            @NotNull KtConstantExpression expression,
            @NotNull IElementType elementType,
            @NotNull ExpressionTypingContext context
    ) {
        String text = expression.getText().toLowerCase();

        if (!text.contains("_")) return;

        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.UnderscoresInNumericLiterals)) {
            context.trace.report(Errors.UNSUPPORTED_FEATURE.on(expression,
                                                               TuplesKt.to(LanguageFeature.UnderscoresInNumericLiterals, components.languageVersionSettings)));
            return;
        }

        if (ParseUtilsKt.hasIllegalUnderscore(expression.getText(), elementType)) {
            context.trace.report(Errors.ILLEGAL_UNDERSCORE.on(expression));
        }
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

        DefinitelyNotNullDeprecationKt.reportDeprecatedDefinitelyNotNullSyntax(expression, right, context);

        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();

        boolean allowBareTypes = BARE_TYPES_ALLOWED.contains(operationType);
        TypeResolutionContext typeResolutionContext = new TypeResolutionContext(context.scope, context.trace, true, allowBareTypes, context.isDebuggerContext);
        PossiblyBareType possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, right);

        KotlinTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);

        KotlinType subjectType = typeInfo.getType();
        KotlinType targetType = reconstructBareType(right, possiblyBareTarget, subjectType, context.trace, components.builtIns);

        if (subjectType != null) {
            checkBinaryWithTypeRHS(expression, context, targetType, subjectType);
            DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
            if (operationType == AS_KEYWORD) {
                DataFlowValue value = components.dataFlowValueFactory.createDataFlowValue(left, subjectType, context);
                typeInfo = typeInfo.replaceDataFlowInfo(dataFlowInfo.establishSubtyping(value, targetType,
                                                                                        components.languageVersionSettings));
            }
        }

        KotlinType result = operationType == AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        KotlinTypeInfo resultTypeInfo = components.dataFlowAnalyzer.checkType(typeInfo.replaceType(result), expression, context);

        RttiExpressionInformation rttiInformation = new RttiExpressionInformation(
                expression.getLeft(),
                subjectType,
                result,
                operationType == AS_SAFE ? RttiOperation.SAFE_AS : RttiOperation.AS
        );
        for (RttiExpressionChecker checker : components.rttiExpressionCheckers) {
            checker.check(rttiInformation, expression, context.trace);
        }

        return resultTypeInfo;
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
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProperCheckAnnotationsTargetInTypeUsePositions)) {
            components.annotationChecker.check(expression.getRight(), context.trace, null);
        }
    }

    private void checkForCastImpossibilityOrRedundancy(
            KtBinaryExpressionWithTypeRHS expression,
            KotlinType actualType,
            KotlinType targetType,
            ExpressionTypingContext context
    ) {
        if (actualType == null || noExpectedType(targetType) || KotlinTypeKt.isError(targetType)) return;

        if (Boolean.TRUE.equals(context.trace.get(BindingContext.CAST_TYPE_USED_AS_EXPECTED_TYPE, expression))) return;

        if (DynamicTypesKt.isDynamic(targetType)) {
            KtTypeReference right = expression.getRight();
            assert right != null : "We know target is dynamic, but RHS is missing";
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(right));
            return;
        }

        if (!TypeUtilsKt.isStubType(actualType) && !CastDiagnosticsUtil.isCastPossible(actualType, targetType, components.platformToKotlinClassMapper)) {
            context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationReference()));
            return;
        }

        if (CastDiagnosticsUtil.INSTANCE.castIsUseless(expression, context, targetType, actualType)) {
            context.trace.report(USELESS_CAST.on(expression));
            return;
        }

        if (CastDiagnosticsUtil.isCastErased(actualType, targetType, KotlinTypeChecker.DEFAULT)) {
            context.trace.report(UNCHECKED_CAST.on(expression, actualType, targetType));
        }
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
            boolean validType = supertype != null && !KotlinTypeKt.isError(supertype);
            if (result == null && (validClassifier || validType)) {
                context.trace.report(NOT_A_SUPERTYPE.on(superTypeQualifier));
            }
            else if (redundantTypeArguments != null) {
                context.trace.report(TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER.on(redundantTypeArguments));
            }

            if (!components.languageVersionSettings.supportsFeature(LanguageFeature.QualifiedSupertypeMayBeExtendedByOtherSupertype) &&
                result != null &&
                (validClassifier || validType)
            ) {
                checkResolvedExplicitlyQualifiedSupertype(context.trace, result, supertypes, superTypeQualifier);
            }
        }
        else {
            if (UnqualifiedSuperKt.isPossiblyAmbiguousUnqualifiedSuper(expression, supertypes)) {
                Pair<Collection<KotlinType>, Boolean> supertypesResolvedFromContextWithEqualsMigration =
                        UnqualifiedSuperKt.resolveUnqualifiedSuperFromExpressionContext(
                                expression, supertypes, components.builtIns.getAnyType());
                Collection<KotlinType> supertypesResolvedFromContext = supertypesResolvedFromContextWithEqualsMigration.getFirst();
                if (supertypesResolvedFromContextWithEqualsMigration.getSecond()) {
                    context.trace.record(SUPER_EXPRESSION_FROM_ANY_MIGRATION, expression, true);
                }
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
                    return LabelResolver.LabeledReceiverResolutionResult.Companion.labelResolutionSuccess(null);
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
                // `this` cannot point to context receiver
                for (ReceiverParameterDescriptor receiver : receivers) {
                    if (!(receiver.getValue() instanceof ContextReceiver)) {
                        result = receiver;
                        break;
                    }
                }
            }
            if (result != null) {
                context.trace.record(REFERENCE_TARGET, expression.getInstanceReference(), result.getContainingDeclaration());
                recordThisOrSuperCallInTraceAndCallExtension(context, result, expression);
            }
            return LabelResolver.LabeledReceiverResolutionResult.Companion.labelResolutionSuccess(result);
        }
    }

    private void recordThisOrSuperCallInTraceAndCallExtension(
            ExpressionTypingContext context,
            ReceiverParameterDescriptor descriptor,
            KtExpression expression
    ) {
        BindingTrace trace = context.trace;
        Call call = CallMaker.makeCall(expression, null, null, expression, Collections.emptyList());
        OldResolutionCandidate<ReceiverParameterDescriptor> resolutionCandidate =
                OldResolutionCandidate.create(
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
            CallCheckerContext callCheckerContext =
                    createCallCheckerContext(context);
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
            @NotNull KtObjectLiteralExpression expression,
            ExpressionTypingContext context
    ) {
        KotlinType[] result = new KotlinType[1];
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace,
                                                                            "trace to resolve object literal expression", expression);
        ObservableBindingTrace traceAdapter = new ObservableBindingTrace(temporaryTrace);
        traceAdapter.addHandler(CLASS, (slice, declaration, descriptor) -> {
            if (slice == CLASS && declaration == expression.getObjectDeclaration()) {
                result[0] = components.wrappedTypeFactory.createRecursionIntolerantDeferredType(context.trace, descriptor::getDefaultType);
            }
        });
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
        return callExpressionResolver.getCallExpressionTypeInfo(expression, context);
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

        KtExpression deparenthesizedBaseExpression = KtPsiUtil.deparenthesize(baseExpression);

        // a[i]++/-- takes special treatment because it is actually let j = i, arr = a in arr.set(j, a.get(j).inc())
        if ((operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS) &&
            deparenthesizedBaseExpression instanceof KtArrayAccessExpression) {
            KtExpression stubExpression = ExpressionTypingUtils.createFakeExpressionOfType(
                    baseExpression.getProject(), context.trace, "e", type);
            TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(
                    context.trace, "trace to resolve array access set method for unary expression", expression);
            ExpressionTypingContext newContext = context.replaceBindingTrace(temporaryBindingTrace);
            resolveImplicitArrayAccessSetMethod(
                    (KtArrayAccessExpression) deparenthesizedBaseExpression,
                    stubExpression,
                    newContext,
                    context.trace
            );
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
                result = ErrorUtils.createErrorType(ErrorTypeKind.UNIT_RETURN_TYPE_FOR_INC_DEC);
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
                    checkLValue(context.trace, context, baseExpression, stubExpression, expression, false);
                }
                // x++ type is x type, but ++x type is x.inc() type
                DataFlowValue receiverValue = components.dataFlowValueFactory.createDataFlowValue(
                        (ReceiverValue) call.getExplicitReceiver(), contextWithExpectedType);
                if (expression instanceof KtPrefixExpression) {
                    result = returnType;
                }
                else {
                    result = receiverType;
                    // Also record data flow information for x++ value (= x)
                    DataFlowValue returnValue = components.dataFlowValueFactory.createDataFlowValue(expression, receiverType, contextWithExpectedType);
                    typeInfo = typeInfo.replaceDataFlowInfo(typeInfo.getDataFlowInfo().assign(returnValue, receiverValue,
                                                                                              components.languageVersionSettings));
                }
            }
        }
        else {
            result = returnType;
        }

        CompileTimeConstant<?> value = components.constantExpressionEvaluator.evaluateExpression(
                expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType, evaluateIntegerConstantInIndependentMode(context)
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
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                call, ResolveConstruct.EXCL_EXCL, Collections.singletonList("baseExpr"), Collections.singletonList(true), context, null);
        KotlinTypeInfo baseTypeInfo = BindingContextUtils.getRecordedTypeInfo(baseExpression, context.trace.getBindingContext());

        boolean isFunctionLiteral = ArgumentTypeResolver.isFunctionLiteralArgument(baseExpression, context);
        boolean isCallableReference = ArgumentTypeResolver.isCallableReferenceArgument(baseExpression, context);
        if (isFunctionLiteral || isCallableReference) {
            DiagnosticFactory0<PsiElement> diagnosticFactory =
                    isFunctionLiteral ? NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION : NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE;
            context.trace.report(diagnosticFactory.on(operationSign));
            return baseTypeInfo != null ? baseTypeInfo : components.expressionTypingServices.getTypeInfo(baseExpression, context);
        }
        assert baseTypeInfo != null : "Base expression was not processed: " + expression;
        KotlinType baseType = baseTypeInfo.getType();
        if (baseType == null) {
            return baseTypeInfo;
        }
        DataFlowInfo dataFlowInfo = baseTypeInfo.getDataFlowInfo();
        if (isKnownToBeNotNull(baseExpression, baseType, context)) {
            context.trace.report(UNNECESSARY_NOT_NULL_ASSERTION.on(operationSign, TypeUtils.makeNotNullable(baseType)));
        }
        else {
            DataFlowValue value = components.dataFlowValueFactory.createDataFlowValue(baseExpression, baseType, context);
            baseTypeInfo = baseTypeInfo.replaceDataFlowInfo(dataFlowInfo.disequate(value, DataFlowValue.nullValue(components.builtIns),
                                                                                   components.languageVersionSettings));
        }
        KotlinType resultingType = components.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
                                   ? resolvedCall.getResultingDescriptor().getReturnType()
                                   : TypeUtils.makeNotNullable(baseType);
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
        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
            ReservedCheckingKt.checkReservedYield(labelExpression, context.trace);
        }
        if (labelExpression != null) {
            PsiElement labelIdentifier = labelExpression.getIdentifier();
            UnderscoreChecker.INSTANCE.checkIdentifier(labelIdentifier, context.trace, components.languageVersionSettings);
        }
        KtExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return TypeInfoFactoryKt.noTypeInfo(context);

        return facade.getTypeInfo(baseExpression, context, isStatement);
    }

    // Returns `true` if warnings should be reported for left-hand side of elvis and not-null (!!) assertion
    private boolean isKnownToBeNotNull(
            @NotNull KtExpression expression,
            @Nullable KotlinType ktType,
            @NotNull ExpressionTypingContext context
    ) {
        if (ktType == null) return false;

        if (KotlinTypeKt.isError(ktType) && !ErrorUtils.isUninferredTypeVariable(ktType)) return false;

        if (!TypeUtils.isNullableType(ktType)) return true;

        DataFlowValue dataFlowValue = components.dataFlowValueFactory.createDataFlowValue(expression, ktType, context);
        return context.dataFlowInfo.getStableNullability(dataFlowValue) == Nullability.NOT_NULL;
    }

    /**
     * @return {@code true} iff expression can be assigned to
     */
    public boolean checkLValue(
            @NotNull BindingTrace trace,
            @NotNull ExpressionTypingContext context,
            @NotNull KtExpression expressionWithParenthesis,
            @Nullable KtExpression rightHandSide,
            @NotNull KtOperationExpression operationExpression,
            boolean arraySetMethodAlreadyResolved
    ) {
        KtExpression expression = KtPsiUtil.deparenthesize(expressionWithParenthesis);
        if (expression instanceof KtArrayAccessExpression) {
            KtArrayAccessExpression arrayAccessExpression = (KtArrayAccessExpression) expression;
            KtExpression arrayExpression = arrayAccessExpression.getArrayExpression();
            if (arrayExpression == null || rightHandSide == null) return false;

            BindingTrace traceWithIndexedLValue;
            boolean methodSetIsResolved;
            if (!arraySetMethodAlreadyResolved) {
                TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(trace, "Trace for checking set function");
                ExpressionTypingContext findSetterContext = context.replaceBindingTrace(ignoreReportsTrace);
                KotlinTypeInfo info = resolveArrayAccessSetMethod(arrayAccessExpression, rightHandSide, findSetterContext, ignoreReportsTrace);

                traceWithIndexedLValue = ignoreReportsTrace;
                methodSetIsResolved = info.getType() != null;
            } else {
                traceWithIndexedLValue = trace;
                methodSetIsResolved = true;
            }

            IElementType operationType = operationExpression.getOperationReference().getReferencedNameElementType();
            if (KtTokens.AUGMENTED_ASSIGNMENTS.contains(operationType)
                    || operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS) {
                ResolvedCall<FunctionDescriptor> resolvedCall = traceWithIndexedLValue.get(INDEXED_LVALUE_SET, expression);
                if (resolvedCall != null && trace.wantsDiagnostics()) {
                    // Call must be validated with the actual, not temporary trace in order to report operator diagnostic
                    // Only unary assignment expressions (++, --) and +=/... must be checked, normal assignments have the proper trace
                    CallCheckerContext callCheckerContext =
                            new CallCheckerContext(
                                    context,
                                    components.deprecationResolver,
                                    components.moduleDescriptor,
                                    components.missingSupertypesResolver,
                                    trace
                            );
                    for (CallChecker checker : components.callCheckers) {
                        checker.check(resolvedCall, expression, callCheckerContext);
                    }
                    // Should make sure resolved call for 'set' operator is recorded, see KT-36956.
                    if (trace.get(INDEXED_LVALUE_SET, expression) == null) {
                        trace.record(INDEXED_LVALUE_SET, expression, resolvedCall);
                    }
                }
            }

            return methodSetIsResolved;
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

        OldResolutionCandidate<PropertySetterDescriptor> resolutionCandidate = OldResolutionCandidate.create(
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
            CallCheckerContext callCheckerContext =
                    createCallCheckerContext(context);
            for (CallChecker checker : components.callCheckers) {
                checker.check(resolvedCall, expression, callCheckerContext);
            }
        }
    }

    @NotNull
    private CallCheckerContext createCallCheckerContext(@NotNull ExpressionTypingContext context) {
        return new CallCheckerContext(
                context,
                components.deprecationResolver,
                components.moduleDescriptor,
                components.missingSupertypesResolver
        );
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
            result = visitIdentityEquality(expression, context);
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
                expression, contextWithExpectedType.trace, contextWithExpectedType.expectedType, evaluateIntegerConstantInIndependentMode(context)
        );
        if (value != null) {
            return components.dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, expression, contextWithExpectedType);
        }
        return components.dataFlowAnalyzer.checkType(result, expression, contextWithExpectedType);
    }

    private KotlinTypeInfo visitIdentityEquality(
            @NotNull KtBinaryExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        KotlinTypeInfo typeInfo = TypeInfoFactoryKt.createTypeInfo(components.builtIns.getBooleanType(), context);

        KtExpression left = expression.getLeft();
        KtExpression right = expression.getRight();

        if (left == null || right == null) return typeInfo;

        // compute type of LHS and RHS
        facade.getTypeInfo(left, context);
        facade.getTypeInfo(right, context);

        EqualityCallChecker.INSTANCE.checkIdentityEquality(expression, context);

        return typeInfo;
    }

    private KotlinTypeInfo visitEquality(
            KtBinaryExpression expression,
            ExpressionTypingContext context,
            KtSimpleNameExpression operationSign,
            KtExpression left,
            KtExpression right
    ) {
        if (right == null || left == null) {
            ExpressionTypingUtils.getTypeInfoOrNullType(right, context, facade);
            ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade);
            return TypeInfoFactoryKt.createTypeInfo(components.builtIns.getBooleanType(), context);
        }

        KotlinTypeInfo leftTypeInfo = getTypeInfoOrNullType(left, context, facade);

        DataFlowInfo dataFlowInfo = leftTypeInfo.getDataFlowInfo();
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        ExpressionReceiver receiver = createReceiverForEquals(left, contextWithDataFlow);
        Collection<FunctionDescriptor> equalsFunctions = findEqualsWithNullableAnyParameter(receiver, expression);

        Call call = CallMaker.makeCallWithExpressions(
                expression,
                receiver,
                null,
                operationSign,
                Collections.singletonList(right),
                Call.CallType.DEFAULT,
                // semantically, a call to `==` is a safe call
                true
        );

        OverloadResolutionResults<FunctionDescriptor> resolutionResults =
                components.callResolver.resolveEqualsCallWithGivenDescriptors(contextWithDataFlow, operationSign, receiver, call, equalsFunctions);

        if (!resolutionResults.isSuccess()) {
            if (resolutionResults.isAmbiguity()) {
                context.trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(operationSign, resolutionResults.getResultingCalls()));
            } else {
                context.trace.report(EQUALS_MISSING.on(operationSign));
            }
        }
        KotlinTypeInfo rightTypeInfo = facade.getTypeInfo(right, contextWithDataFlow);

        KotlinType leftType = leftTypeInfo.getType();
        KotlinType rightType = rightTypeInfo.getType();

        boolean isBuilderInferenceContext = context.inferenceSession instanceof BuilderInferenceSession;

        if (leftType != null && rightType != null && !TypeIntersector.isIntersectionEmpty(leftType, rightType) && isBuilderInferenceContext) {
            context.trace.record(MARKED_EQUALIY_CALL_PROPER_IN_BUILDER_INFERENCE, expression);
        }

        return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
    }

    private ExpressionReceiver createReceiverForEquals(KtExpression left, ExpressionTypingContext context) {
        KotlinType leftType = ExpressionTypingUtils.safeGetType(facade.safeGetTypeInfo(left, context));
        KotlinType receiverType = KotlinBuiltIns.isNothingOrNullableNothing(leftType) ?
                                  components.builtIns.getNullableAnyType() :
                                  leftType;
        return ExpressionReceiver.Companion.create(left, receiverType, context.trace.getBindingContext());
    }

    private List<FunctionDescriptor> findEqualsWithNullableAnyParameter(
            @NotNull ExpressionReceiver receiver,
            @NotNull KtBinaryExpression expression
    ) {
        KotlinType refinedType = KotlinBuiltIns.isNothingOrNullableNothing(receiver.getType()) ?
                                 components.builtIns.getNullableAnyType() :
                                 receiver.getType();
        Collection<? extends SimpleFunctionDescriptor> equalsMembers = refinedType.getMemberScope().getContributedFunctions(
                OperatorNameConventions.EQUALS, new KotlinLookupLocation(expression.getOperationReference())
        );

        return CollectionsKt.filter(equalsMembers, descriptor -> {
            if (ErrorUtils.isError(descriptor)) return true;

            if (descriptor.getValueParameters().size() != 1) return false;
            ValueParameterDescriptor valueParameter = descriptor.getValueParameters().get(0);
            return KotlinBuiltIns.isNullableAny(valueParameter.getType());
        });
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
        if (compareToReturnType != null && !KotlinTypeKt.isError(compareToReturnType)) {
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
        boolean isLeftFunctionLiteral = ArgumentTypeResolver.isFunctionLiteralArgument(left, context);
        boolean isLeftCallableReference = ArgumentTypeResolver.isCallableReferenceArgument(left, context);
        if (leftTypeInfo == null && (isLeftFunctionLiteral || isLeftCallableReference)) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        assert leftTypeInfo != null : "Left expression was not processed: " + expression;
        KotlinType leftType = leftTypeInfo.getType();
        KotlinTypeInfo rightTypeInfo = BindingContextUtils.getRecordedTypeInfo(right, context.trace.getBindingContext());
        if (rightTypeInfo == null && ArgumentTypeResolver.isFunctionLiteralOrCallableReference(right, context)) {
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
            DataFlowValue leftValue = components.dataFlowValueFactory.createDataFlowValue(left, leftType, context);
            DataFlowInfo rightDataFlowInfo = resolvedCall.getDataFlowInfoForArguments().getResultInfo();
            boolean jumpInRight = KotlinBuiltIns.isNothing(rightType);
            DataFlowValue nullValue = DataFlowValue.nullValue(components.builtIns);
            // left argument is considered not-null if it's not-null also in right part or if we have jump in right part
            if (jumpInRight || !rightDataFlowInfo.getStableNullability(leftValue).canBeNull()) {
                dataFlowInfo = dataFlowInfo.disequate(leftValue, nullValue, components.languageVersionSettings);
                if (left instanceof KtBinaryExpressionWithTypeRHS) {
                    dataFlowInfo = establishSubtypingForTypeRHS((KtBinaryExpressionWithTypeRHS) left, dataFlowInfo, context,
                                                                components.languageVersionSettings);
                }
            }
            DataFlowValue resultValue = components.dataFlowValueFactory.createDataFlowValue(expression, type, context);
            dataFlowInfo =
                    dataFlowInfo.assign(resultValue, leftValue, components.languageVersionSettings)
                    .disequate(resultValue, nullValue, components.languageVersionSettings);
            if (!jumpInRight) {
                DataFlowValue rightValue = components.dataFlowValueFactory.createDataFlowValue(right, rightType, context);
                rightDataFlowInfo = rightDataFlowInfo.assign(resultValue, rightValue, components.languageVersionSettings);
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
    private DataFlowInfo establishSubtypingForTypeRHS(
            @NotNull KtBinaryExpressionWithTypeRHS left,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ExpressionTypingContext context,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        IElementType operationType = left.getOperationReference().getReferencedNameElementType();
        if (operationType == AS_SAFE) {
            KtExpression underSafeAs = left.getLeft();
            KotlinType underSafeAsType = context.trace.getType(underSafeAs);
            if (underSafeAsType != null) {
                DataFlowValue underSafeAsValue = components.dataFlowValueFactory.createDataFlowValue(underSafeAs, underSafeAsType, context);
                KotlinType targetType = context.trace.get(BindingContext.TYPE, left.getRight());
                if (targetType != null) {
                    return dataFlowInfo.establishSubtyping(underSafeAsValue, targetType, languageVersionSettings);
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

        Call containsCall = CallMaker.makeCall(
                callElement, receiver, null, operationSign,
                Collections.singletonList(leftArgument), Call.CallType.CONTAINS
        );

        OverloadResolutionResults<FunctionDescriptor> resolutionResult = components.callResolver.resolveCallWithGivenName(
                contextWithDataFlow,
                containsCall,
                operationSign,
                OperatorNameConventions.CONTAINS);
        KotlinType containsType = OverloadResolutionResultsUtil.getResultingType(resolutionResult, context);
        ensureBooleanResult(operationSign, OperatorNameConventions.CONTAINS, containsType, context);

        if (left != null) {
            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
            rightTypeInfo = rightTypeInfo.replaceDataFlowInfo(dataFlowInfo);
        }

        if (resolutionResult.isSuccess() || isResolutionSuccessfulWithOnlyInputTypesWarnings(resolutionResult.getResultingCalls(), context)) {
            return rightTypeInfo.replaceType(components.builtIns.getBooleanType());
        } else {
            return rightTypeInfo.clearType();
        }
    }

    private static boolean isResolutionSuccessfulWithOnlyInputTypesWarnings(
            @Nullable Collection<? extends ResolvedCall<FunctionDescriptor>> allCandidates,
            @NotNull ExpressionTypingContext context
    ) {
        if (allCandidates == null || allCandidates.isEmpty()) return false;

        boolean areAllCandidatesFailedWithOnlyInputTypesError = allCandidates.stream().allMatch((resolvedCall) ->
            resolvedCall instanceof NewAbstractResolvedCall<?> && ((NewAbstractResolvedCall<?>) resolvedCall).containsOnlyOnlyInputTypesErrors()
        );
        boolean isNonStrictOnlyInputTypesCheckEnabled = !context.languageVersionSettings.supportsFeature(LanguageFeature.StrictOnlyInputTypesChecks);

        return areAllCandidatesFailedWithOnlyInputTypesError && isNonStrictOnlyInputTypesCheckEnabled;
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
        if (!context.isDebuggerContext) {
            context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
        }
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @Override
    public KotlinTypeInfo visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, ExpressionTypingContext context) {
        return components.dataFlowAnalyzer.checkType(resolveArrayAccessGetMethod(expression, context), expression, context);
    }

    @Override
    public KotlinTypeInfo visitCollectionLiteralExpression(
            @NotNull KtCollectionLiteralExpression expression, ExpressionTypingContext context
    ) {
        return components.collectionLiteralResolver.resolveCollectionLiteral(expression, context);
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
    private KotlinTypeInfo getTypeInfoForBinaryCall(
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
            resolutionResults = components.callResolver.resolveBinaryCall(contextWithDataFlow, receiver, binaryExpression, name);
        }
        else {
            resolutionResults = OverloadResolutionResultsImpl.nameNotFound();
        }

        if (resolutionResults.isSingleResult()) {
            typeInfo = typeInfo.replaceDataFlowInfo(resolutionResults.getResultingCall().getDataFlowInfoForArguments().getResultInfo());
        }

        return typeInfo.replaceType(OverloadResolutionResultsUtil.getResultingType(resolutionResults, context));
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
        ExpressionTypingContext context = contextWithExpectedType
                .replaceExpectedType(NO_EXPECTED_TYPE)
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
        components.constantExpressionEvaluator.evaluateExpression(expression, context.trace, contextWithExpectedType.expectedType, evaluateIntegerConstantInIndependentMode(context));
        return components.dataFlowAnalyzer.checkType(visitor.typeInfo.replaceType(components.builtIns.getStringType()),
                                                     expression,
                                                     contextWithExpectedType);
    }

    private static void checkLiteralPrefixAndSuffix(@NotNull PsiElement expression, ExpressionTypingContext context) {
        if (expression instanceof StubBasedPsiElement && ((StubBasedPsiElement) expression).getStub() != null) {
            return;
        }

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
        if (components.typeResolutionInterceptor.isEmpty()) return facade.getTypeInfo(baseExpression, context, isStatement);

        KotlinType newExpectedType = components.typeResolutionInterceptor.interceptType(baseExpression, context, context.expectedType);
        KotlinTypeInfo resultTypeInfo = facade.getTypeInfo(baseExpression, newExpectedType == context.expectedType ? context : context.replaceExpectedType(newExpectedType), isStatement);
        KotlinType newResultType = components.typeResolutionInterceptor.interceptType(baseExpression, context, resultTypeInfo.getType());
        components.dataFlowAnalyzer.checkType(newResultType, expression, context);
        return resultTypeInfo.getType() == newResultType ? resultTypeInfo : resultTypeInfo.replaceType(newResultType);
    }

    protected void resolveAnnotationsOnExpression(KtAnnotatedExpression expression, ExpressionTypingContext context) {
        if (isAnnotatedExpressionInBlockLevelBinary(expression)) {
            context.trace.report(ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE.on(expression));
        }

        if (!(expression.getBaseExpression() instanceof KtObjectLiteralExpression)) {
            // annotations on object literals are resolved later inside LazyClassDescriptor
            components.annotationResolver.resolveAnnotationsWithArguments(context.scope, expression.getAnnotationEntries(), context.trace);
        }
    }

    private static boolean isAnnotatedExpressionInBlockLevelBinary(KtAnnotatedExpression annotatedExpression) {
        PsiElement current = annotatedExpression;
        PsiElement parent = current.getParent();

        // Here we implicitly assume that grammar rules are:
        // blockLevelExpression = annotations expression
        // expression = binaryExpression
        // binaryExpression = prefixExpression <op> prefixExpression
        // prefixExpression = annotations expression

        // If there is no binary parent, annotations are being parsed the same way independently of newline after them
        if (!(parent instanceof KtBinaryExpression)) return false;

        while (parent instanceof KtBinaryExpression) {
            // if we came not from the left parent, there's no need to report an error
            if (((KtBinaryExpression) parent).getLeft() != current) {
                return false;
            }
            current = parent;
            parent = parent.getParent();
        }

        return KtPsiUtil.isStatementContainer(parent);
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
                context, call, arrayAccessExpression, isGet ? OperatorNameConventions.GET : OperatorNameConventions.SET);

        List<KtExpression> indices = arrayAccessExpression.getIndexExpressions();

        KotlinTypeInfo resultTypeInfo =
                computeAccumulatedInfoForArrayAccessExpression(arrayTypeInfo, indices, rightHandSide, isGet, context, facade);

        if ((isImplicit && !functionResults.isSuccess()) || !functionResults.isSingleResult()) {
            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
            return resultTypeInfo.clearType();
        }

        if (isGet) {
            traceForResolveResult.record(INDEXED_LVALUE_GET, arrayAccessExpression, functionResults.getResultingCall());
        } else {
            traceForResolveResult.record(INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.getResultingCall());
        }

        return resultTypeInfo.replaceType(functionResults.getResultingDescriptor().getReturnType());
    }

    private static KotlinTypeInfo computeAccumulatedInfoForArrayAccessExpression(
            @NotNull KotlinTypeInfo arrayTypeInfo,
            @NotNull List<KtExpression> indices,
            @Nullable KtExpression rightHandSide,
            boolean isGet,
            @NotNull ExpressionTypingContext context,
            @NotNull ExpressionTypingInternals facade
    ) {
        KotlinTypeInfo accumulatedTypeInfo = null;
        boolean forceResolve = !context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference);

        // The accumulated data flow info of all index expressions is saved on the last index
        if (!indices.isEmpty()) {
            accumulatedTypeInfo = getTypeInfo(indices.get(indices.size() - 1), facade, context, forceResolve);
        }

        if (!isGet && rightHandSide != null) {
            accumulatedTypeInfo = getTypeInfo(rightHandSide, facade, context, forceResolve);
        }

        return accumulatedTypeInfo != null ? accumulatedTypeInfo : arrayTypeInfo;
    }

    private static KotlinTypeInfo getTypeInfo(
            @NotNull KtExpression expression,
            @NotNull ExpressionTypingInternals facade,
            @NotNull ExpressionTypingContext context,
            boolean forceExpressionResolve
    ) {
        if (forceExpressionResolve) {
            return facade.getTypeInfo(expression, context);
        } else {
            return BindingContextUtils.getRecordedTypeInfo(expression, context.trace.getBindingContext());
        }
    }

    private boolean evaluateIntegerConstantInIndependentMode(ExpressionTypingContext context) {
        return context.contextDependency == INDEPENDENT && context.languageVersionSettings.supportsFeature(LanguageFeature.ApproximateIntegerLiteralTypesInReceiverPosition);
    }
}
