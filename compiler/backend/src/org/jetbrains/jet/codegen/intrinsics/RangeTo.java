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
            receiver.put(Type.INT_TYPE, v);
            codegen.gen(arguments.get(0), Type.INT_TYPE);
            v.invokestatic("jet/IntRange", "rangeTo", "(II)Ljet/IntRange;");
            return StackValue.onStack(JetTypeMapper.TYPE_INT_RANGE);
        }
        else {
            JetBinaryExpression expression = (JetBinaryExpression) element;
            final Type leftType = codegen.expressionType(expression.getLeft());
            if (JetTypeMapper.isIntPrimitive(leftType)) {
                codegen.gen(expression.getLeft(), Type.INT_TYPE);
                codegen.gen(expression.getRight(), Type.INT_TYPE);
                v.invokestatic(expectedType.getInternalName(), "rangeTo", "(" + leftType.getDescriptor() + leftType.getDescriptor() + ")" + expectedType.getDescriptor());
                return StackValue.onStack(JetTypeMapper.TYPE_INT_RANGE);
            }
            else {
                throw new UnsupportedOperationException("ranges are only supported for int objects");
            }
        }
    }
}
