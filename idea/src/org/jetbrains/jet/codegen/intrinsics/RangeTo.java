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
    private static final String INT_RANGE_CONSTRUCTOR_DESCRIPTOR = "(II)V";
    private static final Type INT_RANGE_TYPE = Type.getType(IntRange.class);
    private static final String CLASS_INT_RANGE = "jet/IntRange";

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, boolean haveReceiver) {
        JetBinaryExpression expression = (JetBinaryExpression) element;
        final Type leftType = codegen.expressionType(expression.getLeft());
        if (JetTypeMapper.isIntPrimitive(leftType)) {
            v.anew(INT_RANGE_TYPE);
            v.dup();
            codegen.gen(expression.getLeft(), Type.INT_TYPE);
            codegen.gen(expression.getRight(), Type.INT_TYPE);
            v.invokespecial(CLASS_INT_RANGE, "<init>", INT_RANGE_CONSTRUCTOR_DESCRIPTOR);
            return StackValue.onStack(INT_RANGE_TYPE);
        }
        else {
            throw new UnsupportedOperationException("ranges are only supported for int objects");
        }
    }
}
