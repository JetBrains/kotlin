package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetThisExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.AUTOCAST_IMPOSSIBLE;
import static org.jetbrains.jet.lang.resolve.BindingContext.AUTOCAST;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

/**
 * @author abreslav
 */
public class AutoCastUtils {

    private AutoCastUtils() {}

    public static List<ReceiverDescriptor> getAutoCastVariants(@NotNull final BindingContext bindingContext, @NotNull final DataFlowInfo dataFlowInfo, @NotNull ReceiverDescriptor receiverToCast) {
        return receiverToCast.accept(new ReceiverDescriptorVisitor<List<ReceiverDescriptor>, Object>() {
            @Override
            public List<ReceiverDescriptor> visitNoReceiver(ReceiverDescriptor noReceiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<ReceiverDescriptor> visitTransientReceiver(TransientReceiver receiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<ReceiverDescriptor> visitExtensionReceiver(ExtensionReceiver receiver, Object data) {
                return castThis(dataFlowInfo, receiver);
            }

            @Override
            public List<ReceiverDescriptor> visitClassReceiver(ClassReceiver receiver, Object data) {
                return castThis(dataFlowInfo, receiver);
            }

            @Override
            public List<ReceiverDescriptor> visitExpressionReceiver(ExpressionReceiver receiver, Object data) {
                JetExpression expression = receiver.getExpression();
                VariableDescriptor variableDescriptor = getVariableDescriptorFromSimpleName(bindingContext, expression);
                if (variableDescriptor != null) {
                    List<ReceiverDescriptor> result = Lists.newArrayList();
                    for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
                        result.add(new AutoCastReceiver(receiver, possibleType, isAutoCastable(variableDescriptor)));
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

    private static List<ReceiverDescriptor> castThis(@NotNull DataFlowInfo dataFlowInfo, @NotNull ReceiverDescriptor receiver) {
        assert receiver.exists();
        List<ReceiverDescriptor> result = Lists.newArrayList();
        for (JetType possibleType : dataFlowInfo.getPossibleTypesForReceiver(receiver)) {
            result.add(new AutoCastReceiver(receiver, possibleType, true));
        }
        return result;
    }

    @Nullable
    public static JetType castExpression(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
        VariableDescriptor variableDescriptor = getVariableDescriptorFromSimpleName(trace.getBindingContext(), expression);
//        boolean appropriateTypeFound = false;
        if (variableDescriptor != null) {
            List<JetType> possibleTypes = Lists.newArrayList(dataFlowInfo.getPossibleTypesForVariable(variableDescriptor));
            Collections.reverse(possibleTypes);
            for (JetType possibleType : possibleTypes) {
                if (typeChecker.isSubtypeOf(possibleType, expectedType)) {
                    if (isAutoCastable(variableDescriptor)) {
                        trace.record(AUTOCAST, expression, possibleType);
                    }
                    else {
                        trace.report(AUTOCAST_IMPOSSIBLE.on(expression, possibleType, expression.getText()));
                    }
                    return possibleType;
                }
            }
//            if (!appropriateTypeFound) {
//                JetType notnullType = dataFlowInfo.getOutType(variableDescriptor);
//                if (notnullType != null && typeChecker.isSubtypeOf(notnullType, expectedType)) {
//                    appropriateTypeFound = true;
//                }
//            }
        }
        return null;
    }

    @Nullable
    public static VariableDescriptor getVariableDescriptorFromSimpleName(@NotNull BindingContext bindingContext, @NotNull JetExpression expression) {
//        if (expression instanceof JetBinaryExpressionWithTypeRHS) {
//            JetBinaryExpressionWithTypeRHS expression = (JetBinaryExpressionWithTypeRHS) expression;
//            if (expression.getOperationSign().getReferencedNameElementType() == JetTokens.COLON) {
//                return getVariableDescriptorFromSimpleName(bindingContext, expression.getLeft());
//            }
//        }
        JetExpression receiver = JetPsiUtil.deparenthesize(expression);
        VariableDescriptor variableDescriptor = null;
        if (receiver instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) receiver;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, nameExpression);
            if (declarationDescriptor instanceof VariableDescriptor) {
                variableDescriptor = (VariableDescriptor) declarationDescriptor;
            }
        }
        return variableDescriptor;
    }

    public static boolean isAutoCastable(@NotNull VariableDescriptor variableDescriptor) {
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
