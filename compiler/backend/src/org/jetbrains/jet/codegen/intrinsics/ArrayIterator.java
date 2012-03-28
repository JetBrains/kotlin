/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class ArrayIterator implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state) {
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        JetCallExpression call = (JetCallExpression) element;
        FunctionDescriptor funDescriptor = (FunctionDescriptor) codegen.getBindingContext().get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) call.getCalleeExpression());
        ClassDescriptor containingDeclaration = (ClassDescriptor) funDescriptor.getContainingDeclaration().getOriginal();
        JetStandardLibrary standardLibrary = codegen.getState().getInjector().getJetStandardLibrary();
        if(containingDeclaration.equals(standardLibrary.getArray())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([Ljava/lang/Object;)Ljet/Iterator;");
            return StackValue.onStack(JetTypeMapper.TYPE_ITERATOR);
        } else {
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                ClassDescriptor arrayClass = standardLibrary.getPrimitiveArrayClassDescriptor(primitiveType);
                if (containingDeclaration.equals(arrayClass)) {
                    String methodSignature = "([" + jvmPrimitiveType.getJvmLetter() + ")" + jvmPrimitiveType.getIterator().getDescriptor();
                    v.invokestatic("jet/runtime/ArrayIterator", "iterator", methodSignature);
                    return StackValue.onStack(jvmPrimitiveType.getIterator().getAsmType());
                }
            }
            throw new UnsupportedOperationException(containingDeclaration.toString());
        }
    }
}
