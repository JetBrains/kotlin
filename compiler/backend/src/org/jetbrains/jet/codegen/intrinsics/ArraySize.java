package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class ArraySize implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        if(receiver.type.equals(JetTypeMapper.TYPE_OBJECT)) {
            v.visitTypeInsn(Opcodes.CHECKCAST, "jet/arrays/JetArray");
            v.invokevirtual("jet/arrays/JetArray", "getSize", "()I");
        }
        else {
            if(receiver.type.equals(JetTypeMapper.ARRAY_BYTE_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[B");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_SHORT_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[S");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_INT_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[I");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_LONG_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[J");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_FLOAT_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[F");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_DOUBLE_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[D");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_CHAR_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[C");
            else if(receiver.type.equals(JetTypeMapper.ARRAY_BOOL_TYPE))
                v.getfield(receiver.type.getInternalName(), "array", "[Z");
            else
                v.getfield("jet/arrays/JetGenericArray", "array", "[Ljava/lang/Object;");

            v.arraylength();
        }

        return StackValue.onStack(Type.INT_TYPE);
    }
}
