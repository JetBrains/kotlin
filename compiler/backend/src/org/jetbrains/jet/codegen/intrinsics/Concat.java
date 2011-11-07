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
public class Concat implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        codegen.generateStringBuilderConstructor();
        if (receiver == null) {                                    // LHS.plus(RHS)
            v.swap();                                                              // StringBuilder LHS
            codegen.invokeAppendMethod(expectedType);  // StringBuilder(LHS)
            codegen.invokeAppend(arguments.get(0));
        }
        else {                                                     // LHS + RHS
            codegen.invokeAppend(arguments.get(0));                                // StringBuilder(LHS)
            codegen.invokeAppend(arguments.get(1));
        }
        
        v.invokevirtual(ExpressionCodegen.CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
        return StackValue.onStack(Type.getObjectType("java/lang/String"));
    }
}
