package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class BinaryOp implements IntrinsicMethod {
    private final int opcode;

    public BinaryOp(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, boolean haveReceiver) {
        if (!haveReceiver) {
            codegen.gen(arguments.get(0), expectedType);
        }
        codegen.gen(arguments.get(1), expectedType);
        v.visitInsn(expectedType.getOpcode(opcode));
        return StackValue.onStack(expectedType);
    }
}
