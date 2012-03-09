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

import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.newWritableScopeImpl;

/**
 * @author abreslav
 */
public class PatternMatchingTypingVisitor extends ExpressionTypingVisitor {
    protected PatternMatchingTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetType visitIsExpression(JetIsExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression leftHandSide = expression.getLeftHandSide();
        JetType knownType = facade.safeGetType(leftHandSide, context.replaceScope(context.scope));
        JetPattern pattern = expression.getPattern();
        if (pattern != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context).setDebugName("Scope extended in 'is'");
            DataFlowInfo newDataFlowInfo = checkPatternType(pattern, knownType, false, scopeToExtend, context, DataFlowValueFactory.INSTANCE.createDataFlowValue(leftHandSide, knownType, context.trace.getBindingContext()));
            context.patternsToDataFlowInfo.put(pattern, newDataFlowInfo);
            context.patternsToBoundVariableLists.put(pattern, scopeToExtend.getDeclaredVariables());
        }
        return DataFlowUtils.checkType(JetStandardLibrary.getInstance().getBooleanType(), expression, contextWithExpectedType);
    }

    @Override
    public JetType visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext context) {
        return visitWhenExpression(expression, context, false);
    }

    public JetType visitWhenExpression(final JetWhenExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        // TODO :change scope according to the bound value in the when header
        final JetExpression subjectExpression = expression.getSubjectExpression();

        final JetType subjectType = subjectExpression != null ? context.expressionTypingServices.safeGetType(context.scope, subjectExpression, TypeUtils.NO_EXPECTED_TYPE, context.trace) : ErrorUtils.createErrorType("Unknown type");
        final DataFlowValue variableDescriptor = subjectExpression != null ? DataFlowValueFactory.INSTANCE.createDataFlowValue(subjectExpression, subjectType, context.trace.getBindingContext()) : DataFlowValue.NULL;

        // TODO : exhaustive patterns

        Set<JetType> expressionTypes = Sets.newHashSet();
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            JetWhenCondition[] conditions = whenEntry.getConditions();
            DataFlowInfo newDataFlowInfo;
            WritableScope scopeToExtend;
            if (conditions.length == 1) {
                scopeToExtend = newWritableScopeImpl(context).setDebugName("Scope extended in when entry");
                newDataFlowInfo = context.dataFlowInfo;
                JetWhenCondition condition = conditions[0];
                if (condition != null) {
                    newDataFlowInfo = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition, scopeToExtend, context, variableDescriptor);
                }
            }
            else {
                scopeToExtend = newWritableScopeImpl(context); // We don't write to this scope
                newDataFlowInfo = null;
                for (JetWhenCondition condition : conditions) {
                    DataFlowInfo dataFlowInfo = checkWhenCondition(subjectExpression, subjectExpression == null, subjectType, condition, newWritableScopeImpl(context), context, variableDescriptor);
                    if (newDataFlowInfo == null) {
                        newDataFlowInfo = dataFlowInfo;
                    }
                    else {
                        newDataFlowInfo = newDataFlowInfo.or(dataFlowInfo);
                    }
                }
                if (newDataFlowInfo == null) {
                    newDataFlowInfo = context.dataFlowInfo;
                }
                else {
                    newDataFlowInfo = newDataFlowInfo.and(context.dataFlowInfo);
                }
            }
            JetExpression bodyExpression = whenEntry.getExpression();
            if (bodyExpression != null) {
                ExpressionTypingContext newContext = contextWithExpectedType.replaceScope(scopeToExtend).replaceDataFlowInfo(newDataFlowInfo);
                CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
                JetType type = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(bodyExpression), coercionStrategy, newContext, context.trace);
                if (type != null) {
                    expressionTypes.add(type);
                }
            }
        }

        if (!expressionTypes.isEmpty()) {
            return DataFlowUtils.checkImplicitCast(CommonSupertypes.commonSupertype(expressionTypes), expression, contextWithExpectedType, isStatement);
        }
        return null;
    }

    private DataFlowInfo checkWhenCondition(@Nullable final JetExpression subjectExpression, final boolean expectedCondition, final JetType subjectType, JetWhenCondition condition, final WritableScope scopeToExtend, final ExpressionTypingContext context, final DataFlowValue... subjectVariables) {
        final DataFlowInfo[] newDataFlowInfo = new DataFlowInfo[]{context.dataFlowInfo};
        condition.accept(new JetVisitorVoid() {

            @Override
            public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                JetExpression rangeExpression = condition.getRangeExpression();
                if (rangeExpression == null) return;
                if (expectedCondition) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                    facade.getType(rangeExpression, context);
                    return;
                }
                if (!facade.checkInExpression(condition, condition.getOperationReference(), subjectExpression, rangeExpression, context)) {
                    context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition));
                }
            }

            @Override
            public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                JetPattern pattern = condition.getPattern();
                if (expectedCondition) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                }
                if (pattern != null) {
                    newDataFlowInfo[0] = checkPatternType(pattern, subjectType, subjectExpression == null, scopeToExtend, context, subjectVariables);
                }
            }

            @Override
            public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
                JetPattern pattern = condition.getPattern();
                if (pattern != null) {
                    newDataFlowInfo[0] = checkPatternType(pattern, subjectType, subjectExpression == null, scopeToExtend, context, subjectVariables);
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
                context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
            }
        });
        return newDataFlowInfo[0];
    }

    private DataFlowInfo checkPatternType(@NotNull JetPattern pattern, @NotNull final JetType subjectType, final boolean conditionExpected, @NotNull final WritableScope scopeToExtend, final ExpressionTypingContext context, @NotNull final DataFlowValue... subjectVariables) {
        final Ref<DataFlowInfo> result = new Ref<DataFlowInfo>(context.dataFlowInfo);
        pattern.accept(new JetVisitorVoid() {
            @Override
            public void visitTypePattern(JetTypePattern typePattern) {
                JetTypeReference typeReference = typePattern.getTypeReference();
                if (typeReference == null) return;
                JetType type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);
                checkTypeCompatibility(type, subjectType, typePattern);
                result.set(context.dataFlowInfo.establishSubtyping(subjectVariables, type));
            }

            @Override
            public void visitTuplePattern(JetTuplePattern pattern) {
                List<JetTuplePatternEntry> entries = pattern.getEntries();
                TypeConstructor typeConstructor = subjectType.getConstructor();
                if (!JetStandardClasses.getTuple(entries.size()).getTypeConstructor().equals(typeConstructor)
                    || typeConstructor.getParameters().size() != entries.size()) {
                    context.trace.report(TYPE_MISMATCH_IN_TUPLE_PATTERN.on(pattern, subjectType, entries.size()));
                    return;
                }
                for (int i = 0, entriesSize = entries.size(); i < entriesSize; i++) {
                    JetTuplePatternEntry entry = entries.get(i);
                    JetType type = subjectType.getArguments().get(i).getType();

                    // TODO : is a name always allowed, ie for tuple patterns, not decomposer arg lists?
                    ASTNode nameLabelNode = entry.getNameLabelNode();
                    if (nameLabelNode != null) {
//                                context.trace.getErrorHandler().genericError(nameLabelNode, "Unsupported [OperatorConventions]");
                        context.trace.report(UNSUPPORTED.on(nameLabelNode.getPsi(), getClass().getCanonicalName()));
                    }

                    JetPattern entryPattern = entry.getPattern();
                    if (entryPattern != null) {
                        result.set(result.get().and(checkPatternType(entryPattern, type, false, scopeToExtend, context)));
                    }
                }
            }

            @Override
            public void visitDecomposerPattern(JetDecomposerPattern pattern) {
                JetExpression decomposerExpression = pattern.getDecomposerExpression();
                if (decomposerExpression != null) {
                    ReceiverDescriptor receiver = new TransientReceiver(subjectType);
                    JetType selectorReturnType = facade.getSelectorReturnType(receiver, null, decomposerExpression, context);

                    if (pattern.getArgumentList() != null) {
                        result.set(checkPatternType(pattern.getArgumentList(), selectorReturnType == null
                                                                           ? ErrorUtils.createErrorType("No type")
                                                                           : selectorReturnType, false, scopeToExtend, context));
                    }
                }
            }

            @Override
            public void visitWildcardPattern(JetWildcardPattern pattern) {
                // Nothing
            }

            @Override
            public void visitExpressionPattern(JetExpressionPattern pattern) {
                JetExpression expression = pattern.getExpression();
                if (expression == null) return;
                JetType type = facade.getType(expression, context.replaceScope(scopeToExtend));
                if (conditionExpected) {
                    JetType booleanType = JetStandardLibrary.getInstance().getBooleanType();
                    if (type != null && !JetTypeChecker.INSTANCE.equalTypes(booleanType, type)) {
                        context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(pattern, type));
                    }
                    return;
                }
                checkTypeCompatibility(type, subjectType, pattern);
            }

            @Override
            public void visitBindingPattern(JetBindingPattern pattern) {
                JetProperty variableDeclaration = pattern.getVariableDeclaration();
                JetTypeReference propertyTypeRef = variableDeclaration.getPropertyTypeRef();
                JetType type = propertyTypeRef == null ? subjectType : context.expressionTypingServices.getTypeResolver().resolveType(context.scope, propertyTypeRef, context.trace, true);
                VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptorWithType(context.scope.getContainingDeclaration(), variableDeclaration, type, context.trace);
                scopeToExtend.addVariableDescriptor(variableDescriptor);
                if (propertyTypeRef != null) {
                    if (!JetTypeChecker.INSTANCE.isSubtypeOf(subjectType, type)) {
                        context.trace.report(TYPE_MISMATCH_IN_BINDING_PATTERN.on(propertyTypeRef, type, subjectType));
                    }
                }

                JetWhenCondition condition = pattern.getCondition();
                if (condition != null) {
                    int oldLength = subjectVariables.length;
                    DataFlowValue[] newSubjectVariables = new DataFlowValue[oldLength + 1];
                    System.arraycopy(subjectVariables, 0, newSubjectVariables, 0, oldLength);
                    newSubjectVariables[oldLength] = DataFlowValueFactory.INSTANCE.createDataFlowValue(variableDescriptor);
                    result.set(checkWhenCondition(null, false, subjectType, condition, scopeToExtend, context, newSubjectVariables));
                }
            }

            /*
             * (a: SubjectType) is Type
             */
            private void checkTypeCompatibility(@Nullable JetType type, @NotNull JetType subjectType, @NotNull JetElement reportErrorOn) {
                // TODO : Take auto casts into account?
                if (type == null) {
                    return;
                }
                if (TypeUtils.intersect(JetTypeChecker.INSTANCE, Sets.newHashSet(type, subjectType)) == null) {
                    context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
                    return;
                }

                if (BasicExpressionTypingVisitor.isCastErased(subjectType, type, JetTypeChecker.INSTANCE)) {
                    context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(reportErrorOn, type));
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
                context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
            }
        });
        return result.get();
    }

}
