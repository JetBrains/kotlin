package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class UnaryPlus implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, @Nullable PsiElement element, @Nullable List<JetExpression> arguments, StackValue receiver) {
        boolean nullable = expectedType.getSort() == Type.OBJECT;
        if(nullable) {
            expectedType = JetTypeMapper.unboxType(expectedType);
        }
        if(receiver != null && receiver != StackValue.none())
            receiver.put(expectedType, v);
        else {
            assert arguments != null;
            codegen.gen(arguments.get(0), expectedType);
        }
        return StackValue.onStack(expectedType);
    }
}
