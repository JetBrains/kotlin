package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class StringPlus implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        if(receiver == null || receiver == StackValue.none()) {
            codegen.gen(arguments.get(0)).put(JetTypeMapper.JL_STRING_TYPE, v);
            codegen.gen(arguments.get(1)).put(JetTypeMapper.TYPE_OBJECT, v);
        }
        else {
            receiver.put(JetTypeMapper.JL_STRING_TYPE, v);
            codegen.gen(arguments.get(0)).put(JetTypeMapper.TYPE_OBJECT, v);
        }
        v.invokestatic("jet/runtime/Intrinsics", "stringPlus", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");
        return StackValue.onStack(JetTypeMapper.JL_STRING_TYPE);
    }
}
