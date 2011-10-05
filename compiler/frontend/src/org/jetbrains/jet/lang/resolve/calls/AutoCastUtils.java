package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetThisExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.DataFlowInfo;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

/**
 * @author abreslav
 */
public class AutoCastUtils {

    private AutoCastUtils() {}

    public static List<? extends ReceiverDescriptor> getAutocastVariants(@NotNull final BindingContext bindingContext, @NotNull final DataFlowInfo dataFlowInfo, @NotNull ReceiverDescriptor receiverToCast) {
        return receiverToCast.accept(new ReceiverDescriptorVisitor<List<? extends ReceiverDescriptor>, Object>() {
            @Override
            public List<? extends ReceiverDescriptor> visitNoReceiver(ReceiverDescriptor noReceiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<? extends ReceiverDescriptor> visitTransientReceiver(TransientReceiver receiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<? extends ReceiverDescriptor> visitExtensionReceiver(ExtensionReceiver receiver, Object data) {
                return castThis(dataFlowInfo, receiver);
            }

            @Override
            public List<? extends ReceiverDescriptor> visitClassReceiver(ClassReceiver receiver, Object data) {
                return castThis(dataFlowInfo, receiver);
            }

            @Override
            public List<? extends ReceiverDescriptor> visitExpressionReceiver(ExpressionReceiver receiver, Object data) {
                JetExpression expression = receiver.getExpression();
                VariableDescriptor variableDescriptor = getVariableDescriptorFromSimpleName(bindingContext, expression);
                if (variableDescriptor != null && isAutocastable(variableDescriptor)) {
                    List<ReceiverDescriptor> result = Lists.newArrayList();
                    for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
                        result.add(new AutocastReceiver(receiver, possibleType));
                    }
                    return result;
                }
                else if (expression instanceof JetThisExpression) {
                    return castThis(dataFlowInfo, receiver);
                }
                return Collections.emptyList();
            }
        }, null);
    }

    private static List<? extends ReceiverDescriptor> castThis(@NotNull DataFlowInfo dataFlowInfo, @NotNull ReceiverDescriptor receiver) {
        assert receiver.exists();
        List<ReceiverDescriptor> result = Lists.newArrayList();
        for (JetType possibleType : dataFlowInfo.getPossibleTypesForReceiver(receiver)) {
            result.add(new AutocastReceiver(receiver, possibleType));
        }
        return result;
    }

    @Nullable
    public static VariableDescriptor getVariableDescriptorFromSimpleName(@NotNull BindingContext bindingContext, @NotNull JetExpression receiverExpression) {
        if (receiverExpression instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS expression = (JetBinaryExpressionWithTypeRHS) receiverExpression;
            if (expression.getOperationSign().getReferencedNameElementType() == JetTokens.COLON) {
                return getVariableDescriptorFromSimpleName(bindingContext, expression.getLeft());
            }
        }
        VariableDescriptor variableDescriptor = null;
        if (receiverExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) receiverExpression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, nameExpression);
            if (declarationDescriptor instanceof VariableDescriptor) {
                variableDescriptor = (VariableDescriptor) declarationDescriptor;
            }
        }
        return variableDescriptor;
    }

    public static boolean isAutocastable(@NotNull VariableDescriptor variableDescriptor) {
        if (variableDescriptor.isVar()) return false;
        if (variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                if (classDescriptor.getModality().isOverridable() && propertyDescriptor.getModality().isOverridable()) return false;
            }
            else {
                assert !propertyDescriptor.getModality().isOverridable() : "Property outside a class must not be overridable";
            }
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            if (getter == null || !getter.isDefault()) return false;
        }
        return true;
    }

}
