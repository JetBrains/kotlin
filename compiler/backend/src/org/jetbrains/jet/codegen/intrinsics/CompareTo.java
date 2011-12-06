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
public class CompareTo implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, @Nullable PsiElement element, @Nullable List<JetExpression> arguments, StackValue receiver) {
        assert arguments != null;
        receiver.put(receiver.type, v);
        codegen.gen(arguments.get(0), receiver.type);
        if(receiver.type == Type.BYTE_TYPE || receiver.type == Type.SHORT_TYPE || receiver.type == Type.CHAR_TYPE)
            v.sub(Type.INT_TYPE);
        else if(receiver.type == Type.INT_TYPE) {
            v.invokestatic("jet/runtime/Intrinsics", "compare", "(II)I");
        }
        else if(receiver.type == Type.BOOLEAN_TYPE) {
            v.invokestatic("jet/runtime/Intrinsics", "compare", "(ZZ)I");
        }
        else if(receiver.type == Type.LONG_TYPE) {
            v.invokestatic("jet/runtime/Intrinsics", "compare", "(JJ)I");
        }
        else if(receiver.type == Type.FLOAT_TYPE) {
            v.invokestatic("java/lang/Float", "compare", "(FF)I");
        }
        else if(receiver.type == Type.DOUBLE_TYPE) {
            v.invokestatic("java/lang/Double", "compare", "(DD)I");
        }
        else {
            throw new UnsupportedOperationException();
        }
        return StackValue.onStack(Type.INT_TYPE);
    }
}
