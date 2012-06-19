/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.jet.lang.types.expressions.OperatorConventions.*;

/**
 * @author abreslav
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class BasicExpressionTypingVisitor extends ExpressionTypingVisitor {
    protected BasicExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitSimpleNameExpression(JetSimpleNameExpression expression, ExpressionTypingContext context) {
        // TODO : other members
        // TODO : type substitutions???
        JetTypeInfo typeInfo = getSelectorReturnTypeInfo(NO_RECEIVER, null, expression, context);
        JetType type = DataFlowUtils.checkType(typeInfo.getType(), expression, context);
        ExpressionTypingUtils.checkWrappingInRef(expression, context);
        return JetTypeInfo.create(type, typeInfo.getDataFlowInfo()); // TODO : Extensions to this
    }

    @Nullable
    private JetType lookupNamespaceOrClassObject(JetSimpleNameExpression expression, Name referencedName, ExpressionTypingContext context) {
        ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
        if (classifier != null) {
            JetType classObjectType = classifier.getClassObjectType();
            JetType result = null;
            if (classObjectType != null) {
                if (context.namespacesAllowed || classifier.isClassObjectAValue()) {
                    result = classObjectType;
                }
                else {
                    context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
                }
                context.trace.record(REFERENCE_TARGET, expression, classifier);
                //if (result == null) {
                //    return ErrorUtils.createErrorType("No class object in " + expression.getReferencedName());
                //}
                return DataFlowUtils.checkType(result, expression, context);
            }
        }
        JetType[] result = new JetType[1];
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
        if (furtherNameLookup(expression, referencedName, result, context.replaceBindingTrace(temporaryTrace))) {
            temporaryTrace.commit();
            return DataFlowUtils.checkType(result[0], expression, context);
        }
        // To report NO_CLASS_OBJECT when no namespace found
        if (classifier != null) {
            context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
            context.trace.record(REFERENCE_TARGET, expression, classifier);
            return classifier.getDefaultType();
        }
        temporaryTrace.commit();
        return result[0];
    }

    protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull Name referencedName, @NotNull JetType[] result, ExpressionTypingContext context) {
        if (context.namespacesAllowed) {
            result[0] = lookupNamespaceType(expression, referencedName, context);
            return result[0] != null;
        }
        NamespaceType namespaceType = lookupNamespaceType(expression, referencedName, context);
        if (namespaceType != null) {
            context.trace.report(EXPRESSION_EXPECTED_NAMESPACE_FOUND.on(expression));
            result[0] = ErrorUtils.createErrorType("Type for " + referencedName);
        }
        return false;
    }

    @Nullable
    protected NamespaceType lookupNamespaceType(@NotNull JetSimpleNameExpression expression, @NotNull Name referencedName, ExpressionTypingContext context) {
        NamespaceDescriptor namespace = context.scope.getNamespace(referencedName);
        if (namespace == null) {
            return null;
        }
        context.trace.record(REFERENCE_TARGET, expression, namespace);
        return namespace.getNamespaceType();
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
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();
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
            return DataFlowUtils.checkType(value.getType(standardLibrary), expression, context, context.dataFlowInfo);
        }
    }

    @Override
    public JetTypeInfo visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext context) {
        JetTypeReference right = expression.getRight();
        JetType result = null;
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        if (right != null) {
            JetType targetType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, right, context.trace, true);

            boolean tryWithNoExpectedType = true;
            if (isTypeFlexible(expression.getLeft()) || expression.getOperationSign().getReferencedNameElementType() == JetTokens.COLON) {
                TemporaryBindingTrace temporaryTraceWithExpectedType = TemporaryBindingTrace.create(context.trace);
                ExpressionTypingContext contextWithTemporaryTrace = context.replaceBindingTrace(temporaryTraceWithExpectedType).replaceExpectedType(targetType);
                JetTypeInfo typeInfo = facade.getTypeInfo(expression.getLeft(), contextWithTemporaryTrace);
                if (typeInfo.getType() != null && checkBinaryWithTypeRHS(expression, contextWithTemporaryTrace, targetType, typeInfo.getType())) {
                    temporaryTraceWithExpectedType.commit();
                    dataFlowInfo = typeInfo.getDataFlowInfo();
                    tryWithNoExpectedType = false;
                }
            }

            if (tryWithNoExpectedType) {
                ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
                JetTypeInfo typeInfo = facade.getTypeInfo(expression.getLeft(), contextWithNoExpectedType);
                if (typeInfo.getType() != null) {
                    checkBinaryWithTypeRHS(expression, contextWithNoExpectedType, targetType, typeInfo.getType());
                    dataFlowInfo = typeInfo.getDataFlowInfo();
                }
            }

            IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
            result = operationType == JetTokens.AS_SAFE ? TypeUtils.makeNullable(targetType) : targetType;
        }
        else {
            dataFlowInfo = facade.getTypeInfo(expression.getLeft(), context.replaceExpectedType(NO_EXPECTED_TYPE)).getDataFlowInfo();
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
     * Check if assignment from ActualType to TargetType is erased.
     * It is an error in "is" statement and warning in "as".
     */
    public static boolean isCastErased(JetType actualType, JetType targetType, JetTypeChecker typeChecker) {

        if (!(targetType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            // TODO: what if it is TypeParameterDescriptor?
            return false;
        }

        // do not crash on error types
        if (ErrorUtils.isErrorType(actualType) || ErrorUtils.isErrorType(targetType)) {
            return false;
        }

        {
            Multimap<TypeConstructor, TypeProjection> typeSubstitutionMap =
                    SubstitutionUtils.buildDeepSubstitutionMultimap(targetType);

            for (int i = 0; i < actualType.getConstructor().getParameters().size(); ++i) {
                TypeProjection actualTypeParameter = actualType.getArguments().get(i);
                TypeParameterDescriptor subjectTypeParameterDescriptor = actualType.getConstructor().getParameters().get(i);

                if (subjectTypeParameterDescriptor.isReified()) {
                    continue;
                }

                Collection<TypeProjection> subst = typeSubstitutionMap.get(subjectTypeParameterDescriptor.getTypeConstructor());
                for (TypeProjection proj : subst) {
                    //if (!proj.getType().equals(actualTypeParameter.getType())) {
                    if (!typeChecker.isSubtypeOf(actualTypeParameter.getType(), proj.getType())) {
                        return true;
                    }
                }
            }
        }

        {
            JetType targetTypeClerared = TypeUtils.makeUnsubstitutedType(
                    (ClassDescriptor) targetType.getConstructor().getDeclarationDescriptor(), null);

            Multimap<TypeConstructor, TypeProjection> clearTypeSubstitutionMap =
                    SubstitutionUtils.buildDeepSubstitutionMultimap(targetTypeClerared);

            Set<JetType> clearSubstituted = new HashSet<JetType>();

            for (int i = 0; i < actualType.getConstructor().getParameters().size(); ++i) {
                TypeParameterDescriptor subjectTypeParameterDescriptor = actualType.getConstructor().getParameters().get(i);

                Collection<TypeProjection> subst = clearTypeSubstitutionMap.get(subjectTypeParameterDescriptor.getTypeConstructor());
                for (TypeProjection proj : subst) {
                    clearSubstituted.add(proj.getType());
                }
            }

            for (int i = 0; i < targetType.getConstructor().getParameters().size(); ++i) {
                TypeParameterDescriptor typeParameter = targetType.getConstructor().getParameters().get(i);
                TypeProjection typeProjection = targetType.getArguments().get(i);

                if (typeParameter.isReified()) {
                    continue;
                }

                // "is List<*>"
                if (typeProjection.equals(SubstitutionUtils.makeStarProjection(typeParameter))) {
                    continue;
                }

                // if parameter is mapped to nothing then it is erased
                if (!clearSubstituted.contains(typeParameter.getDefaultType())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JetTypeInfo visitTupleExpression(JetTupleExpression expression, ExpressionTypingContext context) {
        List<JetExpression> entries = expression.getEntries();
        List<JetType> types = new ArrayList<JetType>();
        for (JetExpression entry : entries) {
            types.add(context.expressionTypingServices.safeGetType(context.scope, entry, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace)); // TODO
        }
        if (context.expectedType != NO_EXPECTED_TYPE && JetStandardClasses.isTupleType(context.expectedType)) {
            List<JetType> enrichedTypes = checkArgumentTypes(types, entries, context.expectedType.getArguments(), context);
            if (enrichedTypes != types) {
                return JetTypeInfo.create(JetStandardClasses.getTupleType(enrichedTypes), context.dataFlowInfo);
            }
        }
        // TODO : labels
        return DataFlowUtils.checkType(JetStandardClasses.getTupleType(types), expression, context, context.dataFlowInfo);
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
        ReceiverDescriptor thisReceiver = resolveToReceiver(expression, context, false);

        if (thisReceiver != null) {
            if (!thisReceiver.exists()) {
                context.trace.report(NO_THIS.on(expression));
            }
            else {
                result = thisReceiver.getType();
                context.trace.record(BindingContext.EXPRESSION_TYPE, expression.getInstanceReference(), result);
            }
        }
        return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitSuperExpression(JetSuperExpression expression, ExpressionTypingContext context) {
        if (!context.namespacesAllowed) {
            context.trace.report(SUPER_IS_NOT_AN_EXPRESSION.on(expression, expression.getText()));
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        JetType result = null;

        ReceiverDescriptor thisReceiver = resolveToReceiver(expression, context, true);
        if (thisReceiver == null) return JetTypeInfo.create(null, context.dataFlowInfo);

        if (!thisReceiver.exists()) {
            context.trace.report(SUPER_NOT_AVAILABLE.on(expression));
        }
        else {
            JetType thisType = thisReceiver.getType();
            Collection<? extends JetType> supertypes = thisType.getConstructor().getSupertypes();
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
                                   ? JetStandardClasses.getAnyType()
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
        }
        return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
    }

    @Nullable // No class receivers
    private ReceiverDescriptor resolveToReceiver(JetLabelQualifiedInstanceExpression expression, ExpressionTypingContext context, boolean onlyClassReceivers) {
        ReceiverDescriptor thisReceiver = null;
        String labelName = expression.getLabelName();
        if (labelName != null) {
            thisReceiver = context.labelResolver.resolveThisLabel(
                    expression.getInstanceReference(), expression.getTargetLabel(), context, thisReceiver, new LabelName(labelName));
        }
        else {
            if (onlyClassReceivers) {
                List<ReceiverDescriptor> receivers = Lists.newArrayList();
                context.scope.getImplicitReceiversHierarchy(receivers);
                for (ReceiverDescriptor receiver : receivers) {
                    if (receiver instanceof ClassReceiver) {
                        thisReceiver = receiver;
                        break;
                    }
                }
            }
            else {
                thisReceiver = context.scope.getImplicitReceiver();
            }
            if (thisReceiver instanceof ThisReceiverDescriptor) {
                context.trace.record(REFERENCE_TARGET, expression.getInstanceReference(), ((ThisReceiverDescriptor) thisReceiver).getDeclarationDescriptor());
            }
        }
        return thisReceiver;
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
        // TODO : functions as values
        JetExpression selectorExpression = expression.getSelectorExpression();
        JetExpression receiverExpression = expression.getReceiverExpression();
        ExpressionTypingContext contextWithNoExpectedType = context.replaceExpectedType(NO_EXPECTED_TYPE);
        JetTypeInfo receiverTypeInfo = facade.getTypeInfo(receiverExpression, contextWithNoExpectedType.replaceNamespacesAllowed(true));
        JetType receiverType = receiverTypeInfo.getType();
        if (selectorExpression == null) return JetTypeInfo.create(null, context.dataFlowInfo);
        if (receiverType == null) receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

        context = context.replaceDataFlowInfo(receiverTypeInfo.getDataFlowInfo());

        if (selectorExpression instanceof JetSimpleNameExpression) {
            propagateConstantValues(expression, context, (JetSimpleNameExpression) selectorExpression);
        }

        JetTypeInfo selectorReturnTypeInfo = getSelectorReturnTypeInfo(new ExpressionReceiver(receiverExpression, receiverType), expression.getOperationTokenNode(), selectorExpression, context);
        JetType selectorReturnType = selectorReturnTypeInfo.getType();

        //TODO move further
        if (expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
            if (selectorReturnType != null && !selectorReturnType.isNullable() && !JetStandardClasses.isUnit(selectorReturnType)) {
                if (receiverType.isNullable()) {
                    selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                }
            }
        }

        // TODO : this is suspicious: remove this code?
        if (selectorReturnType != null) {
            context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, selectorReturnType);
        }
        return DataFlowUtils.checkType(selectorReturnType, expression, context, selectorReturnTypeInfo.getDataFlowInfo());
    }

    private void propagateConstantValues(JetQualifiedExpression expression, ExpressionTypingContext context, JetSimpleNameExpression selectorExpression) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        CompileTimeConstant<?> receiverValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, receiverExpression);
        CompileTimeConstant<?> wholeExpressionValue = context.trace.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);
        DeclarationDescriptor declarationDescriptor = context.trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, selectorExpression);
        if (wholeExpressionValue == null && receiverValue != null && !(receiverValue instanceof ErrorValue) && receiverValue.getValue() instanceof Number
            && JetStandardLibrary.getInstance().getNumber() == declarationDescriptor) {
            Number value = (Number) receiverValue.getValue();
            Name referencedName = selectorExpression.getReferencedNameAsName();
            if (OperatorConventions.NUMBER_CONVERSIONS.contains(referencedName)) {
                if (DOUBLE.equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new DoubleValue(value.doubleValue()));
                }
                else if (FLOAT.equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new FloatValue(value.floatValue()));
                }
                else if (LONG.equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new LongValue(value.longValue()));
                }
                else if (SHORT.equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ShortValue(value.shortValue()));
                }
                else if (BYTE.equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new ByteValue(value.byteValue()));
                }
                else if (INT.equals(referencedName)) {
                    context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, new IntValue(value.intValue()));
                }
            }
        }
    }

    @Nullable
    private FunctionDescriptor getFunctionDescriptor(@NotNull Call call, @NotNull JetExpression callExpression, @NotNull ReceiverDescriptor receiver,
            @NotNull ExpressionTypingContext context, @NotNull boolean[] result) {

        OverloadResolutionResults<FunctionDescriptor> results = context.resolveFunctionCall(call);
        if (!results.isNothing()) {
            checkSuper(receiver, results, context.trace, callExpression);
            result[0] = true;
            return results.isSingleResult() ? results.getResultingDescriptor() : null;
        }
        result[0] = false;
        return null;
    }

    @Nullable
    private JetType getVariableType(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverDescriptor receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context, @NotNull boolean[] result) {

        TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace);
        OverloadResolutionResults<VariableDescriptor> resolutionResult = context.replaceBindingTrace(traceForVariable).resolveSimpleProperty(receiver, callOperationNode, nameExpression);
        if (!resolutionResult.isNothing()) {
            traceForVariable.commit();
            checkSuper(receiver, resolutionResult, context.trace, nameExpression);
            result[0] = true;
            return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
        }

        ExpressionTypingContext newContext = receiver.exists()
                                             ? context.replaceScope(receiver.getType().getMemberScope())
                                             : context;
        TemporaryBindingTrace traceForNamespaceOrClassObject = TemporaryBindingTrace.create(context.trace);
        JetType jetType = lookupNamespaceOrClassObject(nameExpression, nameExpression.getReferencedNameAsName(), newContext.replaceBindingTrace(traceForNamespaceOrClassObject));
        if (jetType != null) {
            traceForNamespaceOrClassObject.commit();

            // Uncommitted changes in temp context
            context.trace.record(RESOLUTION_SCOPE, nameExpression, context.scope);
            if (context.dataFlowInfo.hasTypeInfoConstraints()) {
                context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, nameExpression, context.dataFlowInfo);
            }
            result[0] = true;
            return jetType;
        }
        result[0] = false;
        return null;
    }

    @NotNull
    public JetTypeInfo getSelectorReturnTypeInfo(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetExpression selectorExpression, @NotNull ExpressionTypingContext context) {
        if (selectorExpression instanceof JetCallExpression) {
            return getCallExpressionTypeInfo((JetCallExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else if (selectorExpression instanceof JetSimpleNameExpression) {
            return getSimpleNameExpressionTypeInfo((JetSimpleNameExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else if (selectorExpression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) selectorExpression;
            JetExpression newReceiverExpression = qualifiedExpression.getReceiverExpression();
            JetTypeInfo newReceiverTypeInfo = getSelectorReturnTypeInfo(receiver, callOperationNode, newReceiverExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
            JetType newReceiverType = newReceiverTypeInfo.getType();
            DataFlowInfo newReceiverDataFlowInfo = newReceiverTypeInfo.getDataFlowInfo();
            JetExpression newSelectorExpression = qualifiedExpression.getSelectorExpression();
            if (newReceiverType != null && newSelectorExpression != null) {
                return getSelectorReturnTypeInfo(new ExpressionReceiver(newReceiverExpression, newReceiverType), qualifiedExpression.getOperationTokenNode(), newSelectorExpression, context.replaceDataFlowInfo(newReceiverDataFlowInfo));
            }
        }
        else {
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
        }
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @NotNull
    private JetTypeInfo getSimpleNameExpressionTypeInfo(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverDescriptor receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context) {

        boolean[] result = new boolean[1];

        TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace);
        JetType type = getVariableType(nameExpression, receiver, callOperationNode, context.replaceBindingTrace(traceForVariable), result);
        if (result[0]) {
            traceForVariable.commit();
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
        TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace);
        FunctionDescriptor functionDescriptor = getFunctionDescriptor(call, nameExpression, receiver, context, result);
        if (result[0]) {
            traceForFunction.commit();
            boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
            context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters));
            type = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        traceForVariable.commit();
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @NotNull
    private JetTypeInfo getCallExpressionTypeInfo(@NotNull JetCallExpression callExpression, @NotNull ReceiverDescriptor receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context) {

        boolean[] result = new boolean[1];
        Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

        TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace);
        FunctionDescriptor functionDescriptor = getFunctionDescriptor(call, callExpression, receiver,
                                                                      context.replaceBindingTrace(traceForFunction), result);
        if (result[0]) {
            traceForFunction.commit();
            if (callExpression.getValueArgumentList() == null && callExpression.getFunctionLiteralArguments().isEmpty()) {
                // there are only type arguments
                boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters));
            }
            if (functionDescriptor == null) {
                return JetTypeInfo.create(null, context.dataFlowInfo);
            }
            JetType type = functionDescriptor.getReturnType();

            DataFlowInfo dataFlowInfo = context.dataFlowInfo;
            JetValueArgumentList argumentList = callExpression.getValueArgumentList();
            if (argumentList != null) {
                for (JetValueArgument argument : argumentList.getArguments()) {
                    JetExpression expression = argument.getArgumentExpression();
                    if (expression != null) {
                        dataFlowInfo = dataFlowInfo.and(facade.getTypeInfo(expression, context.replaceDataFlowInfo(dataFlowInfo)).getDataFlowInfo());
                    }
                }
            }
            return JetTypeInfo.create(type, dataFlowInfo);
        }

        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression instanceof JetSimpleNameExpression && callExpression.getTypeArgumentList() == null) {
            TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace);
            JetType type = getVariableType((JetSimpleNameExpression) calleeExpression, receiver, callOperationNode,
                                           context.replaceBindingTrace(traceForVariable), result);
            if (result[0]) {
                traceForVariable.commit();
                context.trace.report(FUNCTION_EXPECTED.on((JetReferenceExpression) calleeExpression, calleeExpression,
                                                          type != null ? type : ErrorUtils.createErrorType("")));
                return JetTypeInfo.create(null, context.dataFlowInfo);
            }
        }
        traceForFunction.commit();
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    private static void checkSuper(@NotNull ReceiverDescriptor receiverDescriptor, @NotNull OverloadResolutionResults<? extends CallableDescriptor> results,
            @NotNull BindingTrace trace, @NotNull JetExpression expression) {
        if (!results.isSingleResult()) return;
        if (!(receiverDescriptor instanceof ExpressionReceiver)) return;
        JetExpression receiver = ((ExpressionReceiver) receiverDescriptor).getExpression();
        CallableDescriptor descriptor = results.getResultingDescriptor();
        if (receiver instanceof JetSuperExpression && descriptor instanceof MemberDescriptor) {
            if (((MemberDescriptor) descriptor).getModality() == Modality.ABSTRACT) {
                trace.report(ABSTRACT_SUPER_CALL.on(expression));
            }
        }
    }

    @Override
    public JetTypeInfo visitCallExpression(JetCallExpression expression, ExpressionTypingContext context) {
        JetTypeInfo expressionTypeInfo = getCallExpressionTypeInfo(expression, NO_RECEIVER, null, context);
        return DataFlowUtils.checkType(expressionTypeInfo.getType(), expression, context, expressionTypeInfo.getDataFlowInfo());
    }

    @Override
    public JetTypeInfo visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context) {
        return visitUnaryExpression(expression, context, false);
    }

    public JetTypeInfo visitUnaryExpression(JetUnaryExpression expression, ExpressionTypingContext context, boolean isStatement) {
        JetExpression baseExpression = expression.getBaseExpression();
        if (baseExpression == null) return JetTypeInfo.create(null, context.dataFlowInfo);

        JetSimpleNameExpression operationSign = expression.getOperationReference();

        // If it's a labeled expression
        if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
            String referencedName = operationSign.getReferencedName();
            referencedName = referencedName == null ? " <?>" : referencedName;
            context.labelResolver.enterLabeledElement(new LabelName(referencedName.substring(1)), baseExpression);
            // TODO : Some processing for the label?
            JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context, isStatement);
            context.labelResolver.exitLabeledElement(baseExpression);
            return DataFlowUtils.checkType(typeInfo.getType(), expression, context, typeInfo.getDataFlowInfo());
        }

        IElementType operationType = operationSign.getReferencedNameElementType();

        // Type check the base expression
        JetTypeInfo typeInfo = facade.getTypeInfo(baseExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        JetType type = typeInfo.getType();
        if (type == null) {
            return typeInfo;
        }
        DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();

        // Special case for expr!!
        if (operationType == JetTokens.EXCLEXCL) {
            if (isKnownToBeNotNull(baseExpression, context)) {
                context.trace.report(UNNECESSARY_NOT_NULL_ASSERTION.on(operationSign, type));
            }
            else {
                DataFlowValue value = DataFlowValueFactory.INSTANCE.createDataFlowValue(baseExpression, type, context.trace.getBindingContext());
                dataFlowInfo = dataFlowInfo.disequate(value, DataFlowValue.NULL);
            }
            return DataFlowUtils.checkType(TypeUtils.makeNotNullable(type), expression, context, dataFlowInfo);
        }

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
                                        context.replaceExpectedType(NO_EXPECTED_TYPE).replaceBindingTrace(TemporaryBindingTrace.create(context.trace)),
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
            if (JetTypeChecker.INSTANCE.isSubtypeOf(returnType, JetStandardClasses.getUnitType())) {
                result = ErrorUtils.createErrorType(JetStandardClasses.UNIT_ALIAS.getName());
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

    private boolean isKnownToBeNotNull(JetExpression expression, ExpressionTypingContext context) {
        JetType type = context.trace.get(EXPRESSION_TYPE, expression);
        assert type != null : "This method is only supposed to be called when the type is not null";
        if (!type.isNullable()) return true;
        List<JetType> possibleTypes = context.dataFlowInfo
            .getPossibleTypes(DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, type, context.trace.getBindingContext()));
        for (JetType possibleType : possibleTypes) {
            if (!possibleType.isNullable()) {
                return true;
            }
        }
        return false;
    }

    public void checkLValue(BindingTrace trace, JetExpression expression) {
        checkLValue(trace, expression, false);
    }

    private void checkLValue(BindingTrace trace, JetExpression expressionWithParenthesis, boolean canBeThis) {
        JetExpression expression = JetPsiUtil.deparenthesize(expressionWithParenthesis);
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
    public JetTypeInfo visitIdeTemplateExpression(JetIdeTemplateExpression expression, ExpressionTypingContext context) {
        context.trace.report(UNRESOLVED_IDE_TEMPLATE.on(expression, ObjectUtils.notNull(expression.getText(), "<no name>")));
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitBinaryExpression(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        JetSimpleNameExpression operationSign = expression.getOperationReference();

        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();

        JetType result = null;
        IElementType operationType = operationSign.getReferencedNameElementType();
        if (operationType == JetTokens.IDENTIFIER) {
            Name referencedName = operationSign.getReferencedNameAsName();
            if (referencedName != null) {
                result = getTypeForBinaryCall(context.scope, referencedName, context, expression);
            }
        }
        else if (OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)) {
            result = getTypeForBinaryCall(context.scope, OperatorConventions.BINARY_OPERATION_NAMES.get(operationType), context, expression);
        }
        else if (operationType == JetTokens.EQ) {
            result = visitAssignment(expression, contextWithExpectedType);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, contextWithExpectedType);
        }
        else if (OperatorConventions.COMPARISON_OPERATIONS.contains(operationType)) {
            JetType compareToReturnType = getTypeForBinaryCall(context.scope, Name.identifier("compareTo"), context, expression);
            if (compareToReturnType != null) {
                TypeConstructor constructor = compareToReturnType.getConstructor();
                JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();
                TypeConstructor intTypeConstructor = standardLibrary.getInt().getTypeConstructor();
                if (constructor.equals(intTypeConstructor)) {
                    result = standardLibrary.getBooleanType();
                }
                else {
                    context.trace.report(COMPARE_TO_TYPE_MISMATCH.on(operationSign, compareToReturnType));
                }
            }
        }
        else {
            JetType booleanType = JetStandardLibrary.getInstance().getBooleanType();
            if (OperatorConventions.EQUALS_OPERATIONS.contains(operationType)) {
                Name name = Name.identifier("equals");
                if (right != null) {
                    ExpressionReceiver receiver = ExpressionTypingUtils.safeGetExpressionReceiver(facade, left, context.replaceScope(context.scope));
                    OverloadResolutionResults<FunctionDescriptor> resolutionResults = context.resolveExactSignature(
                            receiver, name,
                            Collections.singletonList(JetStandardClasses.getNullableAnyType()));
                    if (resolutionResults.isSuccess()) {
                        FunctionDescriptor equals = resolutionResults.getResultingCall().getResultingDescriptor();
                        context.trace.record(REFERENCE_TARGET, operationSign, equals);
                        if (ensureBooleanResult(operationSign, name, equals.getReturnType(), context)) {
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
                    return JetTypeInfo.create(null, context.dataFlowInfo);
                }
                checkInExpression(expression, expression.getOperationReference(), expression.getLeft(), expression.getRight(), context);
                result = booleanType;
            }
            else if (operationType == JetTokens.ANDAND || operationType == JetTokens.OROR) {
                JetType leftType = facade.getTypeInfo(left, context.replaceScope(context.scope)).getType();
                WritableScopeImpl leftScope = newWritableScopeImpl(context, "Left scope of && or ||");
                DataFlowInfo flowInfoLeft = DataFlowUtils.extractDataFlowInfoFromCondition(left, operationType == JetTokens.ANDAND, leftScope, context);  // TODO: This gets computed twice: here and in extractDataFlowInfoFromCondition() for the whole condition
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
                JetType leftType = facade.getTypeInfo(left, context.replaceScope(context.scope)).getType();
                JetType rightType = right == null
                                    ? null
                                    : facade.getTypeInfo(right, contextWithExpectedType.replaceScope(context.scope)).getType();
                if (leftType != null) {
                    if (!leftType.isNullable()) {
                        context.trace.report(USELESS_ELVIS.on(left, leftType));
                    }
                    if (rightType != null) {
                        DataFlowUtils.checkType(TypeUtils.makeNullableAsSpecified(leftType, rightType.isNullable()), left, contextWithExpectedType);
                        return JetTypeInfo.create(TypeUtils.makeNullableAsSpecified(
                                CommonSupertypes.commonSupertype(Arrays.asList(leftType, rightType)), rightType.isNullable()),
                                                  context.dataFlowInfo);
                    }
                }
            }
            else {
                context.trace.report(UNSUPPORTED.on(operationSign, "Unknown operation"));
            }
        }
        return DataFlowUtils.checkType(result, expression, contextWithExpectedType, context.dataFlowInfo);
    }

    public boolean checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @Nullable JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context) {
        Name name = Name.identifier("contains");
        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, right, context.replaceExpectedType(NO_EXPECTED_TYPE));
        OverloadResolutionResults<FunctionDescriptor> resolutionResult = context.resolveCallWithGivenName(
                CallMaker.makeCallWithExpressions(callElement, receiver, null, operationSign, Collections.singletonList(left)),
                operationSign,
                name);
        JetType containsType = OverloadResolutionResultsUtil.getResultType(resolutionResult);
        ensureBooleanResult(operationSign, name, containsType, context);
        return resolutionResult.isSuccess();
    }

    private void ensureNonemptyIntersectionOfOperandTypes(JetBinaryExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        JetExpression left = expression.getLeft();
        JetExpression right = expression.getRight();

        // TODO : duplicated effort for == and !=
        JetType leftType = facade.getTypeInfo(left, context.replaceScope(context.scope)).getType();
        if (leftType != null && right != null) {
            JetType rightType = facade.getTypeInfo(right, context.replaceScope(context.scope)).getType();

            if (rightType != null) {
                if (TypeUtils.isIntersectionEmpty(leftType, rightType)) {
                    context.trace.report(EQUALITY_NOT_APPLICABLE.on(expression, operationSign, leftType, rightType));
                }
            }
            if (isSenselessComparisonWithNull(leftType, right) || isSenselessComparisonWithNull(rightType, left)) {
                context.trace.report(SENSELESS_COMPARISON.on(expression, expression, operationSign.getReferencedNameElementType() == JetTokens.EXCLEQ));
            }
        }
    }

    private boolean isSenselessComparisonWithNull(@Nullable JetType firstType, @NotNull JetExpression secondExpression) {
        if (firstType == null) return false;
        return !firstType.isNullable() && secondExpression instanceof JetConstantExpression && secondExpression.getNode().getElementType() == JetNodeTypes.NULL;
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
        JetType type = resolveArrayAccessGetMethod(expression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        DataFlowUtils.checkType(type, expression, context);
        return JetTypeInfo.create(type, context.dataFlowInfo);
    }

    @Nullable
    public JetType getTypeForBinaryCall(JetScope scope, Name name, ExpressionTypingContext context, JetBinaryExpression binaryExpression) {
        ExpressionReceiver receiver = safeGetExpressionReceiver(facade, binaryExpression.getLeft(), context.replaceScope(scope));
        return OverloadResolutionResultsUtil.getResultType(getResolutionResultsForBinaryCall(scope, name, context, binaryExpression, receiver));
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
        if (context.namespacesAllowed) {
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
        for (JetStringTemplateEntry entry : expression.getEntries()) {
            entry.accept(new JetVisitorVoid() {

                @Override
                public void visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry) {
                    JetExpression entryExpression = entry.getExpression();
                    if (entryExpression != null) {
                        facade.getTypeInfo(entryExpression, context);
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
        return DataFlowUtils.checkType(JetStandardLibrary.getInstance().getStringType(), expression, contextWithExpectedType, context.dataFlowInfo);
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

    @Nullable
        /*package*/ JetType resolveArrayAccessSetMethod(@NotNull JetArrayAccessExpression arrayAccessExpression, @NotNull JetExpression rightHandSide, @NotNull ExpressionTypingContext context, @NotNull BindingTrace traceForResolveResult) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, rightHandSide, context, traceForResolveResult, false);
    }

    @Nullable
        /*package*/ JetType resolveArrayAccessGetMethod(@NotNull JetArrayAccessExpression arrayAccessExpression, @NotNull ExpressionTypingContext context) {
        return resolveArrayAccessSpecialMethod(arrayAccessExpression, null, context, context.trace, true);
    }

    @Nullable
    private JetType resolveArrayAccessSpecialMethod(@NotNull JetArrayAccessExpression arrayAccessExpression,
                                                    @Nullable JetExpression rightHandSide, //only for 'set' method
                                                    @NotNull ExpressionTypingContext context,
                                                    @NotNull BindingTrace traceForResolveResult,
                                                    boolean isGet) {
        JetType arrayType = facade.getTypeInfo(arrayAccessExpression.getArrayExpression(), context).getType();
        if (arrayType == null) return null;

        ExpressionReceiver receiver = new ExpressionReceiver(arrayAccessExpression.getArrayExpression(), arrayType);
        if (!isGet) assert rightHandSide != null;
        OverloadResolutionResults<FunctionDescriptor> functionResults = context.resolveCallWithGivenName(
                isGet
                ? CallMaker.makeArrayGetCall(receiver, arrayAccessExpression, Call.CallType.ARRAY_GET_METHOD)
                : CallMaker.makeArraySetCall(receiver, arrayAccessExpression, rightHandSide, Call.CallType.ARRAY_SET_METHOD),
                arrayAccessExpression,
                Name.identifier(isGet ? "get" : "set"));
        if (!functionResults.isSuccess()) {
            traceForResolveResult.report(isGet ? NO_GET_METHOD.on(arrayAccessExpression) : NO_SET_METHOD.on(arrayAccessExpression));
            return null;
        }
        traceForResolveResult.record(isGet ? INDEXED_LVALUE_GET : INDEXED_LVALUE_SET, arrayAccessExpression, functionResults.getResultingCall());
        return functionResults.getResultingDescriptor().getReturnType();
    }
}
