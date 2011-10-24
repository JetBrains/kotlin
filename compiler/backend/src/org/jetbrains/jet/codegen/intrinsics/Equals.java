package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
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
public class Equals implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);

        boolean leftNullable = true;
        if(element instanceof JetCallExpression) {
            JetCallExpression jetCallExpression = (JetCallExpression) element;
            JetExpression calleeExpression = jetCallExpression.getCalleeExpression();
            if(calleeExpression != null) {
                JetType leftType = codegen.getBindingContext().get(BindingContext.EXPRESSION_TYPE, calleeExpression);
                if(leftType != null)
                    leftNullable = leftType.isNullable();
            }
        }
        
        JetExpression rightExpr = arguments.get(0);
        JetType rightType = codegen.getBindingContext().get(BindingContext.EXPRESSION_TYPE, rightExpr);
        codegen.gen(rightExpr).put(JetTypeMapper.TYPE_OBJECT, v);

        return codegen.generateEqualsForExpressionsOnStack(JetTokens.EQEQ, JetTypeMapper.TYPE_OBJECT, JetTypeMapper.TYPE_OBJECT, leftNullable, rightType.isNullable());
    }
}
