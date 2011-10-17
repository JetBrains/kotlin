package org.jetbrains.jet.lang.types.expressions;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.calls.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
/*package*/ interface ExpressionTypingInternals extends ExpressionTypingFacade {

    void setResultingDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo);

    @Nullable
    DataFlowInfo getResultingDataFlowInfo();

    @Nullable
    JetType getSelectorReturnType(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetExpression selectorExpression, @NotNull ExpressionTypingContext context);

    void checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @NotNull JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context);
}
