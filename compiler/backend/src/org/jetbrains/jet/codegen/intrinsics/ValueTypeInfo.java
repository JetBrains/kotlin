package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class ValueTypeInfo implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        codegen.gen(arguments.get(0), JetTypeMapper.TYPE_JET_OBJECT);
        v.invokeinterface(JetTypeMapper.TYPE_JET_OBJECT.getInternalName(), JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD, "()Ljet/TypeInfo;");
        return StackValue.onStack(JetTypeMapper.TYPE_TYPEINFO);
    }
}
