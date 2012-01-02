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
 * @author yole
 */
public class Concat implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        codegen.generateStringBuilderConstructor();
        if (receiver == null || receiver == StackValue.none()) {                                                     // LHS + RHS
            codegen.invokeAppend(arguments.get(0));                                // StringBuilder(LHS)
            codegen.invokeAppend(arguments.get(1));
        }
        else {                                    // LHS.plus(RHS)
            v.swap();                                                              // StringBuilder LHS
            codegen.invokeAppendMethod(expectedType);  // StringBuilder(LHS)
            codegen.invokeAppend(arguments.get(0));
        }

        v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        StackValue.onStack(JetTypeMapper.JL_STRING_TYPE).put(expectedType, v);
        return StackValue.onStack(expectedType);
    }
}
