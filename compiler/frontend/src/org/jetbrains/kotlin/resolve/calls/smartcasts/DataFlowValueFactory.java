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

package org.jetbrains.kotlin.resolve.calls.smartcasts;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.*;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableNothing;
import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;

/**
 * This class is intended to create data flow values for different kind of expressions.
 * Then data flow values serve as keys to obtain data flow information for these expressions.
 */
public class DataFlowValueFactory {
    private DataFlowValueFactory() {
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull ResolutionContext resolutionContext
    ) {
        return createDataFlowValue(expression, type, resolutionContext.trace.getBindingContext(),
                                   resolutionContext.scope.getContainingDeclaration());
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        if (expression instanceof JetConstantExpression) {
            JetConstantExpression constantExpression = (JetConstantExpression) expression;
            if (constantExpression.getNode().getElementType() == JetNodeTypes.NULL) return DataFlowValue.NULL;
        }
        if (type.isError()) return DataFlowValue.ERROR;
        if (isNullableNothing(type)) {
            return DataFlowValue.NULL; // 'null' is the only inhabitant of 'Nothing?'
        }

        if (ExpressionTypingUtils.isExclExclExpression(JetPsiUtil.deparenthesize(expression))) {
            // In most cases type of `E!!`-expression is strictly not nullable and we could get proper Nullability
            // by calling `getImmanentNullability` (as it happens below).
            //
            // But there are some problem with types built on type parameters, e.g.
            // fun <T : Any?> foo(x: T) = x!!.hashCode() // there no way in type system to denote that `x!!` is not nullable
            return new DataFlowValue(expression,
                                     type,
                                     /* stableIdentifier  = */false,
                                     /* uncapturedLocalVariable = */false,
                                     Nullability.NOT_NULL);
        }

        IdentifierInfo result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule);
        return new DataFlowValue(result == NO_IDENTIFIER_INFO ? expression : result.id,
                                 type,
                                 result.isStable,
                                 result.isLocal,
                                 getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(@NotNull ThisReceiver receiver) {
        JetType type = receiver.getType();
        return new DataFlowValue(receiver, type, true, false, getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull ReceiverValue receiverValue,
            @NotNull ResolutionContext resolutionContext
    ) {
        return createDataFlowValue(receiverValue, resolutionContext.trace.getBindingContext(),
                                   resolutionContext.scope.getContainingDeclaration());
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull ReceiverValue receiverValue,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        if (receiverValue instanceof TransientReceiver || receiverValue instanceof ScriptReceiver) {
            // SCRIPT: smartcasts data flow
            JetType type = receiverValue.getType();
            boolean nullable = type.isMarkedNullable() || TypeUtils.hasNullableSuperType(type);
            return new DataFlowValue(receiverValue, type, nullable, false, Nullability.NOT_NULL);
        }
        else if (receiverValue instanceof ClassReceiver || receiverValue instanceof ExtensionReceiver) {
            return createDataFlowValue((ThisReceiver) receiverValue);
        }
        else if (receiverValue instanceof ExpressionReceiver) {
            return createDataFlowValue(((ExpressionReceiver) receiverValue).getExpression(),
                                       receiverValue.getType(),
                                       bindingContext,
                                       containingDeclarationOrModule);
        }
        else if (receiverValue == ReceiverValue.NO_RECEIVER) {
            throw new IllegalArgumentException("No DataFlowValue exists for ReceiverValue.NO_RECEIVER");
        }
        else {
            throw new UnsupportedOperationException("Unsupported receiver value: " + receiverValue.getClass().getName());
        }
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull BindingContext bindingContext,
            @Nullable ModuleDescriptor usageContainingModule
    ) {
        JetType type = variableDescriptor.getType();
        return new DataFlowValue(variableDescriptor, type,
                                 isStableVariable(variableDescriptor, usageContainingModule),
                                 isUncapturedLocalVariable(variableDescriptor, bindingContext),
                                 getImmanentNullability(type));
    }

    @NotNull
    private static Nullability getImmanentNullability(@NotNull JetType type) {
        return TypeUtils.isNullableType(type) ? Nullability.UNKNOWN : Nullability.NOT_NULL;
    }

    private static class IdentifierInfo {
        public final Object id;
        public final boolean isStable;
        public final boolean isLocal;
        public final boolean isPackage;

        private IdentifierInfo(Object id, boolean isStable, boolean isLocal, boolean isPackage) {
            assert !isStable || !isLocal : "Identifier info for object " + id + " cannot be stable and local at one time";
            this.id = id;
            this.isStable = isStable;
            this.isLocal = isLocal;
            this.isPackage = isPackage;
        }
    }

    private static final IdentifierInfo NO_IDENTIFIER_INFO = new IdentifierInfo(null, false, false, false) {
        @Override
        public String toString() {
            return "NO_IDENTIFIER_INFO";
        }
    };

    @NotNull
    private static IdentifierInfo createInfo(Object id, boolean isStable, boolean isLocal) {
        return new IdentifierInfo(id, isStable, isLocal, false);
    }

    @NotNull
    private static IdentifierInfo createStableInfo(Object id) {
        return createInfo(id, true, false);
    }

    @NotNull
    private static IdentifierInfo createPackageInfo(Object id) {
        return new IdentifierInfo(id, true, false, true);
    }

    @NotNull
    private static IdentifierInfo combineInfo(@Nullable IdentifierInfo receiverInfo, @NotNull IdentifierInfo selectorInfo) {
        if (selectorInfo.id == null) {
            return NO_IDENTIFIER_INFO;
        }
        if (receiverInfo == null || receiverInfo == NO_IDENTIFIER_INFO || receiverInfo.isPackage) {
            return selectorInfo;
        }
        return createInfo(Pair.create(receiverInfo.id, selectorInfo.id),
                          receiverInfo.isStable && selectorInfo.isStable,
                          // x.y can never be a local variable
                          false);
    }

    @NotNull
    private static IdentifierInfo createPostfixInfo(@NotNull JetPostfixExpression expression, @NotNull IdentifierInfo argumentInfo) {
        if (argumentInfo == NO_IDENTIFIER_INFO) {
            return NO_IDENTIFIER_INFO;
        }
        return createInfo(Pair.create(expression, argumentInfo.id), argumentInfo.isStable, argumentInfo.isLocal);
    }

    @NotNull
    private static IdentifierInfo getIdForStableIdentifier(
            @Nullable JetExpression expression,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        if (expression != null) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(expression);
            if (expression != deparenthesized) {
                return getIdForStableIdentifier(deparenthesized, bindingContext, containingDeclarationOrModule);
            }
        }
        if (expression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) expression;
            JetExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            JetExpression selectorExpression = qualifiedExpression.getSelectorExpression();
            IdentifierInfo receiverId = getIdForStableIdentifier(receiverExpression, bindingContext, containingDeclarationOrModule);
            IdentifierInfo selectorId = getIdForStableIdentifier(selectorExpression, bindingContext, containingDeclarationOrModule);

            return combineInfo(receiverId, selectorId);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return getIdForSimpleNameExpression((JetSimpleNameExpression) expression, bindingContext, containingDeclarationOrModule);
        }
        else if (expression instanceof JetThisExpression) {
            JetThisExpression thisExpression = (JetThisExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getInstanceReference());

            return getIdForThisReceiver(declarationDescriptor);
        }
        else if (expression instanceof JetPostfixExpression) {
            JetPostfixExpression postfixExpression = (JetPostfixExpression) expression;
            IElementType operationType = postfixExpression.getOperationReference().getReferencedNameElementType();
            if (operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS) {
                return createPostfixInfo(postfixExpression,
                        getIdForStableIdentifier(postfixExpression.getBaseExpression(), bindingContext, containingDeclarationOrModule));
            }
        }
        else if (expression instanceof JetRootPackageExpression) {
            //todo return createPackageInfo());
        }
        return NO_IDENTIFIER_INFO;
    }

    @NotNull
    private static IdentifierInfo getIdForSimpleNameExpression(
            @NotNull JetSimpleNameExpression simpleNameExpression,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression);
        if (declarationDescriptor instanceof VariableDescriptor) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(simpleNameExpression, bindingContext);

            // todo uncomment assert
            // KT-4113
            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for
            ModuleDescriptor usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(containingDeclarationOrModule);
            IdentifierInfo receiverInfo =
                    resolvedCall != null ? getIdForImplicitReceiver(resolvedCall.getDispatchReceiver(), simpleNameExpression) : null;

            VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
            return combineInfo(receiverInfo, createInfo(variableDescriptor,
                                                        isStableVariable(variableDescriptor, usageModuleDescriptor),
                                                        isUncapturedLocalVariable(variableDescriptor, bindingContext)));
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
            ReceiverParameterDescriptor receiverParameter = ((CallableDescriptor) descriptorOfThisReceiver).getExtensionReceiverParameter();
            assert receiverParameter != null : "'This' refers to the callable member without a receiver parameter: " +
                                               descriptorOfThisReceiver;
            return createStableInfo(receiverParameter.getValue());
        }
        if (descriptorOfThisReceiver instanceof ClassDescriptor) {
            return createStableInfo(((ClassDescriptor) descriptorOfThisReceiver).getThisAsReceiverParameter().getValue());
        }
        return NO_IDENTIFIER_INFO;
    }

    public static boolean isUncapturedLocalVariable(@NotNull VariableDescriptor variableDescriptor, @NotNull BindingContext bindingContext) {
        return variableDescriptor.isVar()
               && variableDescriptor instanceof LocalVariableDescriptor
               && !BindingContextUtils.isVarCapturedInClosure(bindingContext, variableDescriptor);
    }

    /**
     * Determines whether a variable with a given descriptor is stable or not at the given usage place.
     * <p/>
     * Stable means that the variable value cannot change. The simple (non-property) variable is considered stable if it's immutable (val).
     * <p/>
     * If the variable is a property, it's considered stable if it's immutable (val) AND it's final (not open) AND
     * the default getter is in use (otherwise nobody can guarantee that a getter is consistent) AND
     * (it's private OR internal OR used at the same module where it's defined).
     * The last check corresponds to a risk of changing property definition in another module, e.g. from "val" to "var".
     *
     * @param variableDescriptor    descriptor of a considered variable
     * @param usageModule a module with a considered usage place, or null if it's not known (not recommended)
     * @return true if variable is stable, false otherwise
     */
    public static boolean isStableVariable(
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable ModuleDescriptor usageModule
    ) {
        if (variableDescriptor.isVar()) return false;
        if (variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            if (!isFinal(propertyDescriptor)) return false;
            if (!hasDefaultGetter(propertyDescriptor)) return false;
            if (!invisibleFromOtherModules(propertyDescriptor)) {
                ModuleDescriptor declarationModule = DescriptorUtils.getContainingModule(propertyDescriptor);
                if (usageModule == null || !usageModule.equals(declarationModule)) {
                    return false;
                }
            }
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
