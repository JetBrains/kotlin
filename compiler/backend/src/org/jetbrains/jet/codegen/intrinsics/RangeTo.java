package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import jet.IntRange;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class RangeTo implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        if(arguments.size()==1) {
            final Type leftType = receiver.type;
            final Type rightType = codegen.expressionType(arguments.get(0));
            receiver.put(Type.INT_TYPE, v);
            codegen.gen(arguments.get(0), rightType);
            v.invokestatic("jet/runtime/Ranges", "rangeTo", "(" + receiver.type.getDescriptor() + leftType.getDescriptor() + ")" + expectedType.getDescriptor());
            return StackValue.onStack(expectedType);
        }
        else {
            JetBinaryExpression expression = (JetBinaryExpression) element;
            final Type leftType = codegen.expressionType(expression.getLeft());
            final Type rightType = codegen.expressionType(expression.getRight());
            if (JetTypeMapper.isIntPrimitive(leftType)) {
                codegen.gen(expression.getLeft(), leftType);
                codegen.gen(expression.getRight(), rightType);
                v.invokestatic("jet/runtime/Ranges", "rangeTo", "(" + leftType.getDescriptor() + rightType.getDescriptor() + ")" + expectedType.getDescriptor());
                return StackValue.onStack(expectedType);
            }
            else {
                throw new UnsupportedOperationException("ranges are only supported for int objects");
            }
        }
    }
}
