package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.jet.codegen.CallableMethod;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class PsiMethodCall implements IntrinsicMethod {
    private final PsiMethod myMethod;

    public PsiMethodCall(PsiMethod method) {
        myMethod = method;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element,
                               List<JetExpression> arguments, StackValue receiver) {
        final CallableMethod callableMethod = codegen.getTypeMapper().mapToCallableMethod(myMethod, null);
        codegen.invokeMethodWithArguments(callableMethod, (JetCallExpression) element, receiver);
        return StackValue.onStack(callableMethod.getSignature().getReturnType());
    }
}
