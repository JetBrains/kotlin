package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetTypeProjection;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class TypeInfo implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments) {
        final List<JetTypeProjection> typeArguments = ((JetCallExpression) element).getTypeArguments();
        if (typeArguments.size() != 1) {
            throw new UnsupportedOperationException("one type argument expected");
        }
        codegen.pushTypeArgument(typeArguments.get(0));
        return StackValue.onStack(JetTypeMapper.TYPE_TYPEINFO);
    }
}
