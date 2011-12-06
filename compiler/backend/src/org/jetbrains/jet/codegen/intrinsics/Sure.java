package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class Sure implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        JetCallExpression call = (JetCallExpression) element;
        ResolvedCall<? extends CallableDescriptor> resolvedCall = codegen.getBindingContext().get(BindingContext.RESOLVED_CALL, call.getCalleeExpression());
        assert resolvedCall != null;
        if(resolvedCall.getReceiverArgument().getType().isNullable())  {
            receiver.put(receiver.type, v);
            v.dup();
            Label ok = new Label();
            v.ifnonnull(ok);
            v.invokestatic("jet/runtime/Intrinsics", "throwNpe", "()V");
            v.mark(ok);
            StackValue.onStack(receiver.type).put(expectedType, v);
        }
        else {
            codegen.generateFromResolvedCall(resolvedCall.getReceiverArgument(), expectedType);
        }
        return StackValue.onStack(expectedType);
    }
}
