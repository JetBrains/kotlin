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

package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.JetModuleUtil;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

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
        Pair<Object, Boolean> result = getIdForStableIdentifier(expression, bindingContext, false);
        return new DataFlowValue(result.first == null ? expression : result.first, type, result.second, getImmanentNullability(type));
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

    @NotNull
    private static Pair<Object, Boolean> getIdForStableIdentifier(@NotNull JetExpression expression, @NotNull BindingContext bindingContext, boolean allowNamespaces) {
        if (expression instanceof JetParenthesizedExpression) {
            JetParenthesizedExpression parenthesizedExpression = (JetParenthesizedExpression) expression;
            JetExpression innerExpression = parenthesizedExpression.getExpression();
            if (innerExpression == null) {
                return Pair.create(null, false);
            }
            return getIdForStableIdentifier(innerExpression, bindingContext, allowNamespaces);
        }
        else if (expression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) expression;
            JetExpression selectorExpression = qualifiedExpression.getSelectorExpression();
            if (selectorExpression == null) {
                return Pair.create(null, false);
            }
            Pair<Object, Boolean> receiverId = getIdForStableIdentifier(qualifiedExpression.getReceiverExpression(), bindingContext, true);
            Pair<Object, Boolean> selectorId = getIdForStableIdentifier(selectorExpression, bindingContext, allowNamespaces);
            return receiverId.second ? selectorId : Pair.create(receiverId.first, false);
        }
        if (expression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression);
            if (declarationDescriptor instanceof VariableDescriptor) {
                return Pair.create((Object) declarationDescriptor, isStableVariable((VariableDescriptor) declarationDescriptor));
            }
            if (declarationDescriptor instanceof NamespaceDescriptor) {
                return Pair.create((Object) declarationDescriptor, allowNamespaces);
            }
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                return Pair.create((Object) classDescriptor, classDescriptor.isClassObjectAValue());
            }
        }
        else if (expression instanceof JetThisExpression) {
            JetThisExpression thisExpression = (JetThisExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getInstanceReference());
            if (declarationDescriptor instanceof CallableDescriptor) {
                return Pair.create((Object) ((CallableDescriptor) declarationDescriptor).getReceiverParameter().getValue(), true);
            }
            if (declarationDescriptor instanceof ClassDescriptor) {
                return Pair.create((Object) ((ClassDescriptor) declarationDescriptor).getThisAsReceiverParameter().getValue(), true);
            }
            return Pair.create(null, true);
        }
        else if (expression instanceof JetRootNamespaceExpression) {
            return Pair.create((Object) JetModuleUtil.getRootNamespaceType(expression), allowNamespaces);
        }
        return Pair.create(null, false);
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
