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
public class PlusConcat implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, boolean haveReceiver) {
        final JetExpression lhs = arguments.get(0);
        final Type lhsType = codegen.expressionType(lhs);
        codegen.generateStringBuilderConstructor();                  // StringBuilder
        StackValue value = codegen.generateIntermediateValue(lhs);   // StringBuilder receiver
        value.dupReceiver(v, 1);                                     // receiver StringBuilder receiver
        value.put(lhsType, v);                                       // receiver StringBuilder value
        codegen.invokeAppendMethod(lhsType);                         // receiver StringBuilder
        codegen.invokeAppend(arguments.get(1));                      // receiver StringBuilder
        v.invokevirtual(ExpressionCodegen.CLASS_STRING_BUILDER, "toString", "()Ljava/lang/String;");
        value.store(v);
        return null;
    }
}
