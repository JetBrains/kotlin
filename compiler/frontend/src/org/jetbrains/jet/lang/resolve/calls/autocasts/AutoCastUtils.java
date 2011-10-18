package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.AutoCastReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;

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
//                JetExpression expression = receiver.getExpression();
//                VariableDescriptor variableDescriptor = DataFlowValueFactory.getVariableDescriptorFromSimpleName(bindingContext, expression);
//                if (variableDescriptor != null) {
//                    List<ReceiverDescriptor> result = Lists.newArrayList();
//                    for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
//                        result.add(new AutoCastReceiver(receiver, possibleType, DataFlowValueFactory.isStableVariable(variableDescriptor)));
//                    }
//                    return result;
//                }
//                else if (expression instanceof JetThisExpression) {
//                    return castThis(dataFlowInfo, receiver);
//                }
                DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(receiver.getExpression(),receiver.getType(), bindingContext);
                List<ReceiverDescriptor> result = Lists.newArrayList();
                for (JetType possibleType : dataFlowInfo.getPossibleTypes(dataFlowValue)) {
                    result.add(new AutoCastReceiver(receiver, possibleType, dataFlowValue.isStableIdentifier()));
                }
                return result;
            }
        }, null);
    }

    private static List<ReceiverDescriptor> castThis(@NotNull DataFlowInfo dataFlowInfo, @NotNull ThisReceiverDescriptor receiver) {
        assert receiver.exists();
        List<ReceiverDescriptor> result = Lists.newArrayList();
        for (JetType possibleType : dataFlowInfo.getPossibleTypes(DataFlowValueFactory.INSTANCE.createDataFlowValue(receiver))) {
            result.add(new AutoCastReceiver(receiver, possibleType, true));
        }
        return result;
    }

//    @Nullable
//    public static JetType castExpression(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
//        JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
//        DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(expression, trace.getBindingContext());
//        for (JetType possibleType : dataFlowInfo.getPossibleTypes(dataFlowValue)) {
//            if (typeChecker.isSubtypeOf(possibleType, expectedType)) {
//                if (dataFlowValue.isStableIdentifier()) {
//                    trace.record(AUTOCAST, expression, possibleType);
//                }
//                else {
//                    trace.report(AUTOCAST_IMPOSSIBLE.on(expression, possibleType, expression.getText()));
//                }
//                return possibleType;
//            }
//        }
////        VariableDescriptor variableDescriptor = DataFlowValueFactory.getVariableDescriptorFromSimpleName(trace.getBindingContext(), expression);
////        if (variableDescriptor != null) {
////            List<JetType> possibleTypes = Lists.newArrayList(dataFlowInfo.getPossibleTypes(variableDescriptor));
////            Collections.reverse(possibleTypes);
////            for (JetType possibleType : possibleTypes) {
////                if (typeChecker.isSubtypeOf(possibleType, expectedType)) {
////                    if (DataFlowValueFactory.isStableVariable(variableDescriptor)) {
////                        trace.record(AUTOCAST, expression, possibleType);
////                    }
////                    else {
////                        trace.report(AUTOCAST_IMPOSSIBLE.on(expression, possibleType, expression.getText()));
////                    }
////                    return possibleType;
////                }
////            }
////        }
//        return null;
//    }

}
