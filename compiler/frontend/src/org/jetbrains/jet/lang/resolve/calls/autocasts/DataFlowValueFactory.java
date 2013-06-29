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
    public static final DataFlowValueFactory INSTANCE = new DataFlowValueFactory();

    private DataFlowValueFactory() {}

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull JetExpression expression, @NotNull JetType type, @NotNull BindingContext bindingContext) {
        if (expression instanceof JetConstantExpression) {
            JetConstantExpression constantExpression = (JetConstantExpression) expression;
            if (constantExpression.getNode().getElementType() == JetNodeTypes.NULL) return DataFlowValue.NULL;
        }
        if (TypeUtils.equalTypes(type, KotlinBuiltIns.getInstance().getNullableNothingType())) return DataFlowValue.NULL; // 'null' is the only inhabitant of 'Nothing?'
        IdentifierInfo result = getIdForStableIdentifier(expression, bindingContext);
        return new DataFlowValue(result.id == null ? expression : result.id, type, result.isStable, getImmanentNullability(type));
    }

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull ThisReceiver receiver) {
        JetType type = receiver.getType();
        return new DataFlowValue(receiver, type, true, getImmanentNullability(type));
    }

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull VariableDescriptor variableDescriptor) {
        JetType type = variableDescriptor.getType();
        return new DataFlowValue(variableDescriptor, type, isStableVariable(variableDescriptor), getImmanentNullability(type));
    }

    @NotNull
    public DataFlowValue createDataFlowValue(@NotNull ReceiverValue receiverValue, @NotNull BindingContext bindingContext) {
        return receiverValue.accept(new ReceiverValueVisitor<DataFlowValue, BindingContext>() {
            @Override
            public DataFlowValue visitNoReceiver(ReceiverValue noReceiver, BindingContext data) {
                throw new IllegalArgumentException("No DataFlowValue exists for ReceiverValue.NO_RECEIVER");
            }

            @Override
            public DataFlowValue visitExtensionReceiver(ExtensionReceiver receiver, BindingContext data) {
                return createDataFlowValue(receiver);
            }

            @Override
            public DataFlowValue visitExpressionReceiver(ExpressionReceiver receiver, BindingContext bindingContext) {
                return createDataFlowValue(receiver.getExpression(), receiver.getType(), bindingContext);
            }

            @Override
            public DataFlowValue visitClassReceiver(ClassReceiver receiver, BindingContext data) {
                return createDataFlowValue(receiver);
            }

            @Override
            public DataFlowValue visitTransientReceiver(TransientReceiver receiver, BindingContext data) {
                return createTransientDataFlowValue(receiver);
            }

            @Override
            public DataFlowValue visitScriptReceiver(ScriptReceiver receiver, BindingContext data) {
                return createTransientDataFlowValue(receiver);
            }

            @NotNull
            private DataFlowValue createTransientDataFlowValue(ReceiverValue receiver) {
                JetType type = receiver.getType();
                boolean nullable = type.isNullable() || TypeUtils.hasNullableSuperType(type);
                return new DataFlowValue(receiver, type, nullable, Nullability.NOT_NULL);
            }
        }, bindingContext);
    }

    private Nullability getImmanentNullability(JetType type) {
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

    private static final IdentifierInfo NO_IDENTIFIER_INFO = new IdentifierInfo(null, false, false);

    @NotNull
    private static IdentifierInfo createInfo(Object id, boolean isStable) {
        return new IdentifierInfo(id, isStable, false);
    }

    @NotNull
    private static IdentifierInfo createNamespaceInfo(Object id) {
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
        if (expression instanceof JetParenthesizedExpression) {
            JetParenthesizedExpression parenthesizedExpression = (JetParenthesizedExpression) expression;
            JetExpression innerExpression = parenthesizedExpression.getExpression();

            return getIdForStableIdentifier(innerExpression, bindingContext);
        }
        else if (expression instanceof JetQualifiedExpression) {
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
            return createNamespaceInfo(JetModuleUtil.getRootNamespaceType(expression));
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
            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for " + declarationDescriptor;

            IdentifierInfo receiverInfo = resolvedCall != null ? getIdForImplicitReceiver(resolvedCall.getThisObject(), simpleNameExpression) : null;

            VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
            return combineInfo(receiverInfo, createInfo(variableDescriptor, isStableVariable(variableDescriptor)));
        }
        if (declarationDescriptor instanceof NamespaceDescriptor) {
            return createNamespaceInfo(declarationDescriptor);
        }
        if (declarationDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
            return createInfo(classDescriptor, classDescriptor.isClassObjectAValue());
        }
        return NO_IDENTIFIER_INFO;
    }

    @Nullable
    private static IdentifierInfo getIdForImplicitReceiver(@NotNull ReceiverValue receiverValue, @Nullable final JetExpression expression) {
        return receiverValue.accept(new ReceiverValueVisitor<IdentifierInfo, Void>() {

            @Override
            public IdentifierInfo visitNoReceiver(ReceiverValue noReceiver, Void data) {
                return null;
            }

            @Override
            public IdentifierInfo visitTransientReceiver(TransientReceiver receiver, Void data) {
                assert false: "Transient receiver is implicit for an explicit expression: " + expression + ". Receiver: " + receiver;
                return null;
            }

            @Override
            public IdentifierInfo visitExtensionReceiver(ExtensionReceiver receiver, Void data) {
                return getIdForThisReceiver(receiver);
            }

            @Override
            public IdentifierInfo visitExpressionReceiver(ExpressionReceiver receiver, Void data) {
                // there is an explicit "this" expression and it was analyzed earlier
                return null;
            }

            @Override
            public IdentifierInfo visitClassReceiver(ClassReceiver receiver, Void data) {
                return getIdForThisReceiver(receiver);
            }

            @Override
            public IdentifierInfo visitScriptReceiver(ScriptReceiver receiver, Void data) {
                return getIdForThisReceiver(receiver);
            }
        }, null);
    }

    @NotNull
    private static IdentifierInfo getIdForThisReceiver(@NotNull ThisReceiver thisReceiver) {
        return getIdForThisReceiver(thisReceiver.getDeclarationDescriptor());
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
