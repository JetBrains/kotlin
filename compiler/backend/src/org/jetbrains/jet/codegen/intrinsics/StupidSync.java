package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Arrays;
import java.util.List;

public class StupidSync implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, @Nullable PsiElement element, @Nullable List<JetExpression> arguments, StackValue receiver) {
        receiver.put(receiver.type, v);
        codegen.pushMethodArguments((JetCallExpression)element, Arrays.asList(JetTypeMapper.TYPE_FUNCTION0));
        v.invokestatic("jet/runtime/Intrinsics", "stupidSync", "(Ljava/lang/Object;Ljet/Function0;)Ljava/lang/Object;");
        StackValue.onStack(JetTypeMapper.TYPE_OBJECT).put(expectedType, v);
        return StackValue.onStack(expectedType);
    }
}
