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

package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.JetModuleUtil;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

public class DataFlowValueFactory {
    private DataFlowValueFactory() {}

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull BindingContext bindingContext
    ) {
        if (expression instanceof JetConstantExpression) {
            JetConstantExpression constantExpression = (JetConstantExpression) expression;
            if (constantExpression.getNode().getElementType() == JetNodeTypes.NULL) return DataFlowValue.NULL;
        }
        if (TypeUtils.equalTypes(type, KotlinBuiltIns.getInstance().getNullableNothingType())) return DataFlowValue.NULL; // 'null' is the only inhabitant of 'Nothing?'
        IdentifierInfo result = getIdForStableIdentifier(expression, bindingContext);
        return new DataFlowValue(result == NO_IDENTIFIER_INFO ? expression : result.id, type, result.isStable, getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(@NotNull ThisReceiver receiver) {
        JetType type = receiver.getType();
        return new DataFlowValue(receiver, type, true, getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(@NotNull ReceiverValue receiverValue, @NotNull BindingContext bindingContext) {
        if (receiverValue instanceof TransientReceiver || receiverValue instanceof ScriptReceiver) {
            JetType type = receiverValue.getType();
            boolean nullable = type.isNullable() || TypeUtils.hasNullableSuperType(type);
            return new DataFlowValue(receiverValue, type, nullable, Nullability.NOT_NULL);
        }
        else if (receiverValue instanceof ClassReceiver || receiverValue instanceof ExtensionReceiver) {
            return createDataFlowValue((ThisReceiver) receiverValue);
        }
        else if (receiverValue instanceof ExpressionReceiver) {
            return createDataFlowValue(((ExpressionReceiver) receiverValue).getExpression(), receiverValue.getType(), bindingContext);
        }
        else if (receiverValue instanceof AutoCastReceiver) {
            return createDataFlowValue(((AutoCastReceiver) receiverValue).getOriginal(), bindingContext);
        }
        else if (receiverValue == ReceiverValue.NO_RECEIVER) {
            throw new IllegalArgumentException("No DataFlowValue exists for ReceiverValue.NO_RECEIVER");
        }
        else {
            throw new UnsupportedOperationException("Unsupported receiver value: " + receiverValue.getClass().getName());
        }
    }

    @NotNull
    private static Nullability getImmanentNullability(@NotNull JetType type) {
        return type.isNullable() || TypeUtils.hasNullableSuperType(type) ? Nullability.UNKNOWN : Nullability.NOT_NULL;
    }

    private static class IdentifierInfo {
        public final Object id;
        public final boolean isStable;
        public final boolean isNamespace;

        private IdentifierInfo(Object id, boolean isStable, boolean isNamespace) {
            this.id = id;
            this.isStable = isStable;
            this.isNamespace = isNamespace;
        }
    }

    private static final IdentifierInfo NO_IDENTIFIER_INFO = new IdentifierInfo(null, false, false) {
        @Override
        public String toString() {
            return "NO_IDENTIFIER_INFO";
        }
    };

    @NotNull
    private static IdentifierInfo createInfo(Object id, boolean isStable) {
        return new IdentifierInfo(id, isStable, false);
    }

    @NotNull
    private static IdentifierInfo createPackageInfo(Object id) {
        return new IdentifierInfo(id, true, true);
    }

    @NotNull
    private static IdentifierInfo combineInfo(@Nullable IdentifierInfo receiverInfo, @NotNull IdentifierInfo selectorInfo) {
        if (selectorInfo.id == null) {
            return NO_IDENTIFIER_INFO;
        }
        if (receiverInfo == null || receiverInfo == NO_IDENTIFIER_INFO || receiverInfo.isNamespace) {
            return selectorInfo;
        }
        return createInfo(Pair.create(receiverInfo.id, selectorInfo.id), receiverInfo.isStable && selectorInfo.isStable);
    }

    @NotNull
    private static IdentifierInfo getIdForStableIdentifier(
            @Nullable JetExpression expression,
            @NotNull BindingContext bindingContext
    ) {
        if (expression != null) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(expression);
            if (expression != deparenthesized) {
                return getIdForStableIdentifier(deparenthesized, bindingContext);
            }
        }
        if (expression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) expression;
            JetExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            JetExpression selectorExpression = qualifiedExpression.getSelectorExpression();
            IdentifierInfo receiverId = getIdForStableIdentifier(receiverExpression, bindingContext);
            IdentifierInfo selectorId = getIdForStableIdentifier(selectorExpression, bindingContext);

            return combineInfo(receiverId, selectorId);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return getIdForSimpleNameExpression((JetSimpleNameExpression) expression, bindingContext);
        }
        else if (expression instanceof JetThisExpression) {
            JetThisExpression thisExpression = (JetThisExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getInstanceReference());

            return getIdForThisReceiver(declarationDescriptor);
        }
        else if (expression instanceof JetRootNamespaceExpression) {
            return createPackageInfo(JetModuleUtil.getRootNamespaceType(expression));
        }
        return NO_IDENTIFIER_INFO;
    }

    @NotNull
    private static IdentifierInfo getIdForSimpleNameExpression(
            @NotNull JetSimpleNameExpression simpleNameExpression,
            @NotNull BindingContext bindingContext
    ) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression);
        if (declarationDescriptor instanceof VariableDescriptor) {
            ResolvedCall<?> resolvedCall = bindingContext.get(RESOLVED_CALL, simpleNameExpression);
            // todo uncomment assert
            // KT-4113
            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for " + declarationDescriptor;

            IdentifierInfo receiverInfo = resolvedCall != null ? getIdForImplicitReceiver(resolvedCall.getThisObject(), simpleNameExpression) : null;

            VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
            return combineInfo(receiverInfo, createInfo(variableDescriptor, isStableVariable(variableDescriptor)));
        }
        if (declarationDescriptor instanceof PackageViewDescriptor) {
            return createPackageInfo(declarationDescriptor);
        }
        return NO_IDENTIFIER_INFO;
    }

    @Nullable
    private static IdentifierInfo getIdForImplicitReceiver(@NotNull ReceiverValue receiverValue, @Nullable JetExpression expression) {
        if (receiverValue instanceof ThisReceiver) {
            return getIdForThisReceiver(((ThisReceiver) receiverValue).getDeclarationDescriptor());
        }
        else if (receiverValue instanceof AutoCastReceiver) {
            return getIdForImplicitReceiver(((AutoCastReceiver) receiverValue).getOriginal(), expression);
        }
        else {
            assert !(receiverValue instanceof TransientReceiver)
                    : "Transient receiver is implicit for an explicit expression: " + expression + ". Receiver: " + receiverValue;
            // For ExpressionReceiver there is an explicit "this" expression and it was analyzed earlier
            return null;
        }
    }

    @NotNull
    private static IdentifierInfo getIdForThisReceiver(@Nullable DeclarationDescriptor descriptorOfThisReceiver) {
        if (descriptorOfThisReceiver instanceof CallableDescriptor) {
            ReceiverParameterDescriptor receiverParameter = ((CallableDescriptor) descriptorOfThisReceiver).getReceiverParameter();
            assert receiverParameter != null : "'This' refers to the callable member without a receiver parameter: " + descriptorOfThisReceiver;
            return createInfo(receiverParameter.getValue(), true);
        }
        if (descriptorOfThisReceiver instanceof ClassDescriptor) {
            return createInfo(((ClassDescriptor) descriptorOfThisReceiver).getThisAsReceiverParameter().getValue(), true);
        }
        return NO_IDENTIFIER_INFO;
    }

    public static boolean isStableVariable(@NotNull VariableDescriptor variableDescriptor) {
        if (variableDescriptor.isVar()) return false;
        if (variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            if (!invisibleFromOtherModules(propertyDescriptor)) return false;
            if (!isFinal(propertyDescriptor)) return false;
            if (!hasDefaultGetter(propertyDescriptor)) return false;
        }
        return true;
    }

    private static boolean isFinal(PropertyDescriptor propertyDescriptor) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            if (classDescriptor.getModality().isOverridable() && propertyDescriptor.getModality().isOverridable()) return false;
        }
        else {
            if (propertyDescriptor.getModality().isOverridable()) {
                throw new IllegalStateException("Property outside a class must not be overridable: " + propertyDescriptor.getName());
            }
        }
        return true;
    }

    private static boolean invisibleFromOtherModules(@NotNull DeclarationDescriptorWithVisibility descriptor) {
        if (Visibilities.INVISIBLE_FROM_OTHER_MODULES.contains(descriptor.getVisibility())) return true;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof DeclarationDescriptorWithVisibility)) {
            return false;
        }

        return invisibleFromOtherModules((DeclarationDescriptorWithVisibility) containingDeclaration);
    }

    private static boolean hasDefaultGetter(PropertyDescriptor propertyDescriptor) {
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        return getter == null || getter.isDefault();
    }
}
