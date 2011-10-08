package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

public class ArrayIndices implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        v.arraylength();
        v.invokestatic("jet/IntRange", "count", "(I)Ljet/IntRange;");
        return StackValue.onStack(JetTypeMapper.TYPE_INT_RANGE);
    }
}
