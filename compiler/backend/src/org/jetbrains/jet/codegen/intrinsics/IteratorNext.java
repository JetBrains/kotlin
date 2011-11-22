package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
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
public class IteratorNext implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        String name;
        if(expectedType == Type.CHAR_TYPE)
            name = "Char";
        else if(expectedType == Type.BOOLEAN_TYPE)
            name = "Boolean";
        else if(expectedType == Type.BYTE_TYPE)
            name = "Byte";
        else if(expectedType == Type.SHORT_TYPE)
            name = "Short";
        else if(expectedType == Type.INT_TYPE)
            name = "Int";
        else if(expectedType == Type.LONG_TYPE)
            name = "Long";
        else if(expectedType == Type.FLOAT_TYPE)
            name = "Float";
        else if(expectedType == Type.DOUBLE_TYPE)
            name = "Double";
        else
            throw new UnsupportedOperationException();
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        v.invokevirtual("jet/" + name + "Iterator", "next" + name, "()" + expectedType.getDescriptor());
        return StackValue.onStack(expectedType);
    }
}
