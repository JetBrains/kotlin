package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class Increment implements IntrinsicMethod {
    private final int myDelta;

    public Increment(int delta) {
        myDelta = delta;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        final JetExpression operand = arguments.get(0);
        if (operand instanceof JetReferenceExpression) {
            final int index = codegen.indexOfLocal((JetReferenceExpression) operand);
            if (index >= 0 && JetTypeMapper.isIntPrimitive(expectedType)) {
                v.iinc(index, myDelta);
                return StackValue.local(index, expectedType);
            }
        }
        StackValue value = codegen.genQualified(receiver, operand);
        value.dupReceiver(v, 0);
        value.put(expectedType, v);
        if (expectedType == Type.LONG_TYPE) {
            v.aconst(Long.valueOf(myDelta));
        }
        else if (expectedType == Type.FLOAT_TYPE) {
            v.aconst(Float.valueOf(myDelta));
        }
        else if (expectedType == Type.DOUBLE_TYPE) {
            v.aconst(Double.valueOf(myDelta));
        }
        else {
            v.aconst(myDelta);
        }
        v.add(expectedType);
        value.store(v);
        return value;
    }
}
