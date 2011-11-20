package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class ArrayIterator implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        JetCallExpression call = (JetCallExpression) element;
        FunctionDescriptor funDescriptor = (FunctionDescriptor) codegen.getBindingContext().get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) call.getCalleeExpression());
        ClassDescriptor containingDeclaration = (ClassDescriptor) funDescriptor.getContainingDeclaration().getOriginal();
        JetStandardLibrary standardLibrary = codegen.getState().getStandardLibrary();
        if(containingDeclaration.equals(standardLibrary.getArray())) {
            codegen.generateTypeInfo(funDescriptor.getReturnType().getArguments().get(0).getType());
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([Ljava/lang/Object;Ljet/typeinfo/TypeInfo;)Ljet/Iterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getByteArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([B)Ljet/ByteIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_BYTE_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getShortArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([S)Ljet/ShortIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_SHORT_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getIntArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([I)Ljet/IntIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_INT_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getLongArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([J)Ljet/LongIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_LONG_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getFloatArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([F)Ljet/FloatIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_FLOAT_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getDoubleArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([D)Ljet/DoubleIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_DOUBLE_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getCharArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([C)Ljet/CharIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_CHAR_ITERATOR);
        }
        else if(containingDeclaration.equals(standardLibrary.getBooleanArrayClass())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([Z)Ljet/BooleanIterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_BOOLEAN_ITERATOR);
        }
        else {
            throw new UnsupportedOperationException(containingDeclaration.toString());
        }
    }
}
