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
import com.google.common.collect.Multimap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallExpressionResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.calls.autocasts.Nullability;
import org.jetbrains.jet.lang.resolve.calls.context.ExpressionPosition;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.*;

@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor {
    protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitSimpleNameExpression(JetSimpleNameExpression expression, ExpressionTypingContext context) {
        // TODO : other members
        // TODO : type substitutions???
        CallExpressionResolver callExpressionResolver = context.expressionTypingServices.getCallExpressionResolver();
        JetTypeInfo typeInfo = callExpressionResolver.getSimpleNameExpressionTypeInfo(expression, NO_RECEIVER, null, context);
        JetType type = DataFlowUtils.checkType(typeInfo.getType(), expression, context);
        ExpressionTypingUtils.checkWrappingInRef(expression, context.trace, context.scope);
        return JetTypeInfo.create(type, typeInfo.getDataFlowInfo()); // TODO : Extensions to this
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(JetParenthesizedExpression expression, ExpressionTypingContext context) {
        return visitParenthesizedExpression(expression, context, false);
    }

    public JetTypeInfo visitParenthesizedExpression(JetParenthesizedExpression expression, ExpressionTypingContext context, boolean isStatement) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression == null) {
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        JetTypeInfo typeInfo = facade.getTypeInfo(innerExpression, context.replaceScope(context.scope), isStatement);
        return DataFlowUtils.checkType(typeInfo.getType(), expression, context, typeInfo.getDataFlowInfo());
    }

    @Override
    public JetTypeInfo visitConstantExpression(JetConstantExpression expression, ExpressionTypingContext context) {
        ASTNode node = expression.getNode();
        IElementType elementType = node.getElementType();
        String text = node.getText();
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        CompileTimeConstantResolver compileTimeConstantResolver = context.getCompileTimeConstantResolver();

        CompileTimeConstant<?> value;
        if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
            value = compileTimeConstantResolver.getIntegerValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
            value = compileTimeConstantResolver.getFloatValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
            value = compileTimeConstantResolver.getBooleanValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
            value = compileTimeConstantResolver.getCharValue(text, context.expectedType);
        }
        else if (elementType == JetNodeTypes.NULL) {
            value = compileTimeConstantResolver.getNullValue(context.expectedType);
        }
        else {
            throw new IllegalArgumentException("Unsupported constant: " + expression);
        }
        if (value instanceof ErrorValue) {
            ErrorValue errorValue = (ErrorValue) value;
            context.trace.report(ERROR_COMPILE_TIME_VALUE.on(node.getPsi(), errorValue.getMessage()));
            return JetTypeInfo.create(getDefaultType(elementType), context.dataFlowInfo);
        }
        else {
            context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, value);
            return DataFlowUtils.checkType(value.getType(builtIns), expression, context, context.dataFlowInfo);
        }
    }

    @Override
    public JetTypeInfo visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context) {
        JetExpression left = expression.getLeft();
        JetTypeReference right = expression.getRight();
        JetType result = null;
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (right != null) {
            JetType targetType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, right, context.trace, true);
            IElementType operationType = expression.getOperationSign().getReferencedNameElementType();

            boolean tryWithNoExpectedType = true;
            if (isTypeFlexible(left) || operationType == JetTokens.COLON) {
                TemporaryBindingTrace temporaryTraceWithExpectedType = TemporaryBindingTrace.create(
                        context.trace, "trace for resolve RHSExpression", expression);
                ExpressionTypingContext contextWithTemporaryTrace = context.replaceBindingTrace(temporaryTraceWithExpectedType).replaceExpectedType(targetType);
                JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithTemporaryTrace);
                if (typeInfo.getType() != null && checkBinaryWithTypeRHS(expression, contextWithTemporaryTrace, targetType, typeInfo.getType())) {
                    temporaryTraceWithExpectedType.commit();
                    dataFlowInfo = typeInfo.getDataFlowInfo();
                    tryWithNoExpectedType = false;
                }
            }

            if (tryWithNoExpectedType) {
                ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
                JetTypeInfo typeInfo = facade.getTypeInfo(left, contextWithNoExpectedType);
                if (typeInfo.getType() != null) {
                    checkBinaryWithTypeRHS(expression, contextWithNoExpectedType, targetType, typeInfo.getType());
                    dataFlowInfo = typeInfo.getDataFlowInfo();
                    if (operationType == JetTokens.AS_KEYWORD) {
                        DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(left, typeInfo.getType(), context.trace.getBindingContext());
                        dataFlowInfo = dataFlowInfo.establishSubtyping(value, targetType);
                    }
                }
            }

            result = operationType == JetTokens.AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        }
        else {
            dataFlowInfo = facade.getTypeInfo(left, context.replaceExpectedType(NO_EXPECTED_TYPE)).getDataFlowInfo();
        }
        return DataFlowUtils.checkType(result, expression, context, dataFlowInfo);
    }

    private boolean checkBinaryWithTypeRHS(JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context, @NotNull JetType targetType, JetType actualType) {
        JetSimpleNameExpression operationSign = expression.getOperationSign();
        IElementType operationType = operationSign.getReferencedNameElementType();
        if (operationType == JetTokens.COLON) {
            if (targetType != NO_EXPECTED_TYPE && !JetTypeChecker.INSTANCE.isSubtypeOf(actualType, targetType)) {
                context.trace.report(TYPE_MISMATCH.on(expression.getLeft(), targetType, actualType));
                return false;
            }
            return true;
        }
        else if (operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
            checkForCastImpossibility(expression, actualType, targetType, context);
            return true;
        }
        else {
            context.trace.report(UNSUPPORTED.on(operationSign, "binary operation with type RHS"));
            return false;
        }
    }

    private void checkForCastImpossibility(JetBinaryExpressionWithTypeRHS expression, JetType actualType, JetType targetType, ExpressionTypingContext context) {
        if (actualType == null || targetType == NO_EXPECTED_TYPE) return;

        JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
        if (!typeChecker.isSubtypeOf(targetType, actualType)) {
            if (typeChecker.isSubtypeOf(actualType, targetType)) {
                context.trace.report(USELESS_CAST_STATIC_ASSERT_IS_FINE.on(expression.getOperationSign()));
            }
            else {
                // See JET-58 Make 'as never succeeds' a warning, or even never check for Java (external) types
                context.trace.report(CAST_NEVER_SUCCEEDS.on(expression.getOperationSign()));
            }
        }
        else {
            if (typeChecker.isSubtypeOf(actualType, targetType)) {
                context.trace.report(USELESS_CAST.on(expression.getOperationSign()));
            }
            else {
                if (isCastErased(actualType, targetType, typeChecker)) {
                    context.trace.report(Errors.UNCHECKED_CAST.on(expression, actualType, targetType));
                }
            }
        }
    }

    /**
     * Check if assignment from supertype to subtype is erased.
     * It is an error in "is" statement and warning in "as".
     */
    public static boolean isCastErased(@NotNull JetType supertype, @NotNull JetType subtype, @NotNull JetTypeChecker typeChecker) {
        if (!(subtype.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            // TODO: what if it is TypeParameterDescriptor?
            return false;
        }

        // do not crash on error types
        if (ErrorUtils.isErrorType(supertype) || ErrorUtils.isErrorType(subtype)) {
            return false;
        }

        return hasDowncastsInTypeArguments(supertype, subtype, typeChecker) || hasErasedTypeArguments(supertype, subtype);
    }

    /*
      Check if type arguments are downcasted, which cannot be checked at run-time.

      Examples:
      1. a: MutableList<out Any> is MutableList<String> - true, because 'String' is more specific
      2. a: Collection<String> is List<Any> - false, because 'Any' is less specific, and it is guaranteed by static checker
      3. a: MutableCollection<String> is MutableList<Any> - false, because these types have empty intersection (type parameter is invariant)
     */
    private static boolean hasDowncastsInTypeArguments(
            @NotNull JetType supertype,
            @NotNull JetType subtype,
            @NotNull JetTypeChecker typeChecker
    ) {
        List<TypeParameterDescriptor> superParameters = supertype.getConstructor().getParameters();

        // This map holds arguments for type parameters of all superclasses of a type (see method comment for sample)
        Multimap<TypeConstructor, TypeProjection> subtypeSubstitutionMap = SubstitutionUtils.buildDeepSubstitutionMultimap(subtype);

        for (int i = 0; i < superParameters.size(); i++) {
            TypeProjection superArgument = supertype.getArguments().get(i);
            TypeParameterDescriptor parameter = superParameters.get(i);

            if (parameter.isReified()) {
                continue;
            }

            Collection<TypeProjection> substituted = subtypeSubstitutionMap.get(parameter.getTypeConstructor());
            for (TypeProjection substitutedArgument : substituted) {
                // For sample #2 (a: Collection<String> is List<Any>):
                // parameter = E declared in Collection
                // superArgument = String
                // substitutedArgument = Any, because Collection<Any> is the supertype of List<Any>

                // 1. Any..Nothing
                // 2. String..Nothing
                // 3. String..String
                JetType superOut = TypeCheckingProcedure.getOutType(parameter, superArgument);
                JetType superIn = TypeCheckingProcedure.getInType(parameter, superArgument);

                // 1. String..String
                // 2. Any..Nothing
                // 3. Any..Any
                JetType subOut = TypeCheckingProcedure.getOutType(parameter, substitutedArgument);
                JetType subIn = TypeCheckingProcedure.getInType(parameter, substitutedArgument);

                // super type range must be a subset of sub type range
                if (typeChecker.isSubtypeOf(superOut, subOut) && typeChecker.isSubtypeOf(subIn, superIn)) {
                    // continue
                }
                else {
                    return true;
                }
            }
        }

        return false;
    }

    /*
      Check if type arguments are erased, that is they are not mapped to type parameters of supertype's class

      Examples (MyMap is defined like this: trait MyMap<T>: Map<String, T>):
      1. a: Any is List<String> - true
      2. a: Collection<CharSequence> is List<String> - false
      3. a: Map<String, String> is MyMap<String> - false
     */
    private static boolean hasErasedTypeArguments(
            @NotNull JetType supertype,
            @NotNull JetType subtype
    ) {
        // Erase all type arguments, replacing them with unsubstituted versions:
        // 1. List<E>
        // 2. List<E>
        // 3. MyMap<T>
        JetType subtypeCleared = TypeUtils.makeUnsubstitutedType(
                (ClassDescriptor) subtype.getConstructor().getDeclarationDescriptor(), null);

        // This map holds arguments for type parameters of all superclasses of a type (see method comment for sample)
        // For all "E" declared in Collection, Iterable, etc., value will be type "E", where the latter E is declared in List
        Multimap<TypeConstructor, TypeProjection> clearTypeSubstitutionMap =
                SubstitutionUtils.buildDeepSubstitutionMultimap(subtypeCleared);


        // This set will contain all arguments for type parameters of superclass which are mapped from type parameters of subtype's class
        // 1. empty
        // 2. [E declared in List]
        // 3. [T declared in MyMap]
        Set<JetType> clearSubstituted = new HashSet<JetType>();

        List<TypeParameterDescriptor> superParameters = supertype.getConstructor().getParameters();
        for (TypeParameterDescriptor superParameter : superParameters) {
            Collection<TypeProjection> substituted = clearTypeSubstitutionMap.get(superParameter.getTypeConstructor());
            for (TypeProjection substitutedProjection : substituted) {
                clearSubstituted.add(substitutedProjection.getType());
            }
        }

        // For each type parameter of subtype's class, we check that it is mapped to type parameters of supertype,
        // that is its type is present in clearSubstituted set
        List<TypeParameterDescriptor> subParameters = subtype.getConstructor().getParameters();
        for (int i = 0; i < subParameters.size(); i++) {
            TypeParameterDescriptor parameter = subParameters.get(i);
            TypeProjection argument = subtype.getArguments().get(i);

            if (parameter.isReified()) {
                continue;
            }

            // "is List<*>", no check for type argument, actually
            if (argument.equals(SubstitutionUtils.makeStarProjection(parameter))) {
                continue;
            }

            // if parameter is mapped to nothing then it is erased
            // 1. return from here
            // 2. contains = true, don't return
            // 3. contains = true, don't return
            if (!clearSubstituted.contains(parameter.getDefaultType())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JetTypeInfo visitTupleExpression(JetTupleExpression expression, ExpressionTypingContext context) {
        // TODO: remove this method completely when tuples are droppped
        if (expression.getEntries().size() <= 3) {
            context.trace.report(TUPLES_ARE_NOT_SUPPORTED.on(expression));
        }
        else {
            context.trace.report(TUPLES_ARE_NOT_SUPPORTED_BIG.on(expression));
        }

        List<JetExpression> entries = expression.getEntries();
        List<JetType> types = new ArrayList<JetType>();
        for (JetExpression entry : entries) {
            types.add(context.expressionTypingServices.safeGetType(context.scope, entry, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace)); // TODO
        }
        if (context.expectedType != NO_EXPECTED_TYPE && KotlinBuiltIns.getInstance().isTupleType(context.expectedType)) {
            List<JetType> enrichedTypes = checkArgumentTypes(types, entries, context.expectedType.getArguments(), context);
            if (enrichedTypes != types) {
                return JetTypeInfo.create(KotlinBuiltIns.getInstance().getTupleType(enrichedTypes), context.dataFlowInfo);
            }
        }
        // TODO : labels
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getTupleType(types), expression, context, context.dataFlowInfo);
    }

    @NotNull
    private List<JetType> checkArgumentTypes(@NotNull List<JetType> argumentTypes, @NotNull List<JetExpression> arguments, @NotNull List<TypeProjection> expectedArgumentTypes, @NotNull ExpressionTypingContext context) {
        if (arguments.size() == 0 || argumentTypes.size() != arguments.size() || expectedArgumentTypes.size() != arguments.size()) {
            return argumentTypes;
        }
        List<JetType> result = Lists.newArrayListWithCapacity(arguments.size());
        for (int i = 0, argumentTypesSize = argumentTypes.size(); i < argumentTypesSize; i++) {
            result.add(DataFlowUtils.checkType(argumentTypes.get(i), arguments.get(i), context.replaceExpectedType(expectedArgumentTypes.get(i).getType())));
        }
        return result;
    }

    @Override
    public JetTypeInfo visitThisExpression(JetThisExpression expression, ExpressionTypingContext context) {
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
    public JetTypeInfo visitSuperExpression(JetSuperExpression expression, ExpressionTypingContext context) {
        LabelResolver.LabeledReceiverResolutionResult resolutionResult = resolveToReceiver(expression, context, true);

        if (context.expressionPosition == ExpressionPosition.FREE) {
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

    private JetTypeInfo errorInSuper(JetSuperExpression expression, ExpressionTypingContext context) {
        JetTypeReference superTypeQualifier = expression.getSuperTypeQualifier();
        if (superTypeQualifier != null) {
            context.expressionTypingServices.getTypeResolver().resolveType(context.scope, superTypeQualifier, context.trace, true);
        }
        return JetTypeInfo.create(null, context.dataFlowInfo);
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
            boolean validType = supertype != null && !ErrorUtils.isErrorType(supertype);
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
            context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
            context.trace.record(BindingContext.REFERENCE_TARGET, expression.getInstanceReference(), result.getConstructor().getDeclarationDescriptor());
            if (superTypeQualifier != null) {
                context.trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, superTypeQualifier, context.scope);
            }
        }
        return result;
    }

    @NotNull // No class receivers
    private LabelResolver.LabeledReceiverResolutionResult resolveToReceiver(
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
            }
            return LabelResolver.LabeledReceiverResolutionResult.labelResolutionSuccess(result);
        }
    }

    private boolean isDeclaredInClass(ReceiverParameterDescriptor receiver) {
        return receiver.getContainingDeclaration() instanceof ClassDescriptor;
    }

    @Override
    public JetTypeInfo visitBlockExpression(JetBlockExpression expression, ExpressionTypingContext context) {
        return visitBlockExpression(expression, context, false);
    }

    public JetTypeInfo visitBlockExpression(JetBlockExpression expression, ExpressionTypingContext context, boolean isStatement) {
        return context.expressionTypingServices.getBlockReturnedType(context.scope, expression, isStatement
                                                                                                ? CoercionStrategy.COERCION_TO_UNIT
                                                                                                : CoercionStrategy.NO_COERCION, context, context.trace);
    }

    @Override
    public JetTypeInfo visitHashQualifiedExpression(JetHashQualifiedExpression expression, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(expression, getClass().getCanonicalName()));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitQualifiedExpression(JetQualifiedExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = context.expressionTypingServices.getCallExpressionResolver();
        return callExpressionResolver.getQualifiedExpressionTypeInfo(expression, context);
    }

    @Override
    public JetTypeInfo visitCallExpression(JetCallExpression expression, ExpressionTypingContext context) {
        CallExpressionResolver callExpressionResolver = context.expressionTypingServices.getCallExpressionResolver();
        return callExpressionResolver.getCallExpressionTypeInfo(expression, NO_RECEIVER, null, context);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context) {
        return visitUnaryExpression(expression, context, false);
    }

    public JetTypeInfo visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context, boolean isStatement) {
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
        JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
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
            JetExpression stubExpression = ExpressionTypingUtils.createStubExpressionOfNecessaryType(baseExpression.getProject(), type, context.trace);
            resolveArrayAccessSetMethod((JetArrayAccessExpression) baseExpression,
                                        stubExpression,
                                        context.replaceExpectedType(NO_EXPECTED_TYPE).replaceBindingTrace(
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
            if (JetTypeChecker.INSTANCE.isSubtypeOf(returnType, KotlinBuiltIns.getInstance().getUnitType())) {
                result = ErrorUtils.createErrorType(KotlinBuiltIns.UNIT_ALIAS.getName());
                context.trace.report(INC_DEC_SHOULD_NOT_RETURN_UNIT.on(operationSign));
            }
            else {
                JetType receiverType = receiver.getType();
                if (!JetTypeChecker.INSTANCE.isSubtypeOf(returnType, receiverType)) {
                    context.trace.report(RESULT_TYPE_MISMATCH.on(operationSign, name.getName(), receiverType, returnType));
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
        return DataFlowUtils.checkType(result, expression, context, dataFlowInfo);
    }

    private JetTypeInfo visitExclExclExpression(@NotNull JetUnaryExpression expression, @NotNull ExpressionTypingContext context) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        assert operationSign.getReferencedNameElementType() == JetTokens.EXCLEXCL;

        JetType expectedType;
        if (context.expectedType != NO_EXPECTED_TYPE) {
            expectedType = TypeUtils.makeNullable(context.expectedType);
        }
        else {
            expectedType = NO_EXPECTED_TYPE;
        }
        JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context.replaceExpectedType(expectedType));
        JetType type = typeInfo.getType();
        if (type == null) {
            return typeInfo;
        }
        DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
        if (isKnownToBeNotNull(baseExpression, context) && !ErrorUtils.isErrorType(type)) {
            context.trace.report(UNNECESSARY_NOT_NULL_ASSERTION.on(operationSign, type));
        }
        else {
            DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(baseExpression, type, context.trace.getBindingContext());
            dataFlowInfo = dataFlowInfo.disequate(value, DataFlowValue.NULL);
        }
        return JetTypeInfo.create(TypeUtils.makeNotNullable(type), dataFlowInfo);
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
        return DataFlowUtils.checkType(typeInfo.getType(), expression, context, typeInfo.getDataFlowInfo());
    }

    private boolean isKnownToBeNotNull(JetExpression expression, ExpressionTypingContext context) {
        JetType type = context.trace.get(EXPRESSION_TYPE, expression);
        assert type != null : "This method is only supposed to be called when the type is not null";
        DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, type, context.trace.getBindingContext());
        return !context.dataFlowInfo.getNullability(dataFlowValue).canBeNull();
    }

    public void checkLValue(BindingTrace trace, JetExpression expression) {
        checkLValue(trace, expression, false);
    }

    private void checkLValue(BindingTrace trace, JetExpression expressionWithParenthesis, boolean canBeThis) {
        JetExpression expression = JetPsiUtil.deparenthesizeWithNoTypeResolution(expressionWithParenthesis);
        if (expression instanceof JetArrayAccessExpression) {
            checkLValue(trace, ((JetArrayAccessExpression) expression).getArrayExpression(), true);
            return;
        }
        if (canBeThis && expression instanceof JetThisExpression) return;
        VariableDescriptor variable = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), expression, true);
        if (variable == null) {
            trace.report(VARIABLE_EXPECTED.on(expression != null ? expression : expressionWithParenthesis));
        }
    }

    @Override
    public JetTypeInfo visitBinaryExpression(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        JetSimpleNameExpression operationSign = expression.getOperationReference();

        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();
        IElementType operationType = operationSign.getReferencedNameElementType();

        JetType result = null;
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (operationType == JetTokens.IDENTIFIER) {
            Name referencedName = operationSign.getReferencedNameAsName();
            JetTypeInfo typeInfo = getTypeInfoForBinaryCall(context.scope, referencedName, context, expression);
            result = typeInfo.getType();
            dataFlowInfo = typeInfo.getDataFlowInfo();
        }
        else if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            JetTypeInfo typeInfo = getTypeInfoForBinaryCall(context.scope, OperatorConventions.BINARY_OPERATION_NAMES.get(operationType),
                                                            context, expression);
            result = typeInfo.getType();
            dataFlowInfo = typeInfo.getDataFlowInfo();
        }
        else if (operationType == JetTokens.EQ) {
            result = visitAssignment(expression, contextWithExpectedType);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, contextWithExpectedType);
        }
        else if (OperatorConventions.COMPARISON_OPERATIONS.contains(operationType)) {
            JetTypeInfo typeInfo = getTypeInfoForBinaryCall(context.scope, OperatorConventions.COMPARE_TO, context, expression);
            dataFlowInfo = typeInfo.getDataFlowInfo();
            JetType compareToReturnType = typeInfo.getType();
            if (compareToReturnType != null && !ErrorUtils.isErrorType(compareToReturnType)) {
                TypeConstructor constructor = compareToReturnType.getConstructor();
                KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
                TypeConstructor intTypeConstructor = builtIns.getInt().getTypeConstructor();
                if (constructor.equals(intTypeConstructor)) {
                    result = builtIns.getBooleanType();
                }
                else {
                    context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
                }
            }
        }
        else {
            JetType booleanType = KotlinBuiltIns.getInstance().getBooleanType();
            if (OperatorConventions.EQUALS_OPERATIONS.contains(operationType)) {
                if (right != null) {
                    ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, context);

                    dataFlowInfo = facade.getTypeInfo(left, context).getDataFlowInfo();
                    ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

                    OverloadResolutionResults<FunctionDescriptor> resolutionResults = resolveFakeCall(
                            contextWithDataFlow, receiver, OperatorConventions.EQUALS, KotlinBuiltIns.getInstance().getNullableAnyType());

                    dataFlowInfo = facade.getTypeInfo(right, contextWithDataFlow).getDataFlowInfo();

                    if (resolutionResults.isSuccess()) {
                        FunctionDescriptor equals = resolutionResults.getResultingCall().getResultingDescriptor();
                        context.trace.record(REFERENCE_TARGET, operationSign, equals);
                        context.trace.record(RESOLVED_CALL, operationSign, resolutionResults.getResultingCall());
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
                result = booleanType;
            }
            else if (operationType == JetTokens.EQEQEQ || operationType == JetTokens.EXCLEQEQEQ) {
                ensureNonemptyIntersectionOfOperandTypes(expression, context);

                // TODO : Check comparison pointlessness
                result = booleanType;
            }
            else if (OperatorConventions.IN_OPERATIONS.contains(operationType)) {
                if (right == null) {
                    result = ErrorUtils.createErrorType("No right argument"); // TODO
                    return JetTypeInfo.create(null, dataFlowInfo);
                }
                JetTypeInfo typeInfo = checkInExpression(expression, expression.getOperationReference(), left, right, context);
                dataFlowInfo = typeInfo.getDataFlowInfo();
                result = typeInfo.getType();
            }
            else if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationType)) {
                JetTypeInfo leftTypeInfo = facade.getTypeInfo(left, context);
                JetType leftType = leftTypeInfo.getType();
                dataFlowInfo = leftTypeInfo.getDataFlowInfo();

                WritableScopeImpl leftScope = newWritableScopeImpl(context, "Left scope of && or ||");
                // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
                DataFlowInfo flowInfoLeft =
                        DataFlowUtils.extractDataFlowInfoFromCondition(left, operationType == JetTokens.ANDAND, context).and(dataFlowInfo);
                WritableScopeImpl rightScope = operationType == JetTokens.ANDAND
                                               ? leftScope
                                               : newWritableScopeImpl(context, "Right scope of && or ||");
                JetType rightType = right == null
                                    ? null
                                    : facade.getTypeInfo(right, context.replaceDataFlowInfo(flowInfoLeft).replaceScope(rightScope)).getType();
                if (leftType != null && !isBoolean(leftType)) {
                    context.trace.report(TYPE_MISMATCH.on(left, booleanType, leftType));
                }
                if (rightType != null && !isBoolean(rightType)) {
                    context.trace.report(TYPE_MISMATCH.on(right, booleanType, rightType));
                }
                result = booleanType;
            }
            else if (operationType == JetTokens.ELVIS) {
                JetTypeInfo leftTypeInfo = facade.getTypeInfo(left, context);
                JetType leftType = leftTypeInfo.getType();
                dataFlowInfo = leftTypeInfo.getDataFlowInfo();

                ExpressionTypingContext newContext = contextWithExpectedType.replaceDataFlowInfo(dataFlowInfo).replaceScope(context.scope);
                JetType rightType = right == null ? null : facade.getTypeInfo(right, newContext).getType();
                if (leftType != null) {
                    if (!leftType.isNullable()) {
                        context.trace.report(USELESS_ELVIS.on(left, leftType));
                    }
                    if (rightType != null) {
                        DataFlowUtils.checkType(TypeUtils.makeNullableAsSpecified(leftType, rightType.isNullable()), left, contextWithExpectedType);
                        return JetTypeInfo.create(TypeUtils.makeNullableAsSpecified(
                                CommonSupertypes.commonSupertype(Arrays.asList(leftType, rightType)), rightType.isNullable()),
                                                  dataFlowInfo);
                    }
                }
            }
            else {
                context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
            }
        }
        return DataFlowUtils.checkType(result, expression, contextWithExpectedType, dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo checkInExpression(
            JetElement callElement,
            @NotNull JetSimpleNameExpression operationSign,
            @Nullable JetExpression left,
            @NotNull JetExpression right,
            ExpressionTypingContext context
    ) {
        ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
        DataFlowInfo dataFlowInfo = facade.getTypeInfo(right, contextWithNoExpectedType).getDataFlowInfo();

        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, contextWithNoExpectedType);
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);

        OverloadResolutionResults<FunctionDescriptor> resolutionResult = contextWithDataFlow.resolveCallWithGivenName(
                CallMaker.makeCallWithExpressions(callElement, receiver, null, operationSign, Collections.singletonList(left)),
                operationSign,
                OperatorConventions.CONTAINS);
        JetType containsType = OverloadResolutionResultsUtil.getResultType(resolutionResult);
        ensureBooleanResult(operationSign, OperatorConventions.CONTAINS, containsType, context);

        if (left != null) {
            dataFlowInfo = facade.getTypeInfo(left, contextWithDataFlow).getDataFlowInfo().and(dataFlowInfo);
        }

        return JetTypeInfo.create(resolutionResult.isSuccess() ? KotlinBuiltIns.getInstance().getBooleanType() : null, dataFlowInfo);
    }

    private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression, ExpressionTypingContext context) {
        JetExpression left = expression.getLeft();
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
        if (type == null || ErrorUtils.isErrorType(type)) return;

        DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(expr, type, context.trace.getBindingContext());
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

    protected JetType visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    protected JetType visitAssignment(JetBinaryExpression expression, ExpressionTypingContext context) {
        return assignmentIsNotAnExpressionError(expression, context);
    }

    private JetType assignmentIsNotAnExpressionError(JetBinaryExpression expression, ExpressionTypingContext context) {
        facade.checkStatementType(expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        context.trace.report(ASSIGNMENT_IN_EXPRESSION_CONTEXT.on(expression));
        return null;
    }

    @Override
    public JetTypeInfo visitArrayAccessExpression(JetArrayAccessExpression expression, ExpressionTypingContext context) {
        JetTypeInfo typeInfo = resolveArrayAccessGetMethod(expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        return DataFlowUtils.checkType(typeInfo.getType(), expression, context, typeInfo.getDataFlowInfo());
    }

    @NotNull
    public JetTypeInfo getTypeInfoForBinaryCall(
            JetScope scope,
            Name name,
            ExpressionTypingContext contextWithOldScope,
            JetBinaryExpression binaryExpression
    ) {
        ExpressionTypingContext context = contextWithOldScope.replaceScope(scope);
        JetExpression left = binaryExpression.getLeft();
        DataFlowInfo dataFlowInfo = facade.getTypeInfo(left, context).getDataFlowInfo();

        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, left, context);
        ExpressionTypingContext contextWithDataFlow = context.replaceDataFlowInfo(dataFlowInfo);
        OverloadResolutionResults<FunctionDescriptor> resolutionResults =
                getResolutionResultsForBinaryCall(scope, name, contextWithDataFlow, binaryExpression, receiver);

        JetExpression right = binaryExpression.getRight();
        if (right != null) {
            dataFlowInfo = facade.getTypeInfo(right, contextWithDataFlow).getDataFlowInfo();
        }

        return JetTypeInfo.create(OverloadResolutionResultsUtil.getResultType(resolutionResults), dataFlowInfo);
    }

    @NotNull
        /*package*/ OverloadResolutionResults<FunctionDescriptor> getResolutionResultsForBinaryCall(JetScope scope, Name name, ExpressionTypingContext context, JetBinaryExpression binaryExpression, ExpressionReceiver receiver) {
//        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, binaryExpression.getLeft(), context.replaceScope(scope));
        return context.replaceScope(scope).resolveCallWithGivenName(
                CallMaker.makeCall(receiver, binaryExpression),
                binaryExpression.getOperationReference(),
                name
        );
    }

    @Override
    public JetTypeInfo visitDeclaration(JetDeclaration dcl, ExpressionTypingContext context) {
        context.trace.report(DECLARATION_IN_ILLEGAL_CONTEXT.on(dcl));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitRootNamespaceExpression(JetRootNamespaceExpression expression, ExpressionTypingContext context) {
        if (context.expressionPosition == ExpressionPosition.LHS_OF_DOT) {
            return DataFlowUtils.checkType(JetModuleUtil.getRootNamespaceType(expression), expression, context, context.dataFlowInfo);
        }
        context.trace.report(NAMESPACE_IS_NOT_AN_EXPRESSION.on(expression));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }


    @Override
    public JetTypeInfo visitStringTemplateExpression(JetStringTemplateExpression expression, ExpressionTypingContext contextWithExpectedType) {
        final ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        final StringBuilder builder = new StringBuilder();
        final CompileTimeConstant<?>[] value = new CompileTimeConstant<?>[1];
        final DataFlowInfo[] dataFlowInfo = new DataFlowInfo[1];
        dataFlowInfo[0] = context.dataFlowInfo;

        for (JetStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(new JetVisitorVoid() {

                @Override
                public void visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry) {
                    JetExpression entryExpression = entry.getExpression();
                    if (entryExpression != null) {
                        JetTypeInfo typeInfo = facade.getTypeInfo(entryExpression, context.replaceDataFlowInfo(dataFlowInfo[0]));
                        dataFlowInfo[0] = typeInfo.getDataFlowInfo();
                    }
                    value[0] = CompileTimeConstantResolver.OUT_OF_RANGE;
                }

                @Override
                public void visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry) {
                    builder.append(entry.getText());
                }

                @Override
                public void visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry) {
                    String text = entry.getText();

                    CompileTimeConstant<?> character = CompileTimeConstantResolver.escapedStringToCharValue(text);
                    if (character instanceof ErrorValue) {
                        context.trace.report(ILLEGAL_ESCAPE_SEQUENCE.on(entry));
                        value[0] = CompileTimeConstantResolver.OUT_OF_RANGE;
                    }
                    else {
                        builder.append(((CharValue) character).getValue());
                    }
                }
            });
        }
        if (value[0] != CompileTimeConstantResolver.OUT_OF_RANGE) {
            context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new StringValue(builder.toString()));
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getStringType(), expression, contextWithExpectedType, dataFlowInfo[0]);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(JetAnnotatedExpression expression, ExpressionTypingContext data) {
        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) {
            return JetTypeInfo.create(null, data.dataFlowInfo);
        }
        return facade.getTypeInfo(baseExpression, data);
    }

    @Override
    public JetTypeInfo visitJetElement(JetElement element, ExpressionTypingContext context) {
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
        JetTypeInfo arrayTypeInfo = facade.getTypeInfo(arrayAccessExpression.getArrayExpression(), oldContext);
        JetType arrayType = arrayTypeInfo.getType();
        if (arrayType == null) return arrayTypeInfo;

        DataFlowInfo dataFlowInfo = arrayTypeInfo.getDataFlowInfo();
        ExpressionTypingContext context = oldContext.replaceDataFlowInfo(dataFlowInfo);
        ExpressionReceiver receiver = new ExpressionReceiver(arrayAccessExpression.getArrayExpression(), arrayType);
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

        if (!functionResults.isSuccess()) {
            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        traceForResolveResult.record(isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.getResultingCall());
        return JetTypeInfo.create(functionResults.getResultingDescriptor().getReturnType(), dataFlowInfo);
    }
}
