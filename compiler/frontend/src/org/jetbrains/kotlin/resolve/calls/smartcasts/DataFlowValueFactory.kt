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
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.*;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor;

import java.util.Set;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableNothing;
import static org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR;
import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.*;

/**
 * This class is intended to create data flow values for different kind of expressions.
 * Then data flow values serve as keys to obtain data flow information for these expressions.
 */
public class DataFlowValueFactory {
    private DataFlowValueFactory() {
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull KtExpression expression,
            @NotNull KotlinType type,
            @NotNull ResolutionContext resolutionContext
    ) {
        return createDataFlowValue(expression, type, resolutionContext.trace.getBindingContext(),
                                   resolutionContext.scope.getOwnerDescriptor());
    }

    private static boolean isComplexExpression(@NotNull KtExpression expression) {
        if (expression instanceof KtBlockExpression ||
            expression instanceof KtIfExpression ||
            expression instanceof KtWhenExpression ||
            (expression instanceof KtBinaryExpression && ((KtBinaryExpression) expression).getOperationToken() == KtTokens.ELVIS)) {

            return true;
        }
        if (expression instanceof KtParenthesizedExpression) {
            KtExpression deparenthesized = KtPsiUtil.deparenthesize(expression);
            return deparenthesized != null && isComplexExpression(deparenthesized);
        }
        return false;
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull KtExpression expression,
            @NotNull KotlinType type,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        if (expression instanceof KtConstantExpression) {
            KtConstantExpression constantExpression = (KtConstantExpression) expression;
            if (constantExpression.getNode().getElementType() == KtNodeTypes.NULL) {
                return DataFlowValue.nullValue(DescriptorUtilsKt.getBuiltIns(containingDeclarationOrModule));
            }
        }
        if (type.isError()) return DataFlowValue.ERROR;
        if (isNullableNothing(type)) {
            return DataFlowValue.nullValue(DescriptorUtilsKt.getBuiltIns(containingDeclarationOrModule)); // 'null' is the only inhabitant of 'Nothing?'
        }

        if (ExpressionTypingUtils.isExclExclExpression(KtPsiUtil.deparenthesize(expression))) {
            // In most cases type of `E!!`-expression is strictly not nullable and we could get proper Nullability
            // by calling `getImmanentNullability` (as it happens below).
            //
            // But there are some problem with types built on type parameters, e.g.
            // fun <T : Any?> foo(x: T) = x!!.hashCode() // there no way in type system to denote that `x!!` is not nullable
            return new DataFlowValue(expression,
                                     type,
                                     OTHER,
                                     Nullability.NOT_NULL);
        }

        if (isComplexExpression(expression)) {
            return createDataFlowValueForComplexExpression(expression, type);
        }

        IdentifierInfo result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule);
        return new DataFlowValue(result == NO_IDENTIFIER_INFO ? expression : result.id,
                                 type,
                                 result.kind,
                                 getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValueForStableReceiver(@NotNull ReceiverValue receiver) {
        KotlinType type = receiver.getType();
        return new DataFlowValue(receiver, type, STABLE_VALUE, getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull ReceiverValue receiverValue,
            @NotNull ResolutionContext resolutionContext
    ) {
        return createDataFlowValue(receiverValue, resolutionContext.trace.getBindingContext(),
                                   resolutionContext.scope.getOwnerDescriptor());
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull ReceiverValue receiverValue,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        if (receiverValue instanceof TransientReceiver || receiverValue instanceof ImplicitReceiver) {
            return createDataFlowValueForStableReceiver(receiverValue);
        }
        else if (receiverValue instanceof ExpressionReceiver) {
            return createDataFlowValue(((ExpressionReceiver) receiverValue).getExpression(),
                                       receiverValue.getType(),
                                       bindingContext,
                                       containingDeclarationOrModule);
        }
        else {
            throw new UnsupportedOperationException("Unsupported receiver value: " + receiverValue.getClass().getName());
        }
    }

    @NotNull
    public static DataFlowValue createDataFlowValueForProperty(
            @NotNull KtProperty property,
            @NotNull VariableDescriptor variableDescriptor,
            @NotNull BindingContext bindingContext,
            @Nullable ModuleDescriptor usageContainingModule
    ) {
        KotlinType type = variableDescriptor.getType();
        return new DataFlowValue(variableDescriptor, type,
                                 variableKind(variableDescriptor, usageContainingModule,
                                              bindingContext, property),
                                 getImmanentNullability(type));
    }

    @NotNull
    private static DataFlowValue createDataFlowValueForComplexExpression(
            @NotNull KtExpression expression,
            @NotNull KotlinType type
    ) {
        return new DataFlowValue(expression, type, Kind.STABLE_COMPLEX_EXPRESSION, getImmanentNullability(type));
    }

    @NotNull
    private static Nullability getImmanentNullability(@NotNull KotlinType type) {
        return TypeUtils.isNullableType(type) ? Nullability.UNKNOWN : Nullability.NOT_NULL;
    }

    private static class IdentifierInfo {
        public final Object id;
        public final Kind kind;
        public final boolean isPackage;

        private IdentifierInfo(Object id, Kind kind, boolean isPackage) {
            this.id = id;
            this.kind = kind;
            this.isPackage = isPackage;
        }
    }

    private static final IdentifierInfo NO_IDENTIFIER_INFO = new IdentifierInfo(null, OTHER, false) {
        @Override
        public String toString() {
            return "NO_IDENTIFIER_INFO";
        }
    };

    @NotNull
    private static IdentifierInfo createInfo(Object id, Kind kind) {
        return new IdentifierInfo(id, kind, false);
    }

    @NotNull
    private static IdentifierInfo createStableInfo(Object id) {
        return createInfo(id, STABLE_VALUE);
    }

    @NotNull
    private static IdentifierInfo createPackageOrClassInfo(Object id) {
        return new IdentifierInfo(id, STABLE_VALUE, true);
    }

    @NotNull
    private static IdentifierInfo combineInfo(@Nullable IdentifierInfo receiverInfo, @NotNull IdentifierInfo selectorInfo) {
        if (selectorInfo.id == null || receiverInfo == NO_IDENTIFIER_INFO) {
            return NO_IDENTIFIER_INFO;
        }
        if (receiverInfo == null || receiverInfo.isPackage) {
            return selectorInfo;
        }
        return createInfo(Pair.create(receiverInfo.id, selectorInfo.id),
                          receiverInfo.kind.isStable() ? selectorInfo.kind : OTHER);
    }

    @NotNull
    private static IdentifierInfo createPostfixInfo(@NotNull KtPostfixExpression expression, @NotNull IdentifierInfo argumentInfo) {
        if (argumentInfo == NO_IDENTIFIER_INFO) {
            return NO_IDENTIFIER_INFO;
        }
        return createInfo(Pair.create(expression, argumentInfo.id), argumentInfo.kind);
    }

    @NotNull
    private static IdentifierInfo getIdForStableIdentifier(
            @Nullable KtExpression expression,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        if (expression != null) {
            KtExpression deparenthesized = KtPsiUtil.deparenthesize(expression);
            if (expression != deparenthesized) {
                return getIdForStableIdentifier(deparenthesized, bindingContext, containingDeclarationOrModule);
            }
        }
        if (expression instanceof KtQualifiedExpression) {
            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) expression;
            KtExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            KtExpression selectorExpression = qualifiedExpression.getSelectorExpression();
            IdentifierInfo receiverId = getIdForStableIdentifier(receiverExpression, bindingContext, containingDeclarationOrModule);
            IdentifierInfo selectorId = getIdForStableIdentifier(selectorExpression, bindingContext, containingDeclarationOrModule);

            return combineInfo(receiverId, selectorId);
        }
        if (expression instanceof KtSimpleNameExpression) {
            return getIdForSimpleNameExpression((KtSimpleNameExpression) expression, bindingContext, containingDeclarationOrModule);
        }
        else if (expression instanceof KtThisExpression) {
            KtThisExpression thisExpression = (KtThisExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getInstanceReference());

            return getIdForThisReceiver(declarationDescriptor);
        }
        else if (expression instanceof KtPostfixExpression) {
            KtPostfixExpression postfixExpression = (KtPostfixExpression) expression;
            IElementType operationType = postfixExpression.getOperationReference().getReferencedNameElementType();
            if (operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS) {
                return createPostfixInfo(postfixExpression,
                        getIdForStableIdentifier(postfixExpression.getBaseExpression(), bindingContext, containingDeclarationOrModule));
            }
        }
        return NO_IDENTIFIER_INFO;
    }

    @NotNull
    private static IdentifierInfo getIdForSimpleNameExpression(
            @NotNull KtSimpleNameExpression simpleNameExpression,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclarationOrModule
    ) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression);
        if (declarationDescriptor instanceof VariableDescriptor) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(simpleNameExpression, bindingContext);

            // todo uncomment assert
            // KT-4113
            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for
            ModuleDescriptor usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(containingDeclarationOrModule);
            IdentifierInfo receiverInfo =
                    resolvedCall != null ? getIdForImplicitReceiver(resolvedCall.getDispatchReceiver(), simpleNameExpression) : null;

            VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
            return combineInfo(receiverInfo,
                               createInfo(variableDescriptor,
                                          variableKind(variableDescriptor, usageModuleDescriptor,
                                                       bindingContext, simpleNameExpression)));
        }
        if (declarationDescriptor instanceof PackageViewDescriptor || declarationDescriptor instanceof ClassDescriptor) {
            return createPackageOrClassInfo(declarationDescriptor);
        }
        return NO_IDENTIFIER_INFO;
    }

    @Nullable
    private static IdentifierInfo getIdForImplicitReceiver(@Nullable ReceiverValue receiverValue, @Nullable KtExpression expression) {
        if (receiverValue instanceof ImplicitReceiver) {
            return getIdForThisReceiver(((ImplicitReceiver) receiverValue).getDeclarationDescriptor());
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

    @NotNull
    private static DeclarationDescriptor getVariableContainingDeclaration(@NotNull VariableDescriptor variableDescriptor) {
        DeclarationDescriptor containingDeclarationDescriptor = variableDescriptor.getContainingDeclaration();
        if (containingDeclarationDescriptor instanceof ConstructorDescriptor
            && ((ConstructorDescriptor) containingDeclarationDescriptor).isPrimary()) {
            // This code is necessary just because JetClassInitializer has no associated descriptor in trace
            // Because of it we have to use class itself instead of initializer,
            // otherwise we could not find this descriptor inside isAccessedInsideClosure below
            containingDeclarationDescriptor = containingDeclarationDescriptor.getContainingDeclaration();
            assert containingDeclarationDescriptor != null : "No containing declaration for primary constructor";
        }
        return containingDeclarationDescriptor;
    }

    private static boolean isAccessedInsideClosure(
            @NotNull DeclarationDescriptor variableContainingDeclaration,
            @NotNull BindingContext bindingContext,
            @NotNull KtElement accessElement
    ) {
        KtDeclaration parent = ControlFlowInformationProvider.getElementParentDeclaration(accessElement);
        if (parent != null) {
            DeclarationDescriptor descriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, parent);
            // Access is at the same declaration: not in closure, lower: in closure
            return !variableContainingDeclaration.equals(descriptor);
        }
        return false;
    }

    private static boolean isAccessedBeforeAllClosureWriters(
            @NotNull DeclarationDescriptor variableContainingDeclaration,
            @NotNull Set<KtDeclaration> writers,
            @NotNull BindingContext bindingContext,
            @NotNull KtElement accessElement
    ) {
        // All writers should be before access element, with the exception:
        // writer which is the same with declaration site does not count
        for (KtDeclaration writer : writers) {
            DeclarationDescriptor writerDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, writer);
            // Access is after some writer
            if (!variableContainingDeclaration.equals(writerDescriptor) && !PsiUtilsKt.before(accessElement, writer)) {
                return false;
            }
        }
        // Access is before all writers
        return true;
    }

    private static Kind propertyKind(@NotNull PropertyDescriptor propertyDescriptor, @Nullable ModuleDescriptor usageModule) {
        if (propertyDescriptor.isVar()) return MUTABLE_PROPERTY;
        if (ModalityKt.isOverridable(propertyDescriptor)) return PROPERTY_WITH_GETTER;
        if (!hasDefaultGetter(propertyDescriptor)) return PROPERTY_WITH_GETTER;
        if (!invisibleFromOtherModules(propertyDescriptor)) {
            ModuleDescriptor declarationModule = DescriptorUtils.getContainingModule(propertyDescriptor);
            if (usageModule == null || !usageModule.equals(declarationModule)) {
                return ALIEN_PUBLIC_PROPERTY;
            }
        }
        return STABLE_VALUE;
    }

    private static Kind variableKind(
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable ModuleDescriptor usageModule,
            @NotNull BindingContext bindingContext,
            @NotNull KtElement accessElement
    ) {
        if (variableDescriptor instanceof PropertyDescriptor) {
            return propertyKind((PropertyDescriptor) variableDescriptor, usageModule);
        }
        if (!(variableDescriptor instanceof LocalVariableDescriptor) && !(variableDescriptor instanceof ParameterDescriptor)) return OTHER;
        if (!variableDescriptor.isVar()) return STABLE_VALUE;
        if (variableDescriptor instanceof SyntheticFieldDescriptor) return MUTABLE_PROPERTY;

        // Local variable classification: PREDICTABLE or UNPREDICTABLE
        PreliminaryDeclarationVisitor preliminaryVisitor =
                PreliminaryDeclarationVisitor.Companion.getVisitorByVariable(variableDescriptor, bindingContext);
        // A case when we just analyse an expression alone: counts as unpredictable
        if (preliminaryVisitor == null) return UNPREDICTABLE_VARIABLE;

        // Analyze who writes variable
        // If there is no writer: predictable
        Set<KtDeclaration> writers = preliminaryVisitor.writers(variableDescriptor);
        if (writers.isEmpty()) return PREDICTABLE_VARIABLE;

        // If access element is inside closure: unpredictable
        DeclarationDescriptor variableContainingDeclaration = getVariableContainingDeclaration(variableDescriptor);
        if (isAccessedInsideClosure(variableContainingDeclaration, bindingContext, accessElement)) return UNPREDICTABLE_VARIABLE;

        // Otherwise, predictable iff considered position is BEFORE all writers except declarer itself
        if (isAccessedBeforeAllClosureWriters(variableContainingDeclaration, writers, bindingContext, accessElement)) return PREDICTABLE_VARIABLE;
        else return UNPREDICTABLE_VARIABLE;
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
    public static boolean isStableValue(
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable ModuleDescriptor usageModule
    ) {
        if (variableDescriptor.isVar()) return false;
        if (variableDescriptor instanceof PropertyDescriptor) {
            return propertyKind((PropertyDescriptor) variableDescriptor, usageModule) == STABLE_VALUE;
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
