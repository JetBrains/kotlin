package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.CodegenUtil;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class ValueTypeInfo implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        JetExpression expr = arguments.get(0);
        BindingContext bindingContext = codegen.getBindingContext();
        JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expr);
        DeclarationDescriptor declarationDescriptor = jetType.getConstructor().getDeclarationDescriptor();
        PsiElement psiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declarationDescriptor.getOriginal());
        if(psiElement instanceof JetClassOrObject) {
            codegen.gen(expr, JetTypeMapper.TYPE_JET_OBJECT);
            v.invokeinterface(JetTypeMapper.TYPE_JET_OBJECT.getInternalName(), JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD, "()Ljet/TypeInfo;");
        }
        else {
            codegen.gen(expr, JetTypeMapper.TYPE_OBJECT);
            v.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;");
            v.iconst(jetType.isNullable() ? 1 : 0);
            v.invokestatic("jet/TypeInfo", "getTypeInfo", "(Ljava/lang/Class;Z)Ljet/TypeInfo;");
        }
        return StackValue.onStack(JetTypeMapper.TYPE_TYPEINFO);
    }
}
